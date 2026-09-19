package com.wbscouting.api.service.publicapi;

import com.wbscouting.api.dto.publicapi.ModelCardPublicDto;
import com.wbscouting.api.dto.publicapi.PageResponseDto;
import com.wbscouting.api.entity.Model;
import com.wbscouting.api.entity.ModelMedia;
import com.wbscouting.api.enums.GenderType;
import com.wbscouting.api.enums.MediaType;
import com.wbscouting.api.repository.ModelMediaRepository;
import com.wbscouting.api.repository.ModelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PublicModelCatalogServiceImplTest {

    @Mock
    private ModelRepository modelRepository;

    @Mock
    private ModelMediaRepository modelMediaRepository;

    @InjectMocks
    private PublicModelCatalogServiceImpl catalogService;

    private UUID modelId1;
    private UUID modelId2;
    private Model model1;
    private Model model2;

    @BeforeEach
    void setUp() {
        modelId1 = UUID.randomUUID();
        modelId2 = UUID.randomUUID();

        model1 = Model.builder()
                .id(modelId1)
                .stageName("Alessandra Ambrosio")
                .gender(GenderType.FEMALE)
                .heightCm(177)
                .city("Erechim")
                .isStar(true)
                .isActive(true)
                .primaryPhotoUrl("https://supabase.co/primary1.jpg")
                .build();

        model2 = Model.builder()
                .id(modelId2)
                .stageName("Francisco Lachowski")
                .gender(GenderType.MALE)
                .heightCm(189)
                .city("Curitiba")
                .isStar(false)
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Deve listar modelos ativos e resolver foto de capa priorizando is_cover e primeira BOOK sem N+1")
    void shouldListActiveModelsWithPaginationAndCoverPhoto() {
        Page<Model> page = new PageImpl<>(List.of(model1, model2));
        when(modelRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        // Model 1 possui foto is_cover = true
        ModelMedia coverMediaModel1 = ModelMedia.builder()
                .id(UUID.randomUUID())
                .model(model1)
                .mediaType(MediaType.BOOK)
                .fileUrl("https://supabase.co/cover-alessandra.jpg")
                .displayOrder(1)
                .isCover(true)
                .isActive(true)
                .build();

        // Model 2 não tem is_cover, mas tem foto BOOK no displayOrder 1
        ModelMedia bookMediaModel2 = ModelMedia.builder()
                .id(UUID.randomUUID())
                .model(model2)
                .mediaType(MediaType.BOOK)
                .fileUrl("https://supabase.co/book-francisco.jpg")
                .displayOrder(1)
                .isCover(false)
                .isActive(true)
                .build();

        when(modelMediaRepository.findByModelIdInAndIsActiveTrueOrderByDisplayOrderAsc(List.of(modelId1, modelId2)))
                .thenReturn(List.of(coverMediaModel1, bookMediaModel2));

        PageResponseDto<ModelCardPublicDto> result = catalogService.listModels(
                GenderType.FEMALE, true, "Alessandra", 0, 24, "stageName,asc"
        );

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);

        ModelCardPublicDto card1 = result.getContent().get(0);
        assertThat(card1.getId()).isEqualTo(modelId1);
        assertThat(card1.getStageName()).isEqualTo("Alessandra Ambrosio");
        assertThat(card1.getCoverImageUrl()).isEqualTo("https://supabase.co/cover-alessandra.jpg");

        ModelCardPublicDto card2 = result.getContent().get(1);
        assertThat(card2.getId()).isEqualTo(modelId2);
        assertThat(card2.getStageName()).isEqualTo("Francisco Lachowski");
        assertThat(card2.getCoverImageUrl()).isEqualTo("https://supabase.co/book-francisco.jpg");

        // Verifica que realizou apenas UMA busca em lote de mídias (prevenção de N+1)
        verify(modelMediaRepository, times(1)).findByModelIdInAndIsActiveTrueOrderByDisplayOrderAsc(anyList());
    }

    @Test
    @DisplayName("Deve usar fallback para primaryPhotoUrl se modelo não tiver fotos de mídia")
    void shouldFallbackToPrimaryPhotoUrlWhenNoMedia() {
        Page<Model> page = new PageImpl<>(List.of(model1));
        when(modelRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(modelMediaRepository.findByModelIdInAndIsActiveTrueOrderByDisplayOrderAsc(List.of(modelId1)))
                .thenReturn(Collections.emptyList());

        PageResponseDto<ModelCardPublicDto> result = catalogService.listModels(null, null, null, 0, 24, "stageName,asc");

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCoverImageUrl()).isEqualTo("https://supabase.co/primary1.jpg");
    }

    @Test
    @DisplayName("Deve limitar o tamanho de página ao máximo de 48 itens")
    void shouldEnforceMaxPageSizeOf48() {
        when(modelRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        catalogService.listModels(null, null, null, 0, 100, "stageName,asc");

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(modelRepository).findAll(any(Specification.class), captor.capture());

        assertThat(captor.getValue().getPageSize()).isEqualTo(48);
    }

    @Test
    @DisplayName("Deve retornar página vazia sem consultar mídias quando não houver modelos")
    void shouldReturnEmptyPageWhenNoModelsFound() {
        when(modelRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        PageResponseDto<ModelCardPublicDto> result = catalogService.listModels(null, null, null, 0, 24, null);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        verify(modelMediaRepository, never()).findByModelIdInAndIsActiveTrueOrderByDisplayOrderAsc(anyList());
    }
}
