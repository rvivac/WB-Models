package com.wbscouting.api.service.storage;

import com.wbscouting.api.config.SupabaseProperties;
import com.wbscouting.api.exception.FileSizeExceededException;
import com.wbscouting.api.exception.InvalidFileException;
import com.wbscouting.api.exception.StorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class SupabaseStorageServiceTest {

    private SupabaseProperties properties;
    private RestClient restClient;
    private MockRestServiceServer mockServer;
    private SupabaseStorageService storageService;

    @BeforeEach
    void setUp() {
        properties = new SupabaseProperties();
        properties.setUrl("https://testref.supabase.co");
        properties.setServiceRoleKey("test-service-role-key");
        properties.setAnonKey("test-anon-key");

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(properties.getUrl())
                .defaultHeader("apikey", properties.getServiceRoleKey())
                .defaultHeader("Authorization", "Bearer " + properties.getServiceRoleKey());

        mockServer = MockRestServiceServer.bindTo(builder).build();
        restClient = builder.build();

        storageService = new SupabaseStorageService(restClient, properties);
    }

    @Test
    @DisplayName("Deve gerar URL pública correta para bucket público")
    void shouldGenerateCorrectPublicUrl() {
        String url = storageService.getPublicUrl("models-media", "model-123/banner.webp");
        assertThat(url).isEqualTo("https://testref.supabase.co/storage/v1/object/public/models-media/model-123/banner.webp");
    }

    @Test
    @DisplayName("Deve normalizar caminhos com barras invertidas ou barra inicial na URL pública")
    void shouldNormalizePathInPublicUrl() {
        String url = storageService.getPublicUrl("site-assets", "/hero\\banner.jpg");
        assertThat(url).isEqualTo("https://testref.supabase.co/storage/v1/object/public/site-assets/hero/banner.jpg");
    }

    @Test
    @DisplayName("Deve rejeitar upload com arquivo nulo ou vazio")
    void shouldRejectEmptyFile() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> storageService.uploadFile("candidates-uploads", "test.jpg", emptyFile))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("vazio");
    }

    @Test
    @DisplayName("Deve rejeitar arquivo que excede 5MB no bucket candidates-uploads")
    void shouldRejectCandidateFileExceeding5MB() {
        byte[] largeBytes = new byte[(int) (SupabaseStorageService.MAX_SIZE_CANDIDATES_UPLOADS + 1024)];
        MockMultipartFile largeFile = new MockMultipartFile("file", "photo.jpg", "image/jpeg", largeBytes);

        assertThatThrownBy(() -> storageService.uploadFile("candidates-uploads", "cand-1/photo.jpg", largeFile))
                .isInstanceOf(FileSizeExceededException.class)
                .hasMessageContaining("5 MB");
    }

    @Test
    @DisplayName("Deve rejeitar arquivo que excede 8MB no bucket models-media")
    void shouldRejectModelsMediaFileExceeding8MB() {
        byte[] largeBytes = new byte[(int) (SupabaseStorageService.MAX_SIZE_MODELS_MEDIA + 1024)];
        MockMultipartFile largeFile = new MockMultipartFile("file", "model.png", "image/png", largeBytes);

        assertThatThrownBy(() -> storageService.uploadFile("models-media", "mod-1/photo.png", largeFile))
                .isInstanceOf(FileSizeExceededException.class)
                .hasMessageContaining("8 MB");
    }

    @Test
    @DisplayName("Deve rejeitar arquivo que excede 25MB no bucket site-assets")
    void shouldRejectSiteAssetFileExceeding25MB() {
        byte[] largeBytes = new byte[(int) (SupabaseStorageService.MAX_SIZE_SITE_ASSETS + 1024)];
        MockMultipartFile largeFile = new MockMultipartFile("file", "video.mp4", "video/mp4", largeBytes);

        assertThatThrownBy(() -> storageService.uploadFile("site-assets", "videos/promo.mp4", largeFile))
                .isInstanceOf(FileSizeExceededException.class)
                .hasMessageContaining("25 MB");
    }

    @Test
    @DisplayName("Deve rejeitar vídeo enviado para o bucket candidates-uploads")
    void shouldRejectVideoInCandidateBucket() {
        MockMultipartFile videoFile = new MockMultipartFile("file", "video.mp4", "video/mp4", "fake-video-content".getBytes());

        assertThatThrownBy(() -> storageService.uploadFile("candidates-uploads", "cand-1/video.mp4", videoFile))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("não permitido");
    }

    @Test
    @DisplayName("Deve permitir vídeo com tipo aceito no bucket site-assets")
    void shouldAcceptVideoInSiteAssetsBucket() {
        MockMultipartFile videoFile = new MockMultipartFile("file", "intro.mp4", "video/mp4", "fake-video-content".getBytes());

        mockServer.expect(requestTo("https://testref.supabase.co/storage/v1/object/site-assets/videos/intro.mp4"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", "video/mp4"))
                .andExpect(header("x-upsert", "true"))
                .andRespond(withSuccess());

        String path = storageService.uploadFile("site-assets", "videos/intro.mp4", videoFile);
        assertThat(path).isEqualTo("videos/intro.mp4");
        mockServer.verify();
    }

    @Test
    @DisplayName("Deve rejeitar arquivo com tipo MIME não suportado (ex: PDF)")
    void shouldRejectUnsupportedMimeType() {
        MockMultipartFile pdfFile = new MockMultipartFile("file", "doc.pdf", "application/pdf", "fake-pdf".getBytes());

        assertThatThrownBy(() -> storageService.uploadFile("site-assets", "docs/file.pdf", pdfFile))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("não permitido");
    }

    @Test
    @DisplayName("Deve rejeitar bucket inválido")
    void shouldRejectInvalidBucket() {
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", "fake-image".getBytes());

        assertThatThrownBy(() -> storageService.uploadFile("unknown-bucket", "test.jpg", file))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("não é suportado");
    }

    @Test
    @DisplayName("Deve realizar upload de imagem com sucesso no bucket candidates-uploads")
    void shouldUploadCandidatePhotoSuccessfully() {
        MockMultipartFile file = new MockMultipartFile("file", "front.webp", "image/webp", "image-content".getBytes());

        mockServer.expect(requestTo("https://testref.supabase.co/storage/v1/object/candidates-uploads/c1/front.webp"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", "image/webp"))
                .andExpect(header("x-upsert", "true"))
                .andRespond(withSuccess());

        String uploadedPath = storageService.uploadFile("candidates-uploads", "c1/front.webp", file);
        assertThat(uploadedPath).isEqualTo("c1/front.webp");
        mockServer.verify();
    }

    @Test
    @DisplayName("Deve lançar StorageException se o Supabase retornar erro no upload")
    void shouldThrowStorageExceptionOnUploadError() {
        MockMultipartFile file = new MockMultipartFile("file", "front.jpg", "image/jpeg", "image-content".getBytes());

        mockServer.expect(requestTo("https://testref.supabase.co/storage/v1/object/candidates-uploads/c1/front.jpg"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError().body("Internal Supabase Error"));

        assertThatThrownBy(() -> storageService.uploadFile("candidates-uploads", "c1/front.jpg", file))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("Erro no Supabase Storage durante o upload");

        mockServer.verify();
    }

    @Test
    @DisplayName("Deve deletar arquivo com sucesso enviando payload JSON de prefixes")
    void shouldDeleteFileSuccessfully() {
        mockServer.expect(requestTo("https://testref.supabase.co/storage/v1/object/candidates-uploads"))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().json("{\"prefixes\":[\"c1/front.jpg\"]}"))
                .andRespond(withSuccess());

        storageService.deleteFile("candidates-uploads", "c1/front.jpg");
        mockServer.verify();
    }

    @Test
    @DisplayName("Deve gerar Signed URL e concatenar com baseUrl se o Supabase retornar path relativo")
    void shouldCreateSignedUrlWithRelativePath() {
        String supabaseResponse = "{\"signedURL\":\"/storage/v1/object/sign/candidates-uploads/c1/photo.jpg?token=secret123\"}";

        mockServer.expect(requestTo("https://testref.supabase.co/storage/v1/object/sign/candidates-uploads/c1/photo.jpg"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().json("{\"expiresIn\":300}"))
                .andRespond(withSuccess(supabaseResponse, MediaType.APPLICATION_JSON));

        String signedUrl = storageService.createSignedUrl("candidates-uploads", "c1/photo.jpg", 300);
        assertThat(signedUrl)
                .isEqualTo("https://testref.supabase.co/storage/v1/object/sign/candidates-uploads/c1/photo.jpg?token=secret123");

        mockServer.verify();
    }

    @Test
    @DisplayName("Deve rejeitar Signed URL com tempo de expiração inválido")
    void shouldRejectInvalidExpirationForSignedUrl() {
        assertThatThrownBy(() -> storageService.createSignedUrl("candidates-uploads", "c1/photo.jpg", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maior que zero");
    }
}
