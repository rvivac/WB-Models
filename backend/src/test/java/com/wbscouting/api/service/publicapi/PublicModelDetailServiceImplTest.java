package com.wbscouting.api.service.publicapi;

import com.wbscouting.api.dto.publicapi.ModelDetailPublicDto;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PublicModelDetailServiceImplTest {

    @Mock
    private ModelRepository modelRepository;

    @Mock
    private ModelMediaRepository modelMediaRepository;

    @InjectMocks
    private PublicModelDetailServiceImpl publicModelDetailService;

    private UUID modelId;
    private Model model;

    @BeforeEach
    void setUp() {
        modelId = UUID.randomUUID();
        model = Model.builder()
                .id(modelId)
                .stageName("Lais Ribeiro")
                .gender(GenderType.FEMALE)
                .isStar(true)
                .city("Miguel Alves")
                .nationality("Brasileira")
                .birthDate(LocalDate.now().minusYears(33))
                .instagramUrl("https://instagram.com/laisribeiro")
                .heightCm(180)
                .bustChestCm(new BigDecimal("84.00"))
                .waistCm(new BigDecimal("59.00"))
                .hipsCm(new BigDecimal("88.00"))
                .dressSize("36")
                .shoeSize("39")
                .eyesColor("Castanhos")
                .hairColor("Castanho Escuro")
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Deve retornar detalhes públicos do modelo com cálculo de idade e mídias agrupadas (Book, Polaroid, Composite)")
    void shouldReturnDetailWithCategorizedMediaAndCalculatedAge() {
        when(modelRepository.findByIdAndIsActiveTrue(modelId)).thenReturn(Optional.of(model));

        ModelMedia book1 = ModelMedia.builder()
                .id(UUID.randomUUID())
                .model(model)
                .mediaType(MediaType.BOOK)
                .fileUrl("https://supabase.co/book1.jpg")
                .displayOrder(1)
                .isCover(true)
                .isActive(true)
                .build();

        ModelMedia book2 = ModelMedia.builder()
                .id(UUID.randomUUID())
                .model(model)
                .mediaType(MediaType.BOOK)
                .fileUrl("https://supabase.co/book2.jpg")
                .displayOrder(2)
                .isCover(false)
                .isActive(true)
                .build();

        ModelMedia polaroid1 = ModelMedia.builder()
                .id(UUID.randomUUID())
                .model(model)
                .mediaType(MediaType.POLAROID)
                .fileUrl("https://supabase.co/pol1.jpg")
                .displayOrder(1)
                .isActive(true)
                .build();

        ModelMedia composite = ModelMedia.builder()
                .id(UUID.randomUUID())
                .model(model)
                .mediaType(MediaType.COMPOSITE)
                .fileUrl("https://supabase.co/composite.jpg")
                .displayOrder(1)
                .isActive(true)
                .build();

        when(modelMediaRepository.findByModelIdAndModelIsActiveTrueOrderByDisplayOrderAsc(modelId))
                .thenReturn(List.of(book1, book2, polaroid1, composite));

        ModelDetailPublicDto result = publicModelDetailService.getModelDetail(modelId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(modelId);
        assertThat(result.getStageName()).isEqualTo("Lais Ribeiro");
        assertThat(result.getAge()).isEqualTo(33);
        assertThat(result.getIsStar()).isTrue();
        assertThat(result.getCity()).isEqualTo("Miguel Alves");
        assertThat(result.getNationality()).isEqualTo("Brasileira");
        assertThat(result.getHeightCm()).isEqualTo(180);
        assertThat(result.getEyeColor()).isEqualTo("Castanhos");
        assertThat(result.getHairColor()).isEqualTo("Castanho Escuro");

        assertThat(result.getInstagramUrl()).isEqualTo("https://instagram.com/laisribeiro");
        assertThat(result.getInstagramHandle()).isEqualTo("@laisribeiro");

        // Valida agrupamento de mídias
        assertThat(result.getBookPhotos()).hasSize(2);
        assertThat(result.getBookPhotos().get(0).getFileUrl()).isEqualTo("https://supabase.co/book1.jpg");
        assertThat(result.getBookPhotos().get(0).getIsCover()).isTrue();

        assertThat(result.getPolaroids()).hasSize(1);
        assertThat(result.getPolaroids().get(0).getFileUrl()).isEqualTo("https://supabase.co/pol1.jpg");

        assertThat(result.getComposite()).isNotNull();
        assertThat(result.getComposite().getFileUrl()).isEqualTo("https://supabase.co/composite.jpg");
        assertThat(result.getCompositeUrl()).isEqualTo("https://supabase.co/composite.jpg");
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException se modelo não for encontrado ou estiver inativo")
    void shouldThrowNotFoundWhenModelNotFoundOrInactive() {
        when(modelRepository.findByIdAndIsActiveTrue(modelId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> publicModelDetailService.getModelDetail(modelId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Modelo não encontrado ou inativo");

        verify(modelMediaRepository, never()).findByModelIdAndModelIsActiveTrueOrderByDisplayOrderAsc(any());
    }

    @Test
    @DisplayName("Deve lidar graciosamente com data de nascimento nula e composite não cadastrado")
    void shouldHandleNullBirthDateAndNullComposite() {
        model.setBirthDate(null);
        when(modelRepository.findByIdAndIsActiveTrue(modelId)).thenReturn(Optional.of(model));
        when(modelMediaRepository.findByModelIdAndModelIsActiveTrueOrderByDisplayOrderAsc(modelId))
                .thenReturn(List.of());

        ModelDetailPublicDto result = publicModelDetailService.getModelDetail(modelId);

        assertThat(result).isNotNull();
        assertThat(result.getAge()).isNull();
        assertThat(result.getBookPhotos()).isEmpty();
        assertThat(result.getPolaroids()).isEmpty();
        assertThat(result.getComposite()).isNull();
        assertThat(result.getCompositeUrl()).isNull();
    }
}
