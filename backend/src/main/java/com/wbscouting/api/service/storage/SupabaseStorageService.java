package com.wbscouting.api.service.storage;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wbscouting.api.config.SupabaseProperties;
import com.wbscouting.api.exception.FileSizeExceededException;
import com.wbscouting.api.exception.InvalidFileException;
import com.wbscouting.api.exception.StorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupabaseStorageService implements StorageService {

    private final RestClient supabaseStorageRestClient;
    private final SupabaseProperties supabaseProperties;

    public SupabaseProperties getProperties() {
        return supabaseProperties;
    }

    // Limites de tamanho em bytes conforme especificação dos buckets
    public static final long MAX_SIZE_SITE_ASSETS = 30L * 1024 * 1024;        // 30 MB (banners, logos, PDFs institucionais, composites publicitarios)
    public static final long MAX_SIZE_MODELS_MEDIA = 30L * 1024 * 1024;       // 30 MB (Fotos Book, Polas HD + COMPOSITE PDF export InDesign)
    public static final long MAX_SIZE_CANDIDATES_UPLOADS = 15L * 1024 * 1024; // 15 MB (Fotos WhatsApp/Instagram enviadas por candidatas)

    // Conjuntos de MIME Types permitidos por tipo de mídia
    public static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    public static final Set<String> ALLOWED_VIDEO_TYPES = Set.of(
            "video/mp4",
            "video/webm"
    );

    public static final Set<String> ALLOWED_PDF_TYPES = Set.of(
            "application/pdf"
    );

    @Override
    public String uploadFile(String bucket, String path, MultipartFile file) {
        log.info("Iniciando upload de arquivo para bucket='{}', path='{}'", bucket, path);

        // 🟢 ORDEM CORRETA AGORA: Normalizar PATH ANTES do validateUpload.
        // (Bug antigo: validate era chamado com path BRUTO, e apos a validacao normalizava.
        //  Causava erros de path que comecavam com / ou \ na validacao de /composite/!)
        String normalizedPath = normalizePath(path);
        String contentType = file.getContentType();
        boolean localFallbackEnabled = supabaseProperties.getStorage() != null && supabaseProperties.getStorage().isLocalFallback();

        // 🟢 Validacao agora usa PATH JÁ NORMALIZADO (confiavel)
        validateUpload(bucket, normalizedPath, file);

        // 1. Caso a chave do Supabase não esteja configurada ou seja dummy
        if (!supabaseProperties.isKeyConfigured()) {
            if (localFallbackEnabled) {
                log.warn("[SUPABASE STORAGE LOCAL FALLBACK] Chave do Supabase ('supabase.service-role-key' / 'supabase.key') não configurada ou com valor dummy ('{}'). Realizando salvamento local para bucket='{}', path='{}'.",
                        supabaseProperties.getEffectiveKey(), bucket, normalizedPath);
                saveFileLocally(bucket, normalizedPath, file);
                return normalizedPath;
            } else {
                log.error("[SUPABASE STORAGE AUTH ERROR] Tentativa de upload para o bucket '{}' com chave do Supabase ausente ou inválida ('{}').",
                        bucket, supabaseProperties.getEffectiveKey());
                throw new StorageException(
                        "Chave do Supabase não configurada para upload no bucket '" + bucket + "'. Configure 'supabase.service-role-key' ou 'supabase.key'.",
                        HttpStatus.UNAUTHORIZED,
                        "STORAGE_AUTH_FAILED",
                        "Falha de Autenticação com Armazenamento"
                );
            }
        }

        // 2. Tentativa de upload remoto para o Supabase Storage
        try {
            byte[] fileBytes = file.getBytes();
            String endpointPath = "/storage/v1/object/" + bucket + "/" + normalizedPath;

            supabaseStorageRestClient.post()
                    .uri(uriBuilder -> uriBuilder.path(endpointPath).build())
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .header("x-upsert", "true")
                    .body(fileBytes)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        String errorBody = new String(response.getBody().readAllBytes());
                        HttpStatusCode status = response.getStatusCode();
                        handleRemoteStorageError(bucket, normalizedPath, status, errorBody);
                    })
                    .toBodilessEntity();

            log.info("Upload concluído com sucesso no Supabase Storage: bucket='{}', path='{}'", bucket, normalizedPath);
            return normalizedPath;

        } catch (IOException e) {
            log.error("Falha de I/O ao ler bytes do arquivo multipart", e);
            throw new InvalidFileException("Não foi possível processar o arquivo enviado para upload.", e);
        } catch (StorageException e) {
            if (localFallbackEnabled) {
                log.warn("[SUPABASE STORAGE LOCAL FALLBACK] Falha na integração remota com Supabase Storage. Ativando fallback local para bucket='{}', path='{}'. Motivo: {}",
                        bucket, normalizedPath, e.getMessage());
                saveFileLocally(bucket, normalizedPath, file);
                return normalizedPath;
            }
            throw e;
        } catch (Exception e) {
            log.error("Erro inesperado ao realizar upload para o Supabase Storage", e);
            boolean isTimeoutOrNetwork = e instanceof org.springframework.web.client.ResourceAccessException
                    || e.getCause() instanceof java.net.SocketTimeoutException
                    || e.getCause() instanceof java.net.ConnectException;

            if (isTimeoutOrNetwork) {
                log.error("[SUPABASE STORAGE TIMEOUT / NETWORK ERROR] Timeout ou falha de conexão com o Supabase Storage (URL: {}). Erro: {}",
                        supabaseProperties.getUrl(), e.getMessage());
            }

            if (localFallbackEnabled) {
                log.warn("[SUPABASE STORAGE LOCAL FALLBACK] Salvando arquivo localmente devido a falha de conexão/timeout com o Supabase Storage: bucket='{}', path='{}'.",
                        bucket, normalizedPath);
                saveFileLocally(bucket, normalizedPath, file);
                return normalizedPath;
            }

            HttpStatus status = isTimeoutOrNetwork ? HttpStatus.GATEWAY_TIMEOUT : HttpStatus.BAD_GATEWAY;
            String code = isTimeoutOrNetwork ? "STORAGE_TIMEOUT" : "STORAGE_COMMUNICATION_ERROR";
            String title = isTimeoutOrNetwork ? "Timeout na Comunicação com Armazenamento" : "Falha na Comunicação com Armazenamento";
            throw new StorageException("Falha na comunicação com o serviço de armazenamento: " + e.getMessage(), e, status, code, title);
        }
    }

    @Override
    public void deleteFile(String bucket, String path) {
        if (!StringUtils.hasText(bucket)) {
            throw new InvalidFileException("O nome do bucket é obrigatório.");
        }
        if (!StringUtils.hasText(path)) {
            throw new InvalidFileException("O caminho do arquivo a ser removido é obrigatório.");
        }

        String normalizedPath = normalizePath(path);
        log.info("Removendo arquivo do bucket='{}', path='{}'", bucket, normalizedPath);

        deleteLocalFileIfExists(bucket, normalizedPath);

        if (!supabaseProperties.isKeyConfigured()) {
            log.info("[SUPABASE STORAGE LOCAL] Chave do Supabase dummy ou ausente. Exclusão remota ignorada para bucket='{}', path='{}'.", bucket, normalizedPath);
            return;
        }

        try {
            Map<String, Object> payload = Map.of("prefixes", List.of(normalizedPath));

            supabaseStorageRestClient.method(org.springframework.http.HttpMethod.DELETE)
                    .uri("/storage/v1/object/{bucket}", bucket)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(payload)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        String errorBody = new String(response.getBody().readAllBytes());
                        log.error("Erro retornado pelo Supabase Storage ao deletar arquivo. Status: {}, Body: {}",
                                response.getStatusCode(), errorBody);
                        throw new StorageException("Erro no Supabase Storage durante a remoção: " + errorBody);
                    })
                    .toBodilessEntity();

            log.info("Arquivo removido com sucesso: bucket='{}', path='{}'", bucket, normalizedPath);

        } catch (StorageException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro inesperado ao deletar arquivo no Supabase Storage", e);
            throw new StorageException("Falha ao remover arquivo do armazenamento: " + e.getMessage(), e);
        }
    }

    @Override
    public String getPublicUrl(String bucket, String path) {
        if (!StringUtils.hasText(bucket)) {
            throw new InvalidFileException("O nome do bucket é obrigatório.");
        }
        if (!StringUtils.hasText(path)) {
            log.warn("[SUPABASE STORAGE] getPublicUrl chamado com path NULO/VAZIO para bucket='{}'. URL incompleta será evitada.", bucket);
            return null;
        }

        String normalizedPath = normalizePath(path);
        if (!StringUtils.hasText(normalizedPath)) {
            log.warn("[SUPABASE STORAGE] getPublicUrl path vazio apos normalizacao. bucket='{}', entrada='{}'", bucket, path);
            return null;
        }

        // Se o arquivo foi salvo localmente no fallback, retorna a URL do endpoint local
        String baseDir = (supabaseProperties.getStorage() != null && StringUtils.hasText(supabaseProperties.getStorage().getLocalDir()))
                ? supabaseProperties.getStorage().getLocalDir()
                : "uploads";
        java.nio.file.Path localFile = java.nio.file.Paths.get(baseDir, bucket, normalizedPath).normalize();

        if (java.nio.file.Files.exists(localFile)) {
            String localBaseUrl = (supabaseProperties.getStorage() != null && StringUtils.hasText(supabaseProperties.getStorage().getLocalBaseUrl()))
                    ? sanitizeBaseUrl(supabaseProperties.getStorage().getLocalBaseUrl())
                    : "http://localhost:8080";
            return String.format("%s/api/v1/storage/local/%s/%s", localBaseUrl, bucket, normalizedPath);
        }

        String baseUrl = sanitizeBaseUrl(supabaseProperties.getUrl());
        // Garantia extra: path NÃO PODE terminar com '/' (URL incompleta!)
        while (normalizedPath.endsWith("/")) normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 1);
        if (!StringUtils.hasText(normalizedPath)) return null;

        String storageBase = (supabaseProperties.getStorage() != null && StringUtils.hasText(supabaseProperties.getStorage().getPublicBaseUrl()))
                ? sanitizeBaseUrl(supabaseProperties.getStorage().getPublicBaseUrl())
                : String.format("%s/storage/v1/object/public", baseUrl);

        return String.format("%s/%s/%s", storageBase, bucket, normalizedPath);
    }

    /**
     * Helper de correção de URL para casos onde o banco gravou file_url INCOMPLETO ou null
     * (aconteceu por mapeamento JPA ambíguo storagePath/filePath nas primeiras versões).
     * Prioridade: (1) fileUrl valido contem nome de arquivo (NAO e so o diretorio do bucket)
     *            (2) filePath válido (monta a URL pública em tempo real)
     * Retorna null se ambos forem inválidos.
     */
    public String resolvePublicUrlFromFields(String bucket, String filePath, String fileUrlFromDb) {
        boolean fileUrlPareceValido = StringUtils.hasText(fileUrlFromDb)
                && fileUrlFromDb.contains(bucket + "/")
                // Para ser valida, depois do nome do bucket tem que vir ALGUM CARACTERE DE CAMINHO
                && fileUrlFromDb.lastIndexOf(bucket + "/") + bucket.length() + 1 < fileUrlFromDb.length();
        if (fileUrlPareceValido) return fileUrlFromDb;

        if (StringUtils.hasText(filePath)) {
            return getPublicUrl(bucket, filePath);
        }
        return null;
    }

    @Override
    public String createSignedUrl(String bucket, String path, int expiresInSeconds) {
        if (!StringUtils.hasText(bucket)) {
            throw new InvalidFileException("O nome do bucket é obrigatório.");
        }
        if (!StringUtils.hasText(path)) {
            throw new InvalidFileException("O caminho do arquivo é obrigatório.");
        }
        if (expiresInSeconds <= 0) {
            throw new IllegalArgumentException("O tempo de expiração da Signed URL deve ser maior que zero.");
        }

        String normalizedPath = normalizePath(path);

        // Se existir localmente, retorna URL local direta
        String baseDir = (supabaseProperties.getStorage() != null && StringUtils.hasText(supabaseProperties.getStorage().getLocalDir()))
                ? supabaseProperties.getStorage().getLocalDir()
                : "uploads";
        java.nio.file.Path localFile = java.nio.file.Paths.get(baseDir, bucket, normalizedPath).normalize();
        if (java.nio.file.Files.exists(localFile)) {
            String localBaseUrl = (supabaseProperties.getStorage() != null && StringUtils.hasText(supabaseProperties.getStorage().getLocalBaseUrl()))
                    ? sanitizeBaseUrl(supabaseProperties.getStorage().getLocalBaseUrl())
                    : "http://localhost:8080";
            return String.format("%s/api/v1/storage/local/%s/%s?expiresIn=%d", localBaseUrl, bucket, normalizedPath, expiresInSeconds);
        }

        boolean localFallbackEnabled = supabaseProperties.getStorage() != null && supabaseProperties.getStorage().isLocalFallback();
        if (!supabaseProperties.isKeyConfigured() && localFallbackEnabled) {
            String localBaseUrl = (supabaseProperties.getStorage() != null && StringUtils.hasText(supabaseProperties.getStorage().getLocalBaseUrl()))
                    ? sanitizeBaseUrl(supabaseProperties.getStorage().getLocalBaseUrl())
                    : "http://localhost:8080";
            return String.format("%s/api/v1/storage/local/%s/%s?expiresIn=%d", localBaseUrl, bucket, normalizedPath, expiresInSeconds);
        }

        log.info("Gerando Signed URL para bucket='{}', path='{}', expiraEm={}s", bucket, normalizedPath, expiresInSeconds);

        try {
            Map<String, Object> payload = Map.of("expiresIn", expiresInSeconds);
            String endpointPath = "/storage/v1/object/sign/" + bucket + "/" + normalizedPath;

            SignedUrlResponse response = supabaseStorageRestClient.post()
                    .uri(uriBuilder -> uriBuilder.path(endpointPath).build())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(payload)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        String errorBody = new String(res.getBody().readAllBytes());
                        HttpStatusCode status = res.getStatusCode();
                        log.error("Erro retornado pelo Supabase Storage ao gerar Signed URL. Status: {}, Body: {}",
                                status, errorBody);
                        if (status.value() == 401 || status.value() == 403) {
                            throw new StorageException("Erro de autenticação ao gerar Signed URL: " + errorBody,
                                    HttpStatus.UNAUTHORIZED, "STORAGE_AUTH_FAILED", "Falha de Autenticação com Armazenamento");
                        } else if (status.value() == 404) {
                            throw new StorageException("Objeto ou bucket não encontrado ao gerar Signed URL: " + errorBody,
                                    HttpStatus.NOT_FOUND, "STORAGE_NOT_FOUND", "Recurso Não Encontrado");
                        }
                        throw new StorageException("Erro ao gerar Signed URL no Supabase Storage: " + errorBody);
                    })
                    .body(SignedUrlResponse.class);

            if (response == null || !StringUtils.hasText(response.getSignedUrl())) {
                throw new StorageException("Resposta inválida do Supabase Storage ao gerar Signed URL.");
            }

            String signedUrl = response.getSignedUrl();
            if (signedUrl.startsWith("http://") || signedUrl.startsWith("https://")) {
                return signedUrl;
            }

            String baseUrl = sanitizeBaseUrl(supabaseProperties.getUrl());
            if (!signedUrl.startsWith("/")) {
                signedUrl = "/" + signedUrl;
            }

            return baseUrl + signedUrl;

        } catch (StorageException e) {
            if (localFallbackEnabled) {
                String localBaseUrl = (supabaseProperties.getStorage() != null && StringUtils.hasText(supabaseProperties.getStorage().getLocalBaseUrl()))
                        ? sanitizeBaseUrl(supabaseProperties.getStorage().getLocalBaseUrl())
                        : "http://localhost:8080";
                return String.format("%s/api/v1/storage/local/%s/%s?expiresIn=%d", localBaseUrl, bucket, normalizedPath, expiresInSeconds);
            }
            throw e;
        } catch (Exception e) {
            log.error("Erro inesperado ao gerar Signed URL no Supabase Storage", e);
            if (localFallbackEnabled) {
                String localBaseUrl = (supabaseProperties.getStorage() != null && StringUtils.hasText(supabaseProperties.getStorage().getLocalBaseUrl()))
                        ? sanitizeBaseUrl(supabaseProperties.getStorage().getLocalBaseUrl())
                        : "http://localhost:8080";
                return String.format("%s/api/v1/storage/local/%s/%s?expiresIn=%d", localBaseUrl, bucket, normalizedPath, expiresInSeconds);
            }
            throw new StorageException("Falha na geração de URL assinada: " + e.getMessage(), e);
        }
    }

    private void handleRemoteStorageError(String bucket, String path, HttpStatusCode status, String errorBody) {
        if (status.value() == 401 || status.value() == 403) {
            log.error("[SUPABASE STORAGE AUTH ERROR] Falha de autenticação no Supabase Storage ao acessar o bucket '{}'. Status: {}, Body: {}. Verifique se 'supabase.service-role-key' ou 'supabase.key' é válida.",
                    bucket, status, errorBody);
            throw new StorageException(
                    "Erro de autenticação no Supabase Storage durante o upload: " + errorBody,
                    HttpStatus.UNAUTHORIZED,
                    "STORAGE_AUTH_FAILED",
                    "Falha de Autenticação com Armazenamento"
            );
        } else if (status.value() == 404 || (errorBody != null && errorBody.toLowerCase().contains("bucket not found"))) {
            log.error("[SUPABASE STORAGE BUCKET NOT FOUND] Bucket '{}' não foi encontrado no Supabase Storage. Status: 404, Body: {}. Certifique-se de que o bucket foi criado no painel do Supabase.",
                    bucket, errorBody);
            throw new StorageException(
                    "Bucket '" + bucket + "' não foi encontrado no Supabase Storage: " + errorBody,
                    HttpStatus.NOT_FOUND,
                    "STORAGE_BUCKET_NOT_FOUND",
                    "Bucket de Armazenamento Não Encontrado"
            );
        } else {
            log.error("Erro retornado pelo Supabase Storage no upload. Status: {}, Body: {}", status, errorBody);
            throw new StorageException("Erro no Supabase Storage durante o upload: " + errorBody);
        }
    }

    private void saveFileLocally(String bucket, String normalizedPath, MultipartFile file) {
        try {
            String baseDir = (supabaseProperties.getStorage() != null && StringUtils.hasText(supabaseProperties.getStorage().getLocalDir()))
                    ? supabaseProperties.getStorage().getLocalDir()
                    : "uploads";
            java.nio.file.Path targetPath = java.nio.file.Paths.get(baseDir, bucket, normalizedPath).normalize();
            if (targetPath.getParent() != null) {
                java.nio.file.Files.createDirectories(targetPath.getParent());
            }
            java.nio.file.Files.write(targetPath, file.getBytes(),
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
            log.info("[SUPABASE STORAGE LOCAL] Arquivo salvo localmente em: {}", targetPath.toAbsolutePath());
        } catch (IOException e) {
            log.error("Falha ao salvar arquivo no armazenamento local de fallback: {}", e.getMessage(), e);
            throw new StorageException("Falha ao salvar arquivo no fallback local: " + e.getMessage(), e,
                    HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_LOCAL_FALLBACK_FAILED", "Falha no Armazenamento Local");
        }
    }

    private void deleteLocalFileIfExists(String bucket, String normalizedPath) {
        try {
            String baseDir = (supabaseProperties.getStorage() != null && StringUtils.hasText(supabaseProperties.getStorage().getLocalDir()))
                    ? supabaseProperties.getStorage().getLocalDir()
                    : "uploads";
            java.nio.file.Path targetPath = java.nio.file.Paths.get(baseDir, bucket, normalizedPath).normalize();
            if (java.nio.file.Files.exists(targetPath)) {
                java.nio.file.Files.delete(targetPath);
                log.info("[SUPABASE STORAGE LOCAL] Arquivo local excluído: {}", targetPath.toAbsolutePath());
            }
        } catch (Exception e) {
            log.warn("Falha não-crítica ao tentar excluir arquivo local: {}", e.getMessage());
        }
    }

    /**
     * Validações fail-fast de integridade, tamanho e formato MIME do arquivo
     */
    private void validateUpload(String bucket, String path, MultipartFile file) {
        if (!StringUtils.hasText(bucket)) {
            throw new InvalidFileException("O nome do bucket deve ser informado.");
        }
        if (!StringUtils.hasText(path)) {
            throw new InvalidFileException("O caminho de destino do arquivo deve ser informado.");
        }
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("O arquivo enviado não pode ser nulo ou vazio.");
        }

        String rawContentType = file.getContentType();
        if (!StringUtils.hasText(rawContentType)) {
            throw new InvalidFileException("O Content-Type do arquivo não foi identificado.");
        }
        String contentType = rawContentType.toLowerCase().trim();

        long fileSize = file.getSize();

        // Mapeamento dos limites e validação estrita por bucket
        if ("site-assets".equalsIgnoreCase(bucket)) {
            if (fileSize > MAX_SIZE_SITE_ASSETS) {
                final double maxMb = MAX_SIZE_SITE_ASSETS / (1024.0 * 1024.0);
                throw new FileSizeExceededException(String.format(
                        "O arquivo excede o limite máximo permitido de %.0f MB para o bucket 'site-assets'. Tamanho enviado: %.2f MB",
                        maxMb, fileSize / (1024.0 * 1024.0)));
            }
            boolean isAllowed = ALLOWED_IMAGE_TYPES.contains(contentType)
                    || ALLOWED_VIDEO_TYPES.contains(contentType)
                    || ALLOWED_PDF_TYPES.contains(contentType);
            if (!isAllowed) {
                throw new InvalidFileException(String.format(
                        "Tipo de arquivo '%s' não permitido para o bucket 'site-assets'. Permitidos: Imagens (JPEG, PNG, WEBP), Vídeos (MP4, WEBM) e PDF.",
                        contentType));
            }
        } else if ("models-media".equalsIgnoreCase(bucket)) {
            if (fileSize > MAX_SIZE_MODELS_MEDIA) {
                final double maxMb = MAX_SIZE_MODELS_MEDIA / (1024.0 * 1024.0);
                throw new FileSizeExceededException(String.format(
                        "O arquivo excede o limite máximo permitido de %.0f MB para o bucket 'models-media'. Tamanho enviado: %.2f MB",
                        maxMb, fileSize / (1024.0 * 1024.0)));
            }
            // 🟢 HOTFINAL: models-media ACEITA IMAGENS (Book/Pola) + PDF (Composite) SEMPRE.
            // A regra de negocio (PDF SOMENTE para composite, nao para book/pola) ja eh
            // ENFORCADA 100% na camada Service ModelMediaServiceImpl:
            //   - uploadModelMedia(BOOK/POLAROID) = nao aceita PDF jamais
            //   - uploadOrReplaceComposite(COMPOSITE) = aceita PDF + imagens
            // Nao precisamos de validacao DUPLICADA aqui que depende de string /composite/ no path.
            boolean isAllowedType = ALLOWED_IMAGE_TYPES.contains(contentType)
                    || ALLOWED_PDF_TYPES.contains(contentType);
            if (!isAllowedType) {
                throw new InvalidFileException(String.format(
                        "Tipo de arquivo '%s' não permitido para o bucket 'models-media'. " +
                        "Formatos permitidos: Imagens (JPEG, PNG, WEBP) para Acervo Visual Book & Polaroids " +
                        "e PDF ou Imagens (JPEG, PNG, WEBP) para Composite Oficial.",
                        contentType));
            }
        } else if ("candidates-uploads".equalsIgnoreCase(bucket)) {
            if (fileSize > MAX_SIZE_CANDIDATES_UPLOADS) {
                final double maxMb = MAX_SIZE_CANDIDATES_UPLOADS / (1024.0 * 1024.0);
                throw new FileSizeExceededException(String.format(
                        "O arquivo excede o limite máximo permitido de %.0f MB para o bucket 'candidates-uploads'. Tamanho enviado: %.2f MB",
                        maxMb, fileSize / (1024.0 * 1024.0)));
            }
            if (!ALLOWED_IMAGE_TYPES.contains(contentType)) {
                throw new InvalidFileException(String.format(
                        "Tipo de arquivo '%s' não permitido para o bucket 'candidates-uploads'. " +
                        "Apenas imagens JPEG, PNG e WEBP são aceitas no formulário de cadastro de candidatas.",
                        contentType));
            }
        } else {
            throw new InvalidFileException(String.format("Bucket '%s' não é suportado para uploads.", bucket));
        }
    }

    private String normalizePath(String path) {
        String cleaned = path.trim().replace("\\", "/");
        while (cleaned.startsWith("/")) {
            cleaned = cleaned.substring(1);
        }
        return cleaned;
    }

    private String sanitizeBaseUrl(String url) {
        if (url == null) {
            return "";
        }
        String trimmed = url.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    public static class SignedUrlResponse {
        @JsonProperty("signedURL")
        @JsonAlias({"signedUrl", "signed_url"})
        private String signedUrl;

        public String getSignedUrl() {
            return signedUrl;
        }

        public void setSignedUrl(String signedUrl) {
            this.signedUrl = signedUrl;
        }
    }
}
