package com.wbscouting.api.service.content;

import com.wbscouting.api.config.SupabaseProperties;
import com.wbscouting.api.dto.content.AssetUploadResponseDto;
import com.wbscouting.api.dto.content.SiteContentAdminDto;
import com.wbscouting.api.dto.content.SiteContentPublicDto;
import com.wbscouting.api.dto.content.SiteContentUpdateRequestDto;
import com.wbscouting.api.entity.SiteContent;
import com.wbscouting.api.exception.FileSizeExceededException;
import com.wbscouting.api.exception.InvalidFileException;
import com.wbscouting.api.exception.ResourceNotFoundException;
import com.wbscouting.api.repository.SiteContentRepository;
import com.wbscouting.api.service.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SiteContentServiceImplTest {

    @Mock
    private SiteContentRepository siteContentRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private SupabaseProperties supabaseProperties;

    @InjectMocks
    private SiteContentServiceImpl siteContentService;

    private SiteContent heroContent;
    private UUID adminId;

    @BeforeEach
    void setUp() {
        adminId = UUID.randomUUID();

        heroContent = SiteContent.builder()
                .id(UUID.randomUUID())
                .sectionKey("HOME_HERO")
                .payloadPt(Map.of("title", "Descubra Novos Talentos", "ctaText", "Ver Casting"))
                .payloadEn(Map.of("title", "Discover New Faces", "ctaText", "View Casting"))
                .mediaUrls(Map.of("videoUrl", "https://supabase.co/video.mp4", "posterUrl", "https://supabase.co/poster.jpg"))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .updatedBy(adminId)
                .build();
    }

    @Test
    @DisplayName("Deve retornar conteúdo desempacotado em Português por padrão")
    void getPublicContent_PortugueseDefault() {
        when(siteContentRepository.findBySectionKey("HOME_HERO")).thenReturn(Optional.of(heroContent));

        SiteContentPublicDto dto = siteContentService.getPublicContent("HOME_HERO", "pt");

        assertThat(dto).isNotNull();
        assertThat(dto.getSectionKey()).isEqualTo("HOME_HERO");
        assertThat(dto.getLang()).isEqualTo("pt");
        assertThat(dto.getPayload()).containsEntry("title", "Descubra Novos Talentos");
        assertThat(dto.getMediaUrls()).containsEntry("videoUrl", "https://supabase.co/video.mp4");
    }

    @Test
    @DisplayName("Deve retornar conteúdo desempacotado em Inglês quando solicitado")
    void getPublicContent_English() {
        when(siteContentRepository.findBySectionKey("HOME_HERO")).thenReturn(Optional.of(heroContent));

        SiteContentPublicDto dto = siteContentService.getPublicContent("HOME_HERO", "en");

        assertThat(dto).isNotNull();
        assertThat(dto.getSectionKey()).isEqualTo("HOME_HERO");
        assertThat(dto.getLang()).isEqualTo("en");
        assertThat(dto.getPayload()).containsEntry("title", "Discover New Faces");
    }

    @Test
    @DisplayName("Deve realizar fallback para Português quando o conteúdo em Inglês estiver vazio")
    void getPublicContent_EnglishFallbackToPt() {
        heroContent.setPayloadEn(Collections.emptyMap());
        when(siteContentRepository.findBySectionKey("HOME_HERO")).thenReturn(Optional.of(heroContent));

        SiteContentPublicDto dto = siteContentService.getPublicContent("HOME_HERO", "en");

        assertThat(dto).isNotNull();
        assertThat(dto.getPayload()).containsEntry("title", "Descubra Novos Talentos");
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException para chave de seção inexistente")
    void getPublicContent_NotFound_ThrowsException() {
        when(siteContentRepository.findBySectionKey("INVALID_KEY")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> siteContentService.getPublicContent("INVALID_KEY", "pt"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Conteúdo da seção não encontrado: INVALID_KEY");
    }

    @Test
    @DisplayName("Deve consolidar mapa de todas as seções públicas no idioma solicitado")
    void getAllPublicContent_ReturnsConsolidatedMap() {
        when(siteContentRepository.findAll()).thenReturn(List.of(heroContent));

        Map<String, Map<String, Object>> result = siteContentService.getAllPublicContent("pt");

        assertThat(result).isNotNull();
        assertThat(result).containsKey("HOME_HERO");
        assertThat(result.get("HOME_HERO")).containsEntry("title", "Descubra Novos Talentos");
    }

    @Test
    @DisplayName("Deve listar todos os conteúdos institucionais para o painel administrativo")
    void getAllAdminContent_ReturnsList() {
        when(siteContentRepository.findAll()).thenReturn(List.of(heroContent));

        List<SiteContentAdminDto> list = siteContentService.getAllAdminContent();

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getSectionKey()).isEqualTo("HOME_HERO");
        assertThat(list.get(0).getPayloadPt()).containsEntry("title", "Descubra Novos Talentos");
        assertThat(list.get(0).getPayloadEn()).containsEntry("title", "Discover New Faces");
    }

    @Test
    @DisplayName("Deve atualizar (upsert) conteúdo institucional existente com sucesso")
    void updateContent_ExistingSection_UpdatesSuccessfully() {
        when(siteContentRepository.findBySectionKey("HOME_HERO")).thenReturn(Optional.of(heroContent));
        when(siteContentRepository.save(any(SiteContent.class))).thenAnswer(inv -> inv.getArgument(0));

        SiteContentUpdateRequestDto request = SiteContentUpdateRequestDto.builder()
                .payloadPt(Map.of("title", "Novo Título PT"))
                .payloadEn(Map.of("title", "New Title EN"))
                .mediaUrls(Map.of("videoUrl", "https://supabase.co/new-video.mp4"))
                .build();

        SiteContentAdminDto updated = siteContentService.updateContent("HOME_HERO", request, adminId);

        assertThat(updated).isNotNull();
        assertThat(updated.getPayloadPt()).containsEntry("title", "Novo Título PT");
        assertThat(updated.getPayloadEn()).containsEntry("title", "New Title EN");
        assertThat(updated.getUpdatedBy()).isEqualTo(adminId);
    }

    @Test
    @DisplayName("Deve criar (upsert) nova seção de conteúdo quando ainda não existir")
    void updateContent_NewSection_CreatesSuccessfully() {
        when(siteContentRepository.findBySectionKey("ABOUT_US")).thenReturn(Optional.empty());
        when(siteContentRepository.save(any(SiteContent.class))).thenAnswer(inv -> inv.getArgument(0));

        SiteContentUpdateRequestDto request = SiteContentUpdateRequestDto.builder()
                .payloadPt(Map.of("manifesto", "Nossa história"))
                .payloadEn(Map.of("manifesto", "Our story"))
                .build();

        SiteContentAdminDto created = siteContentService.updateContent("ABOUT_US", request, adminId);

        assertThat(created).isNotNull();
        assertThat(created.getSectionKey()).isEqualTo("ABOUT_US");
        assertThat(created.getPayloadPt()).containsEntry("manifesto", "Nossa história");
        assertThat(created.getUpdatedBy()).isEqualTo(adminId);
    }

    @Test
    @DisplayName("Deve fazer upload de imagem institucional válida (<= 10MB) com sucesso")
    void uploadAsset_ValidImage_Success() {
        MockMultipartFile image = new MockMultipartFile(
                "file", "banner.webp", "image/webp", new byte[1024]
        );

        when(storageService.uploadFile(eq("site-assets"), anyString(), eq(image))).thenReturn("assets/uuid-banner.webp");
        when(storageService.getPublicUrl(eq("site-assets"), anyString())).thenReturn("https://supabase.co/site-assets/assets/uuid-banner.webp");

        AssetUploadResponseDto response = siteContentService.uploadAsset(image, "home");

        assertThat(response).isNotNull();
        assertThat(response.getFileUrl()).isEqualTo("https://supabase.co/site-assets/assets/uuid-banner.webp");
        assertThat(response.getFileType()).isEqualTo("image/webp");
        verify(storageService).uploadFile(eq("site-assets"), anyString(), eq(image));
    }

    @Test
    @DisplayName("Deve fazer upload de vídeo institucional válido (<= 25MB) com sucesso")
    void uploadAsset_ValidVideo_Success() {
        MockMultipartFile video = new MockMultipartFile(
                "file", "hero.mp4", "video/mp4", new byte[2048]
        );

        when(storageService.uploadFile(eq("site-assets"), anyString(), eq(video))).thenReturn("assets/uuid-hero.mp4");
        when(storageService.getPublicUrl(eq("site-assets"), anyString())).thenReturn("https://supabase.co/site-assets/assets/uuid-hero.mp4");

        AssetUploadResponseDto response = siteContentService.uploadAsset(video, "hero");

        assertThat(response).isNotNull();
        assertThat(response.getFileUrl()).isEqualTo("https://supabase.co/site-assets/assets/uuid-hero.mp4");
        assertThat(response.getFileType()).isEqualTo("video/mp4");
    }

    @Test
    @DisplayName("Deve lançar FileSizeExceededException se a imagem exceder 10MB")
    void uploadAsset_ImageExceeds10MB_ThrowsException() {
        MockMultipartFile largeImage = new MockMultipartFile(
                "file", "large.png", "image/png", new byte[11 * 1024 * 1024]
        );

        assertThatThrownBy(() -> siteContentService.uploadAsset(largeImage, "banners"))
                .isInstanceOf(FileSizeExceededException.class)
                .hasMessageContaining("excede o limite máximo permitido de 10 MB");

        verify(storageService, never()).uploadFile(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Deve lançar FileSizeExceededException se o vídeo exceder 25MB")
    void uploadAsset_VideoExceeds25MB_ThrowsException() {
        MockMultipartFile largeVideo = new MockMultipartFile(
                "file", "large.mp4", "video/mp4", new byte[26 * 1024 * 1024]
        );

        assertThatThrownBy(() -> siteContentService.uploadAsset(largeVideo, "hero"))
                .isInstanceOf(FileSizeExceededException.class)
                .hasMessageContaining("excede o limite máximo permitido de 25 MB");

        verify(storageService, never()).uploadFile(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Deve lançar InvalidFileException para formato de arquivo não suportado")
    void uploadAsset_InvalidType_ThrowsException() {
        MockMultipartFile pdf = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", new byte[100]
        );

        assertThatThrownBy(() -> siteContentService.uploadAsset(pdf, "docs"))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("não suportado para o bucket 'site-assets'");

        verify(storageService, never()).uploadFile(anyString(), anyString(), any());
    }
}
