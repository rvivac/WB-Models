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

    // Limites de tamanho em bytes conforme especificação dos buckets
    public static final long MAX_SIZE_SITE_ASSETS = 25L * 1024 * 1024;        // 25 MB
    public static final long MAX_SIZE_MODELS_MEDIA = 8L * 1024 * 1024;        // 8 MB
    public static final long MAX_SIZE_CANDIDATES_UPLOADS = 5L * 1024 * 1024;  // 5 MB

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

    @Override
    public String uploadFile(String bucket, String path, MultipartFile file) {
        log.info("Iniciando upload de arquivo para bucket='{}', path='{}'", bucket, path);

        validateUpload(bucket, path, file);

        String normalizedPath = normalizePath(path);
        String contentType = file.getContentType();

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
                        log.error("Erro retornado pelo Supabase Storage no upload. Status: {}, Body: {}",
                                response.getStatusCode(), errorBody);
                        throw new StorageException("Erro no Supabase Storage durante o upload: " + errorBody);
                    })
                    .toBodilessEntity();

            log.info("Upload concluído com sucesso: bucket='{}', path='{}'", bucket, normalizedPath);
            return normalizedPath;

        } catch (IOException e) {
            log.error("Falha de I/O ao ler bytes do arquivo multipart", e);
            throw new InvalidFileException("Não foi possível processar o arquivo enviado para upload.", e);
        } catch (StorageException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro inesperado ao realizar upload para o Supabase Storage", e);
            throw new StorageException("Falha na comunicação com o serviço de armazenamento: " + e.getMessage(), e);
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
            throw new InvalidFileException("O caminho do arquivo é obrigatório.");
        }

        String baseUrl = sanitizeBaseUrl(supabaseProperties.getUrl());
        String normalizedPath = normalizePath(path);

        return String.format("%s/storage/v1/object/public/%s/%s", baseUrl, bucket, normalizedPath);
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
                        log.error("Erro retornado pelo Supabase Storage ao gerar Signed URL. Status: {}, Body: {}",
                                res.getStatusCode(), errorBody);
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
            throw e;
        } catch (Exception e) {
            log.error("Erro inesperado ao gerar Signed URL no Supabase Storage", e);
            throw new StorageException("Falha na geração de URL assinada: " + e.getMessage(), e);
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
                throw new FileSizeExceededException(String.format(
                        "O arquivo excede o limite máximo permitido de 25 MB para o bucket 'site-assets'. Tamanho enviado: %.2f MB",
                        fileSize / (1024.0 * 1024.0)));
            }
            boolean isAllowed = ALLOWED_IMAGE_TYPES.contains(contentType) || ALLOWED_VIDEO_TYPES.contains(contentType);
            if (!isAllowed) {
                throw new InvalidFileException(String.format(
                        "Tipo de arquivo '%s' não permitido para o bucket 'site-assets'. Permitidos: JPEG, PNG, WEBP, MP4, WEBM.",
                        contentType));
            }
        } else if ("models-media".equalsIgnoreCase(bucket)) {
            if (fileSize > MAX_SIZE_MODELS_MEDIA) {
                throw new FileSizeExceededException(String.format(
                        "O arquivo excede o limite máximo permitido de 8 MB para o bucket 'models-media'. Tamanho enviado: %.2f MB",
                        fileSize / (1024.0 * 1024.0)));
            }
            if (!ALLOWED_IMAGE_TYPES.contains(contentType)) {
                throw new InvalidFileException(String.format(
                        "Tipo de arquivo '%s' não permitido para o bucket 'models-media'. Apenas imagens JPEG, PNG e WEBP são aceitas.",
                        contentType));
            }
        } else if ("candidates-uploads".equalsIgnoreCase(bucket)) {
            if (fileSize > MAX_SIZE_CANDIDATES_UPLOADS) {
                throw new FileSizeExceededException(String.format(
                        "O arquivo excede o limite máximo permitido de 5 MB para o bucket 'candidates-uploads'. Tamanho enviado: %.2f MB",
                        fileSize / (1024.0 * 1024.0)));
            }
            if (!ALLOWED_IMAGE_TYPES.contains(contentType)) {
                throw new InvalidFileException(String.format(
                        "Tipo de arquivo '%s' não permitido para o bucket 'candidates-uploads'. Apenas imagens JPEG, PNG e WEBP são aceitas.",
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
