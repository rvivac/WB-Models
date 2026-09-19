package com.wbscouting.api.service.model;

import com.wbscouting.api.dto.model.*;
import com.wbscouting.api.entity.Model;
import com.wbscouting.api.enums.GenderType;
import com.wbscouting.api.exception.ResourceNotFoundException;
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
import org.springframework.data.domain.PageRequest;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModelServiceImplTest {

    @Mock
    private ModelRepository modelRepository;

    @InjectMocks
    private ModelServiceImpl modelService;

    private Model model;
    private UUID modelId;

    @BeforeEach
    void setUp() {
        modelId = UUID.randomUUID();
        model = Model.builder()
                .id(modelId)
                .stageName("Gisele B.")
                .gender(GenderType.FEMALE)
                .isStar(true)
                .isFeaturedHome(true)
                .featuredOrder(1)
                .isActive(true)
                .heightCm(180)
                .city("São Paulo")
                .nationality("Brasileira")
                .bustChestCm(new BigDecimal("86.00"))
                .waistCm(new BigDecimal("60.00"))
                .hipsCm(new BigDecimal("89.00"))
                .build();
    }

    @Test
    @DisplayName("Deve cadastrar novo modelo com sucesso")
    void shouldCreateModelSuccessfully() {
        ModelCreateRequestDto request = ModelCreateRequestDto.builder()
                .stageName("Gisele B.")
                .gender(GenderType.FEMALE)
                .isStar(true)
                .isFeaturedHome(true)
                .featuredOrder(1)
                .isActive(true)
                .heightCm(180)
                .birthDate(LocalDate.of(2000, 1, 1))
                .build();

        when(modelRepository.save(any(Model.class))).thenReturn(model);

        ModelAdminResponseDto response = modelService.createModel(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(modelId);
        assertThat(response.getStageName()).isEqualTo("Gisele B.");
        assertThat(response.getGender()).isEqualTo(GenderType.FEMALE);
        assertThat(response.getIsStar()).isTrue();
        verify(modelRepository, times(1)).save(any(Model.class));
    }

    @Test
    @DisplayName("Deve atualizar modelo existente com sucesso")
    void shouldUpdateModelSuccessfully() {
        ModelUpdateRequestDto request = ModelUpdateRequestDto.builder()
                .stageName("Gisele Bundchen")
                .gender(GenderType.FEMALE)
                .isStar(true)
                .isFeaturedHome(true)
                .featuredOrder(2)
                .isActive(true)
                .heightCm(181)
                .build();

        when(modelRepository.findById(modelId)).thenReturn(Optional.of(model));
        when(modelRepository.save(any(Model.class))).thenReturn(model);

        ModelAdminResponseDto response = modelService.updateModel(modelId, request);

        assertThat(response).isNotNull();
        verify(modelRepository, times(1)).findById(modelId);
        verify(modelRepository, times(1)).save(model);
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao tentar atualizar modelo inexistente")
    void shouldThrowExceptionWhenUpdatingNonExistentModel() {
        UUID nonExistentId = UUID.randomUUID();
        ModelUpdateRequestDto request = ModelUpdateRequestDto.builder()
                .stageName("Inexistente")
                .gender(GenderType.FEMALE)
                .build();

        when(modelRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> modelService.updateModel(nonExistentId, request))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(modelRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve alterar pontualmente o status de ativação")
    void shouldUpdateStatusSuccessfully() {
        ModelStatusPatchDto patch = new ModelStatusPatchDto(false);
        when(modelRepository.findById(modelId)).thenReturn(Optional.of(model));
        when(modelRepository.save(any(Model.class))).thenReturn(model);

        ModelAdminResponseDto response = modelService.updateStatus(modelId, patch);

        assertThat(response).isNotNull();
        assertThat(model.getIsActive()).isFalse();
        verify(modelRepository, times(1)).save(model);
    }

    @Test
    @DisplayName("Deve alterar pontualmente o status de Star")
    void shouldUpdateStarSuccessfully() {
        ModelStarPatchDto patch = new ModelStarPatchDto(false);
        when(modelRepository.findById(modelId)).thenReturn(Optional.of(model));
        when(modelRepository.save(any(Model.class))).thenReturn(model);

        ModelAdminResponseDto response = modelService.updateStar(modelId, patch);

        assertThat(response).isNotNull();
        assertThat(model.getIsStar()).isFalse();
        verify(modelRepository, times(1)).save(model);
    }

    @Test
    @DisplayName("Deve alterar pontualmente o status de destaque na home")
    void shouldUpdateFeaturedSuccessfully() {
        ModelFeaturedPatchDto patch = new ModelFeaturedPatchDto(true, 5);
        when(modelRepository.findById(modelId)).thenReturn(Optional.of(model));
        when(modelRepository.save(any(Model.class))).thenReturn(model);

        ModelAdminResponseDto response = modelService.updateFeatured(modelId, patch);

        assertThat(response).isNotNull();
        assertThat(model.getIsFeaturedHome()).isTrue();
        assertThat(model.getFeaturedOrder()).isEqualTo(5);
        verify(modelRepository, times(1)).save(model);
    }

    @Test
    @DisplayName("Deve listar modelos no painel administrativo com paginação")
    void shouldListAdminModelsSuccessfully() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Model> page = new PageImpl<>(List.of(model), pageable, 1);

        when(modelRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<ModelAdminResponseDto> result = modelService.listAdminModels(GenderType.FEMALE, true, true, "Gisele", pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getStageName()).isEqualTo("Gisele B.");
    }

    @Test
    @DisplayName("Deve obter detalhes administrativos de um modelo por ID")
    void shouldGetAdminModelByIdSuccessfully() {
        when(modelRepository.findById(modelId)).thenReturn(Optional.of(model));

        ModelAdminResponseDto response = modelService.getAdminModelById(modelId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(modelId);
        assertThat(response.getStageName()).isEqualTo("Gisele B.");
    }

    @Test
    @DisplayName("Deve excluir modelo com sucesso")
    void shouldDeleteModelSuccessfully() {
        when(modelRepository.findById(modelId)).thenReturn(Optional.of(model));

        modelService.deleteModel(modelId);

        verify(modelRepository, times(1)).delete(model);
    }
}
