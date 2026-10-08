package com.wbscouting.api.service.media;

import com.wbscouting.api.config.SupabaseProperties;
import com.wbscouting.api.dto.media.MediaOrderItemDto;
import com.wbscouting.api.dto.media.MediaReorderRequestDto;
import com.wbscouting.api.dto.media.MediaUploadResponseDto;
import com.wbscouting.api.entity.Model;
import com.wbscouting.api.entity.ModelMedia;
import com.wbscouting.api.enums.MediaType;
import com.wbscouting.api.exception.ResourceNotFoundException;
import com.wbscouting.api.repository.ModelMediaRepository;
import com.wbscouting.api.repository.ModelRepository;
import com.wbscouting.api.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModelMediaServiceImpl implements ModelMediaService {

    private final ModelRepository modelRepository;
    private final ModelMediaRepository modelMediaRepository;
    private final StorageService storageService;
    private final SupabaseProperties supabaseProperties;

    @Override
    @Transactional
    public MediaUploadResponseDto uploadMedia(UUID modelId, MediaType mediaType, boolean isCover, MultipartFile file) {
        log.info("Iniciando upload de mídia para modelId='{}', mediaType='{}', isCover='{}'", modelId, mediaType, isCover);

        Model model = modelRepository.findById(modelId)
                .orElseThrow(() -> new ResourceNotFoundException("Modelo", "id", modelId));

        if (isCover && mediaType != MediaType.BOOK) {
            throw new IllegalArgumentException("Apenas mídias do tipo BOOK podem ser marcadas como capa.");
        }

        String bucket = supabaseProperties.resolveBucketModelsMedia();

        // Se a nova mídia for do tipo COMPOSITE, substitui o composite anterior se houver
        if (mediaType == MediaType.COMPOSITE) {
            Optional<ModelMedia> existingComposite = modelMediaRepository.findByModelIdAndMediaTypeAndIsActiveTrue(modelId, MediaType.COMPOSITE);
            if (existingComposite.isPresent()) {
                ModelMedia oldComp = existingComposite.get();
                try {
                    storageService.deleteFile(bucket, oldComp.getFilePath());
                } catch (Exception e) {
                    log.warn("Falha ao remover arquivo do composite anterior do storage: {}", e.getMessage());
                }
                modelMediaRepository.delete(oldComp);
                modelMediaRepository.flush();
            }
        }

        // Se marcada como capa, reseta a capa atual atomicamente
        if (isCover) {
            modelMediaRepository.findByModelIdAndIsCoverTrue(modelId).ifPresent(oldCover -> {
                oldCover.setIsCover(false);
                modelMediaRepository.save(oldCover);
            });
        }

        // Cálculo da próxima ordem de exibição
        Integer nextOrder = modelMediaRepository.findNextDisplayOrder(modelId, mediaType);
        if (nextOrder == null) {
            nextOrder = 1;
        }

        // Padrão de caminho: {modelId}/{mediaType.toLowerCase()}/{uuid}-{sanitized-original-filename}
        String sanitizedFilename = sanitizeFilename(file != null ? file.getOriginalFilename() : null);
        String storagePath = String.format("%s/%s/%s-%s",
                modelId,
                mediaType.name().toLowerCase(),
                UUID.randomUUID(),
                sanitizedFilename);

        // Upload físico no bucket Supabase Storage
        String uploadedPath = storageService.uploadFile(bucket, storagePath, file);
        String rawPublicUrl = storageService.getPublicUrl(bucket, uploadedPath);

        // ============================================================
        // PROTECAO INCONDICIONAL: GARANTE QUE A URL SALVA NO BANCO É VÁLIDA!
        // Se o storage retornar URL incompleta (ex: termina com "/public/models-media/" sem path)
        // o helper resolvePublicUrlFromFields remonta a URL correta a partir de bucket + uploadedPath.
        // ============================================================
        String safePublicUrl = storageService.resolvePublicUrlFromFields(bucket, uploadedPath, rawPublicUrl);
        if (!java.util.Objects.equals(rawPublicUrl, safePublicUrl)) {
            log.warn("[uploadMedia] Fallback URL ativado. modelId={}, mediaType={}, rawURL={}, safeURL={}",
                    modelId, mediaType,
                    org.springframework.util.StringUtils.truncate(rawPublicUrl == null ? "null" : rawPublicUrl, 80),
                    org.springframework.util.StringUtils.truncate(safePublicUrl == null ? "null" : safePublicUrl, 120));
        }

        // Bust cache -> garante que a foto NOVA apareça INSTANTANEAMENTE no site público,
        // evitando cache HTTP 404 do browser/Supabase por 1h. (vence a cada hora)
        String bustCache = "?v=" + (System.currentTimeMillis() / 3_600_000L);
        String displayUrl = safePublicUrl + bustCache;

        // Persistência do registro JPA (grava URL SEGURA, garantida válida)
        ModelMedia media = ModelMedia.builder()
                .model(model)
                .mediaType(mediaType)
                .fileUrl(safePublicUrl)
                .filePath(uploadedPath)
                .displayOrder(nextOrder)
                .isCover(isCover)
                .isActive(true)
                .build();

        ModelMedia savedMedia = modelMediaRepository.save(media);

        // Se for capa, sincroniza a foto principal do modelo (também com URL segura + bust cache)
        if (isCover) {
            model.setPrimaryPhotoUrl(displayUrl);
            modelRepository.save(model);
        }

        log.info("Mídia cadastrada com sucesso: id='{}', modelId='{}', url='{}'", savedMedia.getId(), modelId, safePublicUrl);
        return toDto(savedMedia);
    }

    @Override
    @Transactional
    public void deleteMedia(UUID modelId, UUID mediaId) {
        log.info("Iniciando exclusão de mídia id='{}' do modelo id='{}'", mediaId, modelId);

        ModelMedia media = modelMediaRepository.findByIdAndModelId(mediaId, modelId)
                .orElseThrow(() -> new ResourceNotFoundException("Mídia", "id", mediaId));

        String bucket = supabaseProperties.resolveBucketModelsMedia();
        String filePath = media.getFilePath();

        // 1. Excluir arquivo físico no Supabase Storage
        storageService.deleteFile(bucket, filePath);

        boolean wasCover = Boolean.TRUE.equals(media.getIsCover());

        // 2. Deletar registro na tabela model_media
        modelMediaRepository.delete(media);
        modelMediaRepository.flush();

        // 3. Se a mídia deletada for a capa (is_cover = true), promove a mais antiga remanescente do tipo BOOK
        if (wasCover) {
            Model model = media.getModel();
            Optional<ModelMedia> oldestBookOpt = modelMediaRepository.findFirstByModelIdAndMediaTypeOrderByCreatedAtAsc(modelId, MediaType.BOOK);
            if (oldestBookOpt.isPresent()) {
                ModelMedia promotedCover = oldestBookOpt.get();
                promotedCover.setIsCover(true);
                modelMediaRepository.save(promotedCover);
                model.setPrimaryPhotoUrl(promotedCover.getFileUrl());
                log.info("Mídia id='{}' promovida automaticamente a nova capa do modelo id='{}'", promotedCover.getId(), modelId);
            } else {
                model.setPrimaryPhotoUrl(null);
                log.info("Nenhuma mídia BOOK remanescente para capa do modelo id='{}'", modelId);
            }
            modelRepository.save(model);
        }

        log.info("Mídia id='{}' excluída com sucesso.", mediaId);
    }

    @Override
    @Transactional
    public void reorderMedia(UUID modelId, MediaReorderRequestDto reorderDto) {
        log.info("Reordenando mídias do modelo id='{}' com {} itens", modelId,
                reorderDto != null && reorderDto.getItems() != null ? reorderDto.getItems().size() : 0);

        if (!modelRepository.existsById(modelId)) {
            throw new ResourceNotFoundException("Modelo", "id", modelId);
        }

        if (reorderDto == null || reorderDto.getItems() == null || reorderDto.getItems().isEmpty()) {
            return;
        }

        for (MediaOrderItemDto item : reorderDto.getItems()) {
            ModelMedia media = modelMediaRepository.findByIdAndModelId(item.getMediaId(), modelId)
                    .orElseThrow(() -> new ResourceNotFoundException("Mídia", "id", item.getMediaId()));
            media.setDisplayOrder(item.getDisplayOrder());
            modelMediaRepository.save(media);
        }

        log.info("Reordenação de mídias concluída com sucesso para o modelo id='{}'", modelId);
    }

    @Override
    @Transactional
    public void setCoverMedia(UUID modelId, UUID mediaId) {
        log.info("Definindo mídia id='{}' como capa do modelo id='{}'", mediaId, modelId);

        Model model = modelRepository.findById(modelId)
                .orElseThrow(() -> new ResourceNotFoundException("Modelo", "id", modelId));

        ModelMedia targetMedia = modelMediaRepository.findByIdAndModelId(mediaId, modelId)
                .orElseThrow(() -> new ResourceNotFoundException("Mídia", "id", mediaId));

        if (targetMedia.getMediaType() != MediaType.BOOK) {
            throw new IllegalArgumentException("Apenas mídias do tipo BOOK podem ser marcadas como capa.");
        }

        // Desmarca a capa anterior se houver
        modelMediaRepository.findByModelIdAndIsCoverTrue(modelId).ifPresent(oldCover -> {
            if (!oldCover.getId().equals(mediaId)) {
                oldCover.setIsCover(false);
                modelMediaRepository.save(oldCover);
            }
        });

        // Define a nova capa
        targetMedia.setIsCover(true);
        modelMediaRepository.save(targetMedia);

        // Atualiza a foto principal do modelo
        model.setPrimaryPhotoUrl(targetMedia.getFileUrl());
        modelRepository.save(model);

        log.info("Mídia id='{}' definida como capa com sucesso para o modelo id='{}'", mediaId, modelId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MediaUploadResponseDto> listModelMedia(UUID modelId) {
        if (!modelRepository.existsById(modelId)) {
            throw new ResourceNotFoundException("Modelo", "id", modelId);
        }

        return modelMediaRepository.findByModelIdOrderByMediaTypeAscDisplayOrderAsc(modelId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "image.jpg";
        }
        String name = filename.replace("\\", "/");
        int lastSlash = name.lastIndexOf('/');
        if (lastSlash >= 0) {
            name = name.substring(lastSlash + 1);
        }
        String sanitized = name.trim().replaceAll("[^a-zA-Z0-9._-]", "-").replaceAll("-+", "-");
        return sanitized.isEmpty() ? "image.jpg" : sanitized;
    }

    @Override
    @Transactional(readOnly = true)
    public com.wbscouting.api.dto.media.ModelCompositeResponseDto getComposite(UUID modelId) {
        if (!modelRepository.existsById(modelId)) {
            throw new ResourceNotFoundException("Modelo", "id", modelId);
        }

        return modelMediaRepository.findByModelIdAndMediaTypeAndIsActiveTrue(modelId, MediaType.COMPOSITE)
                .map(comp -> toCompositeDtoSafe(comp, null, null))
                .orElse(null);
    }

    @Override
    @Transactional
    public com.wbscouting.api.dto.media.ModelCompositeResponseDto uploadOrReplaceComposite(UUID modelId, MultipartFile file) {
        try {
            // ============================================================
            // 🛡️ ENVELOPE ANTI-500 TOTAL
            // QUALQUER excecao lancada DENTRO do metodo (mesmo as inesperadas)
            // e capturada NO CATCH mais externo no final e convertida
            // para IllegalArgumentException/IllegalStateException (mensagem amigavel)
            // ============================================================

            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("O arquivo do composite não pode estar vazio.");
            }

            // 🔴 CORREÇÃO DE LIMITE: 30 MB (compatível com storage.validateUpload, evitava FileSizeExceededException silenciosa)
            final long MAX_SIZE_30_MB = 30L * 1024 * 1024;
            long fileSize = file.getSize();
            log.info("[COMPOSITE UPLOAD] INICIO. modelId={}, originalFilename='{}', contentType='{}', size={} bytes ({})",
                    modelId, file.getOriginalFilename(), file.getContentType(), fileSize,
                    fileSize > 0 ? String.format("%.2f MB", fileSize / (1024.0 * 1024.0)) : "desconhecido");

            if (fileSize > MAX_SIZE_30_MB) {
                double mbReal = Math.round((fileSize / (1024.0 * 1024.0)) * 10.0) / 10.0;
                throw new IllegalArgumentException(
                    "Arquivo do composite muito grande: " + mbReal + " MB. " +
                    "O limite máximo permitido é 30 MB. Compacte o PDF ou envie uma imagem JPG/PNG menor."
                );
            }

            // 🔴 VALIDAÇÃO MIME BRANDA (respeita extensao se MIME for application/octet-stream / vazio)
            String contentType = file.getContentType();
            String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";

            boolean isPdf = originalFilename.endsWith(".pdf")
                || (contentType != null && contentType.equalsIgnoreCase("application/pdf"));
            boolean isImage = originalFilename.endsWith(".jpg") || originalFilename.endsWith(".jpeg")
                || originalFilename.endsWith(".png") || originalFilename.endsWith(".webp") || originalFilename.endsWith(".heic")
                || (contentType != null && (
                    contentType.equalsIgnoreCase("image/jpeg") || contentType.equalsIgnoreCase("image/png")
                    || contentType.equalsIgnoreCase("image/webp") || contentType.equalsIgnoreCase("image/heic")
                 ));

            if (!isPdf && !isImage) {
                String detalheContentType = (contentType == null || contentType.isBlank() || contentType.equalsIgnoreCase("application/octet-stream"))
                    ? " (o navegador não informou o tipo de arquivo, e a extensão não é PDF/JPG/PNG/WEBP)"
                    : " (tipo detectado: " + contentType + ")";
                throw new IllegalArgumentException(
                    "Formato de arquivo do composite inválido." + detalheContentType +
                    ". Os formatos permitidos são: PDF, JPG, PNG e WEBP."
                );
            }

            Model model = modelRepository.findById(modelId)
                    .orElseThrow(() -> new ResourceNotFoundException("Modelo", "id", modelId));

            String bucket = supabaseProperties.resolveBucketModelsMedia();

            // Substituição atômica: remove composite anterior do storage e banco se houver
            Optional<ModelMedia> existingComposite = modelMediaRepository.findByModelIdAndMediaTypeAndIsActiveTrue(modelId, MediaType.COMPOSITE);
            if (existingComposite.isPresent()) {
                ModelMedia oldComp = existingComposite.get();
                try {
                    // 🟢 SEGURANCA TOTAL: NUNCA deixa a remocao do composite ANTIGO travar o upload do NOVO.
                    String oldPath = oldComp.getFilePath();
                    if (oldPath != null && !oldPath.isBlank()) {
                        storageService.deleteFile(bucket, oldPath);
                        log.info("[COMPOSITE UPLOAD] Removido composite anterior SUCESSO. ModelId={}, oldPath={}", modelId, oldPath);
                    } else {
                        log.warn("[COMPOSITE UPLOAD] Composite antigo SEM filePath (registro LEGADO antigo). Pulando remocao storage e deletando apenas do banco. ModelId={}, oldMediaId={}",
                                modelId, oldComp.getId());
                    }
                } catch (Exception e) {
                    log.warn("[COMPOSITE UPLOAD] (Nao critico, continuando mesmo assim) Falha ao remover arquivo composite anterior do bucket: {}. Motivo: {}.",
                            oldComp.getFilePath(), e.getMessage());
                }
                // Remocao do banco SEMPRE acontece, com try/catch proprio tambem.
                try {
                    modelMediaRepository.delete(oldComp);
                    modelMediaRepository.flush();
                } catch (Exception eDB) {
                    log.error("[COMPOSITE UPLOAD] Falha ao deletar registro do composite antigo no banco. Vai tentar prosseguir salvando o novo. ModelId={}. Erro: {}",
                            modelId, eDB.getMessage());
                }
            }

            // 🟢 NULL-SAFETY: sanitizeFilename nulo/vazio = fallback composite-data.pdf/jpg
            String originalName = sanitizeFilename(file.getOriginalFilename());
            if (originalName == null || originalName.isBlank()) {
                originalName = isPdf ? "composite-document.pdf" : "composite-image.jpg";
            }
            String filename = "models/" + modelId + "/composite/" + UUID.randomUUID() + "-" + originalName;

            // 🔴 TRATAMENTO ESPECÍFICO DE TODAS AS EXCEÇÕES DO UPLOAD
            String publicUrl;
            try {
                publicUrl = storageService.uploadFile(bucket, filename, file);
                // 🛡️ CAMADA ANTI-NOSUCHKEY (1): se uploadFile retornar nula/vazia = falhou sem lancar excecao.
                //    Interrompemos IMEDIATAMENTE: NÃO gravamos registro no banco que aponta para nada.
                if (publicUrl == null || publicUrl.isBlank()) {
                    log.error("[COMPOSITE UPLOAD] storage.uploadFile retornou nulo/vazio sem lancar excecao. Registro NAO sera gravado para evitar NoSuchKey 404. ModelId={}, filename={}.",
                            modelId, filename);
                    throw new IllegalArgumentException(
                        "Falha interna ao salvar o composite no Storage. O objeto não foi criado no bucket. " +
                        "Verifique configurações do bucket models-media (MIME types permitidos / RLS policies / Service Role Key) e tente novamente. " +
                        "Se persistir, remova o composite quebrado, salve o modelo e reenvie o arquivo."
                    );
                }
            } catch (com.wbscouting.api.exception.FileSizeExceededException fsEx) {
                double mbReal = Math.round((fileSize / (1024.0 * 1024.0)) * 10.0) / 10.0;
                log.error("[COMPOSITE UPLOAD] FileSizeExceededException mesmo apos validacao. ModelId={}, size={} MB", modelId, mbReal, fsEx);
                throw new IllegalArgumentException(
                    "Arquivo do composite muito grande: " + mbReal + " MB. Limite 30 MB. " +
                    (fsEx.getMessage() != null ? "Detalhe: " + fsEx.getMessage() : "")
                );
            } catch (com.wbscouting.api.exception.InvalidFileException ivfEx) {
                log.warn("[COMPOSITE UPLOAD] InvalidFileException. ModelId={}.", modelId, ivfEx);
                throw new IllegalArgumentException(
                    "Não foi possível ler o arquivo do composite. Ele pode estar corrompido ou ser um tipo não suportado. " +
                    "Tente baixar o arquivo novamente e reenviar. Detalhe: " + (ivfEx.getMessage() != null ? ivfEx.getMessage() : "")
                );
            } catch (com.wbscouting.api.exception.StorageException stEx) {
                log.error("[COMPOSITE UPLOAD] StorageException (timeout, auth, rede, MIME_TYPE). ModelId={}.", modelId, stEx);
                String rawMsg = stEx.getMessage() != null ? stEx.getMessage().toLowerCase() : "";
                // 🛡️ CAMADA AJUDA ESPECIFICA: detecta MIME TYPE bloqueado no bucket (415 invalid_mime_type / application/pdf is not supported)
                if (rawMsg.contains("invalid_mime_type") || rawMsg.contains("application/pdf") || rawMsg.contains("mime type")) {
                    throw new IllegalArgumentException(
                        "⚠️ O bucket 'models-media' no Supabase Ainda BLOQUEIA arquivos PDF! " +
                        "Como corrigir em 30 segundos no Painel Supabase: " +
                        "1) Storage → bucket models-media → ⚙️ Configurações (engrenagem) → Allowed MIME Types. " +
                        "2) Adicione a linha: application/pdf e clique em UPDATE BUCKET SETTINGS. " +
                        "3) Depois: Remova o composite quebrado no Admin, salve, e reenvie o arquivo. " +
                        "Detalhe técnico do Supabase: " + (stEx.getMessage() != null ? stEx.getMessage() : "")
                    );
                }
                org.springframework.http.HttpStatus st = stEx.getStatus();
                if (st != null && (st.is5xxServerError() || st == org.springframework.http.HttpStatus.GATEWAY_TIMEOUT)) {
                    throw new IllegalArgumentException(
                        "Ocorreu um timeout ou falha temporária de conexão ao enviar o composite para o armazenamento na nuvem. " +
                        "Tente novamente em alguns segundos. Detalhe: " + (stEx.getMessage() != null ? stEx.getMessage() : "")
                    );
                }
                throw new IllegalArgumentException(
                    "Não foi possível enviar o composite para o armazenamento na nuvem. " +
                    "Detalhe: " + (stEx.getMessage() != null ? stEx.getMessage() : "Erro de comunicação.")
                );
            } catch (Exception exUploadGenerica) {
                // 🛡️ CAMADA EXTRA ANTI-FRACASSO (MAIS IMPORTANTE DE TODA):
                // Captura QUALQUER exceção NAO PREVISTA nos catches acima (ex: RestClientException,
                // RuntimeException, NullPointerException, IllegalArgumentException de validacao interna,
                // Jackson JSON parsing exception do RestClient, etc).
                // Interrompe 100% o fluxo: NAO salva model_media quebrado = NUNCA MAIS NoSuchKey.
                String causa = (exUploadGenerica.getCause() != null && exUploadGenerica.getCause().getMessage() != null)
                    ? exUploadGenerica.getCause().getMessage() : exUploadGenerica.getMessage();
                String tipo = exUploadGenerica.getClass().getSimpleName();
                log.error("[COMPOSITE UPLOAD] Excecao GENERICA nao prevista em storage.uploadFile (interrompendo save anti-NoSuchKey). " +
                    "ModelId={}, ExceptionType={}, Causa={}.", modelId, tipo, causa, exUploadGenerica);
                throw new IllegalArgumentException(
                    "Falha ao enviar composite para o Storage (erro inesperado: " + tipo + "). " +
                    "Remova o composite quebrado no Admin, salve o modelo, e reenvie o arquivo. " +
                    "Detalhe técnico: " + (causa != null ? causa : tipo)
                );
            }

            OffsetDateTime agora = OffsetDateTime.now();
            ModelMedia media = ModelMedia.builder()
                    .model(model)
                    .mediaType(MediaType.COMPOSITE)
                    .fileUrl(publicUrl)
                    .filePath(filename)
                    .displayOrder(1)
                    .isCover(false)
                    .isActive(true)
                    // Timestamps EXPLICITOS anti-not-null
                    .createdAt(agora)
                    .updatedAt(agora)
                    .build();

            ModelMedia saved = modelMediaRepository.save(media);
            log.info("[COMPOSITE UPLOAD] SUCESSO. ModelId={}, finalPath={}, urlSize={} chars",
                    modelId, filename, publicUrl != null ? publicUrl.length() : 0);
            // toCompositeDto com envelope anti-null interno (ver abaixo)
            return toCompositeDtoSafe(saved, originalName, file.getSize());

        } catch (IllegalArgumentException | ResourceNotFoundException eTratado) {
            // Mensagens amigaveis ja tratadas: relancar como estao (400 Bad Request / 404)
            // IllegalArgumentException tem HANDLER EXISTENTE DESDE SEMPRE no GlobalExceptionHandler linha 140 = HTTP 400 c/ mensagem!
            throw eTratado;
        } catch (Exception eQualquerOutra) {
            // ============================================================
            // 🛡️ CAIXA FORTE FINAL: Nenhuma excecao nao tratada escapa como 500 generico
            // Inclui: NullPointerException, ConstraintViolationException,
            //         TransactionSystemException, DataIntegrityViolationException etc.
            // Converte TUDO para IllegalArgumentException = tem HANDLER EXISTENTE.
            // ============================================================
            log.error("[COMPOSITE UPLOAD] EXCECAO NAO TRATADA CONVERTIDA PARA MENSAGEM AMIGAVEL (evitando 500 generico). ModelId={}",
                    modelId, eQualquerOutra);
            String motivo = (eQualquerOutra.getMessage() != null && !eQualquerOutra.getMessage().isBlank())
                ? eQualquerOutra.getMessage()
                : "Erro interno durante o processamento do composite no banco de dados.";
            if (eQualquerOutra.getCause() != null && eQualquerOutra.getCause().getMessage() != null
                && !eQualquerOutra.getCause().getMessage().isBlank()) {
                motivo = motivo + ". Detalhe tecnico: " + eQualquerOutra.getCause().getMessage();
            }
            throw new IllegalArgumentException(
                "Não foi possível salvar o composite. " + motivo + ". " +
                "Tente novamente em alguns segundos ou verifique se o arquivo está íntegro."
            );
        }
    }

    @Override
    @Transactional
    public void deleteComposite(UUID modelId) {
        if (!modelRepository.existsById(modelId)) {
            throw new ResourceNotFoundException("Modelo", "id", modelId);
        }

        modelMediaRepository.findByModelIdAndMediaTypeAndIsActiveTrue(modelId, MediaType.COMPOSITE)
                .ifPresent(comp -> {
                    try {
                        storageService.deleteFile(supabaseProperties.getBuckets().getModelsMedia(), comp.getFilePath());
                    } catch (Exception e) {
                        log.warn("Falha ao remover arquivo do composite do storage: {}", e.getMessage());
                    }
                    modelMediaRepository.delete(comp);
                });
    }

    /**
     * 🟢 Versao NULL-SAFE do toCompositeDto. NUNCA lança NPE mesmo que media tenha campos nulos.
     * Evita 500 generico no Jackson ao serializar DTO de resposta.
     */
    private com.wbscouting.api.dto.media.ModelCompositeResponseDto toCompositeDtoSafe(ModelMedia media, String originalFilename, Long sizeBytes) {
        try {
            String name = originalFilename;
            if (name == null || name.isBlank()) {
                if (media != null && media.getFilePath() != null && !media.getFilePath().isBlank()) {
                    name = media.getFilePath().substring(Math.max(0, media.getFilePath().lastIndexOf('/') + 1));
                    if (name.matches("^[0-9a-fA-F\\-]{36}-.+")) {
                        name = name.length() >= 37 ? name.substring(37) : "composite";
                    }
                } else {
                    name = "composite";
                }
            }
            // Fallback extra: se nome ainda estiver muito estranho (sem extensa), usa ext por MIME
            if (!name.toLowerCase().contains(".") && media != null) {
                name = name + (".pdf".equalsIgnoreCase("pdf") ? ".pdf" : ".jpg");
                name = name.replace(".pdf.pdf", ".pdf");
            }

            String type = name.toLowerCase().endsWith(".pdf") ? "PDF" : "IMAGE";
            String bucket = supabaseProperties.resolveBucketModelsMedia();

            String rawFileUrl = media != null ? media.getFileUrl() : null;
            String filePath = media != null ? media.getFilePath() : null;

            // Fallback de URL incompleta (usa storage service se ele existir)
            String safePublicUrl = rawFileUrl;
            try {
                if (filePath != null || (rawFileUrl != null && !rawFileUrl.isBlank())) {
                    String resolved = storageService.resolvePublicUrlFromFields(bucket, filePath, rawFileUrl);
                    if (resolved != null && !resolved.isBlank()) {
                        safePublicUrl = resolved;
                    }
                }
            } catch (Exception ignoreHelper) {
                // Se helper falhar, usa raw. Nada de NPE.
                safePublicUrl = rawFileUrl;
            }
            // Null safety final na URL
            if (safePublicUrl == null || safePublicUrl.isBlank()) {
                safePublicUrl = rawFileUrl;
                if (safePublicUrl == null || safePublicUrl.isBlank()) {
                    if (filePath != null && !filePath.isBlank()) {
                        safePublicUrl = filePath;
                    } else {
                        safePublicUrl = "";
                    }
                }
            }
            // Bust cache epochHour (evita cache CDN)
            if (!safePublicUrl.isBlank() && safePublicUrl.startsWith("http")) {
                safePublicUrl = safePublicUrl + (safePublicUrl.contains("?") ? "&" : "?") + "v=" + (System.currentTimeMillis() / 3_600_000L);
            }

            // Fallback: sizeBytes nulo
            long size = 0L;
            if (sizeBytes != null) size = sizeBytes;

            // Fallback updatedAt: NUNCA NULO
            OffsetDateTime dt = OffsetDateTime.now();
            if (media != null) {
                if (media.getUpdatedAt() != null) {
                    dt = media.getUpdatedAt();
                } else if (media.getCreatedAt() != null) {
                    dt = media.getCreatedAt();
                }
            }

            // Fallback final ID
            UUID idFinal = (media != null && media.getId() != null) ? media.getId() : UUID.randomUUID();

            return com.wbscouting.api.dto.media.ModelCompositeResponseDto.builder()
                    .id(idFinal)
                    .fileUrl(safePublicUrl)
                    .filePath(filePath != null ? filePath : "")
                    .fileName(name)
                    .fileType(type)
                    .fileSizeBytes(size)
                    .updatedAt(dt)
                    .build();
        } catch (Exception qualquerExcecaoInternaDto) {
            // Caixa forte DO DTO: se algo falhar na montagem do DTO, retorna um objeto minimo valido
            // ao inves de 500 generico por erro de serializacao Jackson.
            log.error("[COMPOSITE UPLOAD] Falha INTERNA na montagem do DTO de resposta. Retornando DTO fallback. ModelMedia={}",
                    media != null && media.getId() != null ? media.getId() : "null",
                    qualquerExcecaoInternaDto);
            return com.wbscouting.api.dto.media.ModelCompositeResponseDto.builder()
                    .id(UUID.randomUUID())
                    .fileUrl("")
                    .filePath("")
                    .fileName("composite.pdf")
                    .fileType("PDF")
                    .fileSizeBytes(sizeBytes != null ? sizeBytes : 0L)
                    .updatedAt(OffsetDateTime.now())
                    .build();
        }
    }

    private MediaUploadResponseDto toDto(ModelMedia media) {
        // ============================================================
        // PROTECAO CONSISTENCIA: ADMIN === PUBLICO
        // Perfil publico usava storageService.resolvePublicUrlFromFields
        // para remontar URLs incompletas.
        // Admin NAO usava. Por isso foto nova Eve aparecia no publico
        // e NAO aparecia no painel.
        // Aplicamos exatamente a mesma logica aqui.
        // ============================================================
        String bucket = supabaseProperties.resolveBucketModelsMedia();
        String rawUrl = media.getFileUrl();
        String filePath = media.getFilePath();
        String safeUrl = storageService.resolvePublicUrlFromFields(bucket, filePath, rawUrl);

        // Bust cache de 1 hora (forca navegador carregar midia nova, evita HTTP 404 cache antigo)
        if (safeUrl != null && !safeUrl.isBlank() && !safeUrl.contains("?v=")) {
            safeUrl = safeUrl + "?v=" + (System.currentTimeMillis() / 3_600_000L);
        }

        return MediaUploadResponseDto.builder()
                .id(media.getId())
                .modelId(media.getModel().getId())
                .mediaType(media.getMediaType())
                .fileUrl(safeUrl)
                .filePath(media.getFilePath())
                .displayOrder(media.getDisplayOrder())
                .isCover(media.getIsCover())
                .createdAt(media.getCreatedAt())
                .build();
    }
}