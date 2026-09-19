package com.wbscouting.api.service.media;

import com.wbscouting.api.config.SupabaseProperties;
import com.wbscouting.api.dto.media.MediaOrderItemDto;
import com.wbscouting.api.dto.media.MediaReorderRequestDto;
import com.wbscouting.api.dto.media.MediaUploadResponseDto;
import com.wbscouting.api.entity.Model;
import com.wbscouting.api.entity.ModelMedia;
import com.wbscouting.api.enums.GenderType;
import com.wbscouting.api.enums.MediaType;
import com.wbscouting.api.exception.ResourceNotFoundException;
import com.wbscouting.api.repository.ModelMediaRepository;
import com.wbscouting.api.repository.ModelRepository;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModelMediaServiceImplTest {

    @Mock
    private ModelRepository modelRepository;

    @Mock
    private ModelMediaRepository modelMediaRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private SupabaseProperties supabaseProperties;

    @InjectMocks
    private ModelMediaServiceImpl modelMediaService;

    private UUID modelId;
    private UUID mediaId;
    private Model model;
    private SupabaseProperties.Buckets buckets;

    @BeforeEach
    void setUp() {
        modelId = UUID.randomUUID();
        mediaId = UUID.randomUUID();

        model = Model.builder()
                .id(modelId)
                .stageName("Gisele Test")
                .gender(GenderType.FEMALE)
                .primaryPhotoUrl(null)
                .build();

        buckets = new SupabaseProperties.Buckets();
        lenient().when(supabaseProperties.getBuckets()).thenReturn(buckets);
    }

    @Test
    @DisplayName("Deve fazer upload de mídia BOOK como capa com sucesso e atualizar primaryPhotoUrl do modelo")
    void uploadMedia_BookAsCover_Success() {
        MockMultipartFile file = new MockMultipartFile("file", "editorial photo.jpg", "image/jpeg", "image-bytes".getBytes());

        ModelMedia existingCover = ModelMedia.builder()
                .id(UUID.randomUUID())
                .model(model)
                .mediaType(MediaType.BOOK)
                .fileUrl("https://supabase.co/old-cover.jpg")
                .storagePath("models-media/old-cover.jpg")
                .isCover(true)
                .build();

        when(modelRepository.findById(modelId)).thenReturn(Optional.of(model));
        when(modelMediaRepository.findByModelIdAndIsCoverTrue(modelId)).thenReturn(Optional.of(existingCover));
        when(modelMediaRepository.findNextDisplayOrder(modelId, MediaType.BOOK)).thenReturn(3);
        when(storageService.uploadFile(eq("models-media"), any(String.class), eq(file)))
                .thenReturn("models-media/new-cover.jpg");
        when(storageService.getPublicUrl("models-media", "models-media/new-cover.jpg"))
                .thenReturn("https://supabase.co/new-cover.jpg");

        when(modelMediaRepository.save(any(ModelMedia.class))).thenAnswer(invocation -> {
            ModelMedia mm = invocation.getArgument(0);
            if (mm.getId() == null) {
                mm.setId(mediaId);
                mm.setCreatedAt(OffsetDateTime.now());
            }
            return mm;
        });

        MediaUploadResponseDto response = modelMediaService.uploadMedia(modelId, MediaType.BOOK, true, file);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(mediaId);
        assertThat(response.getMediaType()).isEqualTo(MediaType.BOOK);
        assertThat(response.getIsCover()).isTrue();
        assertThat(response.getDisplayOrder()).isEqualTo(3);
        assertThat(response.getFileUrl()).isEqualTo("https://supabase.co/new-cover.jpg");

        // Verifica que capa antiga foi resetada para false
        assertThat(existingCover.getIsCover()).isFalse();
        verify(modelMediaRepository).save(existingCover);

        // Verifica sincronização do primaryPhotoUrl no Model
        assertThat(model.getPrimaryPhotoUrl()).isEqualTo("https://supabase.co/new-cover.jpg");
        verify(modelRepository).save(model);
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando tentar definir POLAROID como capa")
    void uploadMedia_PolaroidAsCover_ThrowsIllegalArgumentException() {
        MockMultipartFile file = new MockMultipartFile("file", "natural.jpg", "image/jpeg", "image-bytes".getBytes());
        when(modelRepository.findById(modelId)).thenReturn(Optional.of(model));

        assertThatThrownBy(() -> modelMediaService.uploadMedia(modelId, MediaType.POLAROID, true, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Apenas mídias do tipo BOOK podem ser marcadas como capa.");

        verify(storageService, never()).uploadFile(any(), any(), any());
    }

    @Test
    @DisplayName("Deve substituir composite anterior ao fazer upload de novo COMPOSITE")
    void uploadMedia_Composite_ReplacesOldComposite() {
        MockMultipartFile file = new MockMultipartFile("file", "composite.jpg", "image/jpeg", "image-bytes".getBytes());

        ModelMedia oldComposite = ModelMedia.builder()
                .id(UUID.randomUUID())
                .model(model)
                .mediaType(MediaType.COMPOSITE)
                .storagePath("models-media/old-comp.jpg")
                .isActive(true)
                .build();

        when(modelRepository.findById(modelId)).thenReturn(Optional.of(model));
        when(modelMediaRepository.findByModelIdAndMediaTypeAndIsActiveTrue(modelId, MediaType.COMPOSITE))
                .thenReturn(Optional.of(oldComposite));
        when(modelMediaRepository.findNextDisplayOrder(modelId, MediaType.COMPOSITE)).thenReturn(1);
        when(storageService.uploadFile(eq("models-media"), any(), eq(file))).thenReturn("models-media/new-comp.jpg");
        when(storageService.getPublicUrl(any(), any())).thenReturn("https://supabase.co/new-comp.jpg");
        when(modelMediaRepository.save(any(ModelMedia.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MediaUploadResponseDto response = modelMediaService.uploadMedia(modelId, MediaType.COMPOSITE, false, file);

        assertThat(response).isNotNull();
        verify(storageService).deleteFile("models-media", "models-media/old-comp.jpg");
        verify(modelMediaRepository).delete(oldComposite);
    }

    @Test
    @DisplayName("Deve definir mídia BOOK existente como capa e desmarcar a anterior")
    void setCoverMedia_Book_Success() {
        ModelMedia oldCover = ModelMedia.builder()
                .id(UUID.randomUUID())
                .model(model)
                .mediaType(MediaType.BOOK)
                .isCover(true)
                .build();

        ModelMedia newCover = ModelMedia.builder()
                .id(mediaId)
                .model(model)
                .mediaType(MediaType.BOOK)
                .fileUrl("https://supabase.co/target-cover.jpg")
                .isCover(false)
                .build();

        when(modelRepository.findById(modelId)).thenReturn(Optional.of(model));
        when(modelMediaRepository.findByIdAndModelId(mediaId, modelId)).thenReturn(Optional.of(newCover));
        when(modelMediaRepository.findByModelIdAndIsCoverTrue(modelId)).thenReturn(Optional.of(oldCover));

        modelMediaService.setCoverMedia(modelId, mediaId);

        assertThat(oldCover.getIsCover()).isFalse();
        assertThat(newCover.getIsCover()).isTrue();
        assertThat(model.getPrimaryPhotoUrl()).isEqualTo("https://supabase.co/target-cover.jpg");

        verify(modelMediaRepository).save(oldCover);
        verify(modelMediaRepository).save(newCover);
        verify(modelRepository).save(model);
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException ao tentar definir POLAROID como capa via setCoverMedia")
    void setCoverMedia_Polaroid_ThrowsException() {
        ModelMedia polaroid = ModelMedia.builder()
                .id(mediaId)
                .model(model)
                .mediaType(MediaType.POLAROID)
                .isCover(false)
                .build();

        when(modelRepository.findById(modelId)).thenReturn(Optional.of(model));
        when(modelMediaRepository.findByIdAndModelId(mediaId, modelId)).thenReturn(Optional.of(polaroid));

        assertThatThrownBy(() -> modelMediaService.setCoverMedia(modelId, mediaId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Apenas mídias do tipo BOOK podem ser marcadas como capa.");
    }

    @Test
    @DisplayName("Deve executar double-delete seguro excluindo no storage e no banco quando não é capa")
    void deleteMedia_NonCover_Success() {
        ModelMedia media = ModelMedia.builder()
                .id(mediaId)
                .model(model)
                .mediaType(MediaType.POLAROID)
                .storagePath("models-media/polaroid.jpg")
                .isCover(false)
                .build();

        when(modelMediaRepository.findByIdAndModelId(mediaId, modelId)).thenReturn(Optional.of(media));

        modelMediaService.deleteMedia(modelId, mediaId);

        // 1. Double delete: Primeiro storage
        verify(storageService).deleteFile("models-media", "models-media/polaroid.jpg");
        // 2. Depois banco
        verify(modelMediaRepository).delete(media);
        // Não altera cover
        verify(modelMediaRepository, never()).findFirstByModelIdAndMediaTypeOrderByCreatedAtAsc(any(), any());
    }

    @Test
    @DisplayName("Deve executar double-delete e promover o BOOK mais antigo remanescente a nova capa")
    void deleteMedia_Cover_PromotesOldestBook() {
        ModelMedia media = ModelMedia.builder()
                .id(mediaId)
                .model(model)
                .mediaType(MediaType.BOOK)
                .storagePath("models-media/current-cover.jpg")
                .isCover(true)
                .build();

        ModelMedia oldestBook = ModelMedia.builder()
                .id(UUID.randomUUID())
                .model(model)
                .mediaType(MediaType.BOOK)
                .fileUrl("https://supabase.co/oldest-book.jpg")
                .isCover(false)
                .createdAt(OffsetDateTime.now().minusDays(5))
                .build();

        when(modelMediaRepository.findByIdAndModelId(mediaId, modelId)).thenReturn(Optional.of(media));
        when(modelMediaRepository.findFirstByModelIdAndMediaTypeOrderByCreatedAtAsc(modelId, MediaType.BOOK))
                .thenReturn(Optional.of(oldestBook));

        modelMediaService.deleteMedia(modelId, mediaId);

        verify(storageService).deleteFile("models-media", "models-media/current-cover.jpg");
        verify(modelMediaRepository).delete(media);

        assertThat(oldestBook.getIsCover()).isTrue();
        assertThat(model.getPrimaryPhotoUrl()).isEqualTo("https://supabase.co/oldest-book.jpg");

        verify(modelMediaRepository).save(oldestBook);
        verify(modelRepository).save(model);
    }

    @Test
    @DisplayName("Deve executar double-delete de capa e setar primaryPhotoUrl como null se não houver BOOK remanescente")
    void deleteMedia_Cover_NoRemainingBook_SetsNullPrimaryPhotoUrl() {
        ModelMedia media = ModelMedia.builder()
                .id(mediaId)
                .model(model)
                .mediaType(MediaType.BOOK)
                .storagePath("models-media/cover.jpg")
                .isCover(true)
                .build();

        when(modelMediaRepository.findByIdAndModelId(mediaId, modelId)).thenReturn(Optional.of(media));
        when(modelMediaRepository.findFirstByModelIdAndMediaTypeOrderByCreatedAtAsc(modelId, MediaType.BOOK))
                .thenReturn(Optional.empty());

        modelMediaService.deleteMedia(modelId, mediaId);

        verify(storageService).deleteFile("models-media", "models-media/cover.jpg");
        verify(modelMediaRepository).delete(media);

        assertThat(model.getPrimaryPhotoUrl()).isNull();
        verify(modelRepository).save(model);
    }

    @Test
    @DisplayName("Deve reordenar mídias em lote com sucesso")
    void reorderMedia_Success() {
        UUID mediaId2 = UUID.randomUUID();
        ModelMedia media1 = ModelMedia.builder().id(mediaId).model(model).displayOrder(1).build();
        ModelMedia media2 = ModelMedia.builder().id(mediaId2).model(model).displayOrder(2).build();

        MediaReorderRequestDto request = MediaReorderRequestDto.builder()
                .items(List.of(
                        MediaOrderItemDto.builder().mediaId(mediaId).displayOrder(5).build(),
                        MediaOrderItemDto.builder().mediaId(mediaId2).displayOrder(6).build()
                ))
                .build();

        when(modelRepository.existsById(modelId)).thenReturn(true);
        when(modelMediaRepository.findByIdAndModelId(mediaId, modelId)).thenReturn(Optional.of(media1));
        when(modelMediaRepository.findByIdAndModelId(mediaId2, modelId)).thenReturn(Optional.of(media2));

        modelMediaService.reorderMedia(modelId, request);

        assertThat(media1.getDisplayOrder()).isEqualTo(5);
        assertThat(media2.getDisplayOrder()).isEqualTo(6);

        verify(modelMediaRepository).save(media1);
        verify(modelMediaRepository).save(media2);
    }

    @Test
    @DisplayName("Deve listar mídias do modelo ordenadas por mediaType e displayOrder")
    void listModelMedia_Success() {
        ModelMedia m1 = ModelMedia.builder()
                .id(mediaId)
                .model(model)
                .mediaType(MediaType.BOOK)
                .fileUrl("https://supabase.co/1.jpg")
                .storagePath("models-media/1.jpg")
                .displayOrder(1)
                .isCover(true)
                .createdAt(OffsetDateTime.now())
                .build();

        when(modelRepository.existsById(modelId)).thenReturn(true);
        when(modelMediaRepository.findByModelIdOrderByMediaTypeAscDisplayOrderAsc(modelId))
                .thenReturn(List.of(m1));

        List<MediaUploadResponseDto> result = modelMediaService.listModelMedia(modelId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(mediaId);
        assertThat(result.get(0).getMediaType()).isEqualTo(MediaType.BOOK);
    }
}
