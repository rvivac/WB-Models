package com.wbscouting.api.service.model;

import com.wbscouting.api.config.SupabaseProperties;
import com.wbscouting.api.dto.ModelDTO;
import com.wbscouting.api.dto.model.*;
import com.wbscouting.api.entity.Model;
import com.wbscouting.api.entity.ModelMedia;
import com.wbscouting.api.enums.GenderType;
import com.wbscouting.api.exception.ResourceNotFoundException;
import com.wbscouting.api.repository.ModelMediaRepository;
import com.wbscouting.api.repository.ModelRepository;
import com.wbscouting.api.repository.specification.ModelSpecification;
import com.wbscouting.api.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModelServiceImpl implements ModelService {

    private final ModelRepository modelRepository;
    private final ModelMediaRepository modelMediaRepository;
    private final StorageService storageService;
    private final SupabaseProperties supabaseProperties;

    /**
     * JDBC Template com SQL PURO para diagnostico de conexao/dados.
     * required=false: injecao falha silenciosamente em ambientes sem DataSource
     * (ex: testes unitarios leves). Null-safe check em cada uso.
     */
    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public ModelAdminResponseDto createModel(ModelCreateRequestDto dto) {
        log.info("Cadastrando novo modelo artístico: {}", dto.getStageName());

        Model model = Model.builder()
                .stageName(dto.getStageName())
                .gender(dto.getGender())
                .isStar(Boolean.TRUE.equals(dto.getIsStar()))
                .isFeaturedHome(Boolean.TRUE.equals(dto.getIsFeaturedHome()))
                .featuredOrder(dto.getFeaturedOrder())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .primaryPhotoUrl(dto.getPrimaryPhotoUrl())
                .instagramUrl(dto.getInstagramUrl())
                .birthDate(dto.getBirthDate())
                .heightCm(dto.getHeightCm())
                .city(dto.getCity())
                .nationality(dto.getNationality())
                .dressSize(dto.getDressSize())
                .shoeSize(dto.getShoeSize())
                .bustChestCm(dto.getBustChestCm())
                .waistCm(dto.getWaistCm())
                .hipsCm(dto.getHipsCm())
                .hairColor(dto.getHairColor())
                .eyesColor(dto.getEyesColor())
                .build();

        Model savedModel = modelRepository.save(model);
        log.info("Modelo cadastrado com sucesso com ID: {}", savedModel.getId());
        return mapToAdminResponse(savedModel);
    }

    @Override
    @Transactional
    public ModelAdminResponseDto updateModel(UUID id, ModelUpdateRequestDto dto) {
        log.info("Atualizando dados do modelo com ID: {}", id);

        Model model = modelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Modelo", "id", id));

        model.setStageName(dto.getStageName());
        model.setGender(dto.getGender());
        if (dto.getIsStar() != null) model.setIsStar(dto.getIsStar());
        if (dto.getIsFeaturedHome() != null) model.setIsFeaturedHome(dto.getIsFeaturedHome());
        model.setFeaturedOrder(dto.getFeaturedOrder());
        if (dto.getIsActive() != null) model.setIsActive(dto.getIsActive());
        model.setPrimaryPhotoUrl(dto.getPrimaryPhotoUrl());
        model.setInstagramUrl(dto.getInstagramUrl());
        model.setBirthDate(dto.getBirthDate());
        model.setHeightCm(dto.getHeightCm());
        model.setCity(dto.getCity());
        model.setNationality(dto.getNationality());
        model.setDressSize(dto.getDressSize());
        model.setShoeSize(dto.getShoeSize());
        model.setBustChestCm(dto.getBustChestCm());
        model.setWaistCm(dto.getWaistCm());
        model.setHipsCm(dto.getHipsCm());
        model.setHairColor(dto.getHairColor());
        model.setEyesColor(dto.getEyesColor());

        Model updatedModel = modelRepository.save(model);
        log.info("Modelo ID: {} atualizado com sucesso", updatedModel.getId());
        return mapToAdminResponse(updatedModel);
    }

    @Override
    @Transactional
    public ModelAdminResponseDto updateStatus(UUID id, ModelStatusPatchDto dto) {
        log.info("Atualizando status de ativação do modelo ID: {} para {}", id, dto.getIsActive());

        Model model = modelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Modelo", "id", id));

        model.setIsActive(dto.getIsActive());
        Model saved = modelRepository.save(model);
        return mapToAdminResponse(saved);
    }

    @Override
    @Transactional
    public ModelAdminResponseDto updateStar(UUID id, ModelStarPatchDto dto) {
        log.info("Atualizando status Star do modelo ID: {} para {}", id, dto.getIsStar());

        Model model = modelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Modelo", "id", id));

        model.setIsStar(dto.getIsStar());
        Model saved = modelRepository.save(model);
        return mapToAdminResponse(saved);
    }

    @Override
    @Transactional
    public ModelAdminResponseDto updateFeatured(UUID id, ModelFeaturedPatchDto dto) {
        log.info("Atualizando destaque na home do modelo ID: {} (isFeaturedHome={}, featuredOrder={})",
                id, dto.getIsFeaturedHome(), dto.getFeaturedOrder());

        Model model = modelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Modelo", "id", id));

        model.setIsFeaturedHome(dto.getIsFeaturedHome());
        model.setFeaturedOrder(dto.getFeaturedOrder());
        Model saved = modelRepository.save(model);
        return mapToAdminResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ModelAdminResponseDto> listAdminModels(
            GenderType gender, Boolean isStar, Boolean isActive, String search, Pageable pageable) {

        // ============================================================
        // 🔥🔥 DIAGNOSTICO SQL PURO VIA JDBC DIRETO (SEM JPA/HIBERNATE)
        //    Permite confirmar no painel Render se: a) o DB conectado eh o correto;
        //    b) as linhas realmente existem. Nao afeta a consulta real final.
        // ============================================================
        if (jdbcTemplate != null) {
            try {
                Integer jdbcCount = jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM public.models", Integer.class);
                log.info("[DIAGNOSTICO-SQL-PURO] SELECT count(*) FROM public.models (JDBC direto) = {}", jdbcCount);

                List<Map<String, Object>> jdbcRows = jdbcTemplate.queryForList(
                        "SELECT id, stage_name, is_active, is_star, created_at FROM public.models ORDER BY created_at DESC LIMIT 5");
                log.info("[DIAGNOSTICO-SQL-PURO] 5 linhas JDBC direto (public.models) = {}", jdbcRows);
            } catch (Exception jdbcEx) {
                log.error("[DIAGNOSTICO-SQL-PURO] Falha ao executar SQL puro: {}", jdbcEx.getMessage());
            }
        }

        // 1. SANITIZACAO DO PARAMETRO DE BUSCA: vazio/branco → NULL (para o LIKE do JPQL nao filtrar nada)
        final String sanitizedSearch = StringUtils.hasText(search) ? search.trim() : null;

        // 2. Verifica se ha algum filtro REAL aplicado (apos sanitizacao)
        final boolean hasFilters = (gender != null)
                || (isStar != null)
                || (isActive != null)
                || (sanitizedSearch != null);

        // 3. LOGGER DE PARAMETROS (ajuda a identificar filtros corretamente)
        log.info("[ADMIN MODEL] listAdminModels → gender={}, isStar={}, isActive={}, sanitizedSearch='{}', " +
                        "hasFilters={}, pageable.page={}, pageable.size={}, pageable.sort={}",
                gender, isStar, isActive, sanitizedSearch,
                hasFilters,
                pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        // ============================================================
        // 🔥🔥 CORACAO DA CORRECAO — JPQLs PURAS (task requerida):
        //    - NENHUMA Specification (evita join fetch/root.fetch indevido em specs)
        //    - NENHUM @EntityGraph acoplado em collections (Model.media)
        //    - countQuery EXPLICITA em ambas as queries (resolve totalElements=0)
        //    - Warning HHH90003004 NAO ocorre mais aqui.
        // ============================================================
        final Page<Model> pageResult;

        if (hasFilters) {
            log.info("[ADMIN MODEL] Executando findAdminWithFilters() (JPQL pura) com filtros.");
            pageResult = modelRepository.findAdminWithFilters(
                    gender,
                    isStar,
                    isActive,
                    sanitizedSearch,
                    pageable
            );
        } else {
            log.info("[ADMIN MODEL] Executando findAllAdminPure() (JPQL pura) SEM filtros.");
            pageResult = modelRepository.findAllAdminPure(pageable);
        }

        // ============================================================
        // LOG DE RESULTADO.
        // ============================================================
        log.info("[ADMIN MODEL] Resultado retornado: totalElements={}, numberOfElements={}, totalPages={}",
                pageResult.getTotalElements(), pageResult.getNumberOfElements(), pageResult.getTotalPages());

        // Dump dos primeiros 5 IDs (diagnostico: mostram se as instancias sao as esperadas)
        if (pageResult.getNumberOfElements() > 0) {
            pageResult.getContent().stream().limit(5).forEach(m ->
                    log.debug("[ADMIN MODEL] Modelo retornado → id={}, stageName='{}', isActive={}, isStar={}",
                            m.getId(), m.getStageName(), m.getIsActive(), m.getIsStar()));
        } else {
            log.warn("[ADMIN MODEL] Conteudo desta pagina VAZIO. Total global (content[]) = 0.");
        }

        return pageResult.map(this::mapToAdminResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ModelAdminResponseDto getAdminModelById(UUID id) {
        Model model = modelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Modelo", "id", id));
        return mapToAdminResponse(model);
    }

    @Override
    @Transactional
    public void deleteModel(UUID id) {
        log.info("Excluindo modelo com ID: {}", id);

        Model model = modelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Modelo", "id", id));

        // 1. Buscar TODAS as midias vinculadas ao modelo (ativas e inativas)
        List<ModelMedia> allMedia = modelMediaRepository.findByModelIdOrderByDisplayOrderAsc(id);
        String bucket = supabaseProperties.resolveBucketModelsMedia();

        // 2. Remover arquivos FISICOS do Supabase Storage (FAIL-SAFE)
        int deletedFiles = 0;
        int failedFiles = 0;
        for (ModelMedia media : allMedia) {
            String filePath = media.getFilePath();
            if (!StringUtils.hasText(filePath)) {
                log.warn("Midia ID: {} do modelo ID: {} sem filePath valido. Pulando remocao do storage.", media.getId(), id);
                continue;
            }
            try {
                storageService.deleteFile(bucket, filePath);
                deletedFiles++;
            } catch (Exception e) {
                failedFiles++;
                log.warn("Falha NAO-CRITICA ao remover arquivo do storage. Midia ID: {}, path: {}, motivo: {}. Procedendo com exclusao do banco.",
                        media.getId(), filePath, e.getMessage());
            }
        }

        log.info("Remocao de arquivos do storage concluida. Modelo ID: {} -> {} removidos, {} falhas (nao criticas).", id, deletedFiles, failedFiles);

        // 3. Deletar as midias do banco SEM depender do cascade (evita FK constraint em alguns bancos)
        if (!allMedia.isEmpty()) {
            modelMediaRepository.deleteAll(allMedia);
            modelMediaRepository.flush();
            log.info("{} registros de midia removidos da tabela model_media para o modelo ID: {}", allMedia.size(), id);
        }

        // 4. Excluir definitivamente o modelo do banco
        modelRepository.delete(model);
        modelRepository.flush();

        log.info("Modelo ID: {} excluido com sucesso do banco de dados (Hard Delete).", id);
    }

    // Métodos Públicos de Catálogo (Portal / Site Institucional)

    @Override
    @Transactional(readOnly = true)
    public Page<ModelDTO.SummaryResponse> listModels(GenderType gender, Boolean isStar, Pageable pageable) {
        Page<Model> models;

        if (gender != null) {
            models = modelRepository.findByGenderAndIsActiveTrue(gender, pageable);
        } else if (Boolean.TRUE.equals(isStar)) {
            models = modelRepository.findByIsStarTrueAndIsActiveTrue(pageable);
        } else {
            models = modelRepository.findByIsActiveTrue(pageable);
        }

        return models.map(this::mapToSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ModelDTO.SummaryResponse> getFeaturedHomeModels() {
        return modelRepository.findFeaturedHomeModels()
                .stream()
                .map(this::mapToSummary)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ModelDTO.DetailResponse getModelById(UUID id) {
        Model model = modelRepository.findById(id)
                .filter(Model::getIsActive)
                .orElseThrow(() -> new ResourceNotFoundException("Modelo", "id", id));

        return mapToDetail(model);
    }

    private ModelAdminResponseDto mapToAdminResponse(Model model) {
        return ModelAdminResponseDto.builder()
                .id(model.getId())
                .stageName(model.getStageName())
                .gender(model.getGender())
                .isStar(model.getIsStar())
                .isFeaturedHome(model.getIsFeaturedHome())
                .featuredOrder(model.getFeaturedOrder())
                .isActive(model.getIsActive())
                .primaryPhotoUrl(model.getPrimaryPhotoUrl())
                .instagramUrl(model.getInstagramUrl())
                .birthDate(model.getBirthDate())
                .heightCm(model.getHeightCm())
                .city(model.getCity())
                .nationality(model.getNationality())
                .dressSize(model.getDressSize())
                .shoeSize(model.getShoeSize())
                .bustChestCm(model.getBustChestCm())
                .waistCm(model.getWaistCm())
                .hipsCm(model.getHipsCm())
                .hairColor(model.getHairColor())
                .eyesColor(model.getEyesColor())
                .createdAt(model.getCreatedAt())
                .updatedAt(model.getUpdatedAt())
                .build();
    }

    private ModelDTO.SummaryResponse mapToSummary(Model model) {
        return ModelDTO.SummaryResponse.builder()
                .id(model.getId())
                .stageName(model.getStageName())
                .gender(model.getGender())
                .isStar(model.getIsStar())
                .primaryPhotoUrl(model.getPrimaryPhotoUrl())
                .heightCm(model.getHeightCm())
                .dressSize(model.getDressSize())
                .shoeSize(model.getShoeSize())
                .city(model.getCity())
                .build();
    }

    private ModelDTO.DetailResponse mapToDetail(Model model) {
        List<ModelDTO.MediaResponse> mediaResponses = model.getMedia()
                .stream()
                .filter(m -> Boolean.TRUE.equals(m.getIsActive()))
                .map(m -> ModelDTO.MediaResponse.builder()
                        .id(m.getId())
                        .mediaType(m.getMediaType())
                        .fileUrl(m.getFileUrl())
                        .displayOrder(m.getDisplayOrder())
                        .build())
                .collect(Collectors.toList());

        return ModelDTO.DetailResponse.builder()
                .id(model.getId())
                .stageName(model.getStageName())
                .gender(model.getGender())
                .isStar(model.getIsStar())
                .isFeaturedHome(model.getIsFeaturedHome())
                .primaryPhotoUrl(model.getPrimaryPhotoUrl())
                .instagramUrl(model.getInstagramUrl())
                .birthDate(model.getBirthDate())
                .heightCm(model.getHeightCm())
                .city(model.getCity())
                .nationality(model.getNationality())
                .dressSize(model.getDressSize())
                .shoeSize(model.getShoeSize())
                .bustChestCm(model.getBustChestCm())
                .waistCm(model.getWaistCm())
                .hipsCm(model.getHipsCm())
                .hairColor(model.getHairColor())
                .eyesColor(model.getEyesColor())
                .media(mediaResponses)
                .build();
    }
}
