package com.wbscouting.api.service.publicapi;

import com.wbscouting.api.dto.publicapi.ModelCardPublicDto;
import com.wbscouting.api.dto.publicapi.ModelDetailPublicDto;
import com.wbscouting.api.dto.publicapi.ModelMediaPublicItemDto;
import com.wbscouting.api.dto.publicapi.PageResponseDto;
import com.wbscouting.api.entity.Model;
import com.wbscouting.api.entity.ModelMedia;
import com.wbscouting.api.enums.GenderType;
import com.wbscouting.api.enums.MediaType;
import com.wbscouting.api.exception.ResourceNotFoundException;
import com.wbscouting.api.repository.ModelMediaRepository;
import com.wbscouting.api.repository.ModelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicModelServiceImplTest {

    @Mock
    private ModelRepository modelRepository;

    @Mock
    private ModelMediaRepository modelMediaRepository;

    @Mock
    private PublicModelDetailService publicModelDetailService;

    @InjectMocks
    private PublicModelServiceImpl publicModelService;

    private UUID modelId;
    private Model model;

    @BeforeEach
    void setUp() {
        modelId = UUID.randomUUID();

        model = Model.builder()
                .id(modelId)
                .stageName("Isabeli Fontana")
                .gender(GenderType.FEMALE)
                .city("Curitiba")
                .nationality("Brasileira")
                .birthDate(LocalDate.now().minusYears(25))
                .heightCm(178)
                .bustChestCm(new BigDecimal("86.00"))
                .waistCm(new BigDecimal("60.00"))
                .hipsCm(new BigDecimal("89.00"))
                .dressSize("36")
                .shoeSize("38")
                .eyesColor("Azul")
                .hairColor("Castanho")
                .instagramUrl("https://instagram.com/isabeli")
                .isFeaturedHome(true)
                .featuredOrder(1)
                .isStar(true)
                .isActive(true)
                .primaryPhotoUrl("https://supabase.co/models-media/cover.jpg")
                .build();
    }

    @Test
    @DisplayName("Deve retornar lista de modelos em destaque para a home pública")
    void getFeaturedModels_Success() {
        when(modelRepository.findFeaturedHomeModels()).thenReturn(List.of(model));

        List<ModelCardPublicDto> result = publicModelService.getFeaturedModels();

        assertThat(result).hasSize(1);
        ModelCardPublicDto card = result.get(0);
        assertThat(card.getId()).isEqualTo(modelId);
        assertThat(card.getStageName()).isEqualTo("Isabeli Fontana");
        assertThat(card.getCoverImageUrl()).isEqualTo("https://supabase.co/models-media/cover.jpg");
        assertThat(card.getIsStar()).isTrue();
    }

    @Test
    @DisplayName("Deve listar casting público com paginação e filtros")
    void listModels_Success() {
        Page<Model> page = new PageImpl<>(List.of(model));
        when(modelRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResponseDto<ModelCardPublicDto> response = publicModelService.listModels(GenderType.FEMALE, true, 0, 24);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getPageNumber()).isEqualTo(0);
        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getContent().get(0).getStageName()).isEqualTo("Isabeli Fontana");
    }

    @Test
    @DisplayName("Deve delegar busca de detalhes do modelo para o PublicModelDetailService")
    void getModelDetail_Success() {
        ModelDetailPublicDto expectedDetail = ModelDetailPublicDto.builder()
                .id(modelId)
                .stageName("Isabeli Fontana")
                .age(25)
                .eyeColor("Azul")
                .bookPhotos(List.of(ModelMediaPublicItemDto.builder()
                        .id(UUID.randomUUID())
                        .fileUrl("https://supabase.co/book-1.jpg")
                        .displayOrder(1)
                        .isCover(true)
                        .build()))
                .polaroids(List.of(ModelMediaPublicItemDto.builder()
                        .id(UUID.randomUUID())
                        .fileUrl("https://supabase.co/polaroid-1.jpg")
                        .displayOrder(1)
                        .isCover(false)
                        .build()))
                .composite(ModelMediaPublicItemDto.builder()
                        .id(UUID.randomUUID())
                        .fileUrl("https://supabase.co/comp-card.jpg")
                        .displayOrder(1)
                        .isCover(false)
                        .build())
                .build();

        when(publicModelDetailService.getModelDetail(modelId)).thenReturn(expectedDetail);

        ModelDetailPublicDto detail = publicModelService.getModelDetail(modelId);

        assertThat(detail).isNotNull();
        assertThat(detail.getId()).isEqualTo(modelId);
        assertThat(detail.getStageName()).isEqualTo("Isabeli Fontana");
        assertThat(detail.getAge()).isEqualTo(25);
        assertThat(detail.getEyeColor()).isEqualTo("Azul");
        assertThat(detail.getBookPhotos()).hasSize(1);
        assertThat(detail.getBookPhotos().get(0).getFileUrl()).isEqualTo("https://supabase.co/book-1.jpg");
        assertThat(detail.getPolaroids()).hasSize(1);
        assertThat(detail.getPolaroids().get(0).getFileUrl()).isEqualTo("https://supabase.co/polaroid-1.jpg");
        assertThat(detail.getCompositeUrl()).isEqualTo("https://supabase.co/comp-card.jpg");
    }

    @Test
    @DisplayName("Deve propagar ResourceNotFoundException quando o modelo não for encontrado pelo PublicModelDetailService")
    void getModelDetail_NotFound_ThrowsException() {
        when(publicModelDetailService.getModelDetail(modelId))
                .thenThrow(new ResourceNotFoundException("Modelo não encontrado ou inativo"));

        assertThatThrownBy(() -> publicModelService.getModelDetail(modelId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Modelo não encontrado ou inativo");
    }

    @Test
    @DisplayName("Deve resolver capa usando fallback para a primeira foto do book quando primaryPhotoUrl for nulo")
    void resolveCoverImageUrl_FallbackToBookPhoto() {
        model.setPrimaryPhotoUrl(null);
        ModelMedia bookMedia = ModelMedia.builder()
                .id(UUID.randomUUID())
                .model(model)
                .mediaType(MediaType.BOOK)
                .fileUrl("https://supabase.co/first-book.jpg")
                .displayOrder(1)
                .isActive(true)
                .build();
        model.setMedia(List.of(bookMedia));

        when(modelRepository.findFeaturedHomeModels()).thenReturn(List.of(model));

        List<ModelCardPublicDto> result = publicModelService.getFeaturedModels();

        assertThat(result.get(0).getCoverImageUrl()).isEqualTo("https://supabase.co/first-book.jpg");
    }
}
