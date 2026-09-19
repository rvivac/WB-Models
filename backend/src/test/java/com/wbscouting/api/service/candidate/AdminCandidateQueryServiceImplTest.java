package com.wbscouting.api.service.candidate;

import com.wbscouting.api.config.SupabaseProperties;
import com.wbscouting.api.dto.admin.candidate.CandidateDetailAdminDto;
import com.wbscouting.api.dto.admin.candidate.CandidateListItemAdminDto;
import com.wbscouting.api.dto.common.PageResponseDto;
import com.wbscouting.api.entity.Candidate;
import com.wbscouting.api.entity.CandidatePhoto;
import com.wbscouting.api.enums.CandidateStatus;
import com.wbscouting.api.exception.ResourceNotFoundException;
import com.wbscouting.api.repository.CandidatePhotoRepository;
import com.wbscouting.api.repository.CandidateRepository;
import com.wbscouting.api.service.storage.StorageService;
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
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminCandidateQueryServiceImplTest {

    @Mock
    private CandidateRepository candidateRepository;

    @Mock
    private CandidatePhotoRepository candidatePhotoRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private SupabaseProperties supabaseProperties;

    @InjectMocks
    private AdminCandidateQueryServiceImpl adminCandidateQueryService;

    private UUID candidateId;
    private Candidate candidate;
    private CandidatePhoto photo1;
    private CandidatePhoto photo2;

    @BeforeEach
    void setUp() {
        candidateId = UUID.randomUUID();

        photo1 = CandidatePhoto.builder()
                .id(UUID.randomUUID())
                .storagePath("cand-test/photo1.jpg")
                .displayOrder(1)
                .build();

        photo2 = CandidatePhoto.builder()
                .id(UUID.randomUUID())
                .storagePath("cand-test/photo2.jpg")
                .displayOrder(2)
                .build();

        candidate = Candidate.builder()
                .id(candidateId)
                .fullName("Gisele Bündchen")
                .email("gisele@exemplo.com")
                .phone("51999998888")
                .birthDate(LocalDate.now().minusYears(17))
                .age(17)
                .gender("FEMALE")
                .heightCm(new BigDecimal("180.00"))
                .city("Horizontina")
                .state("RS")
                .status(CandidateStatus.PENDING)
                .internalNotes("Grande potencial editorial")
                .photos(new ArrayList<>(List.of(photo2, photo1)))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        photo1.setCandidate(candidate);
        photo2.setCandidate(candidate);
    }

    @Test
    @DisplayName("Deve listar candidaturas com PageResponseDto e contagem agregada de fotos sem gerar Signed URLs")
    void listCandidates_ReturnsPageResponseDto() {
        Page<Candidate> candidatePage = new PageImpl<>(List.of(candidate));
        when(candidateRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(candidatePage);
        when(candidatePhotoRepository.countPhotosByCandidateIds(List.of(candidateId)))
                .thenReturn(List.<Object[]>of(new Object[]{candidateId, 2L}));

        PageResponseDto<CandidateListItemAdminDto> result = adminCandidateQueryService.listCandidates(
                CandidateStatus.PENDING, "FEMALE", "Gisele", true, 0, 20
        );

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);

        CandidateListItemAdminDto dto = result.getContent().get(0);
        assertThat(dto.getId()).isEqualTo(candidateId);
        assertThat(dto.getFullName()).isEqualTo("Gisele Bündchen");
        assertThat(dto.getAge()).isEqualTo(17);
        assertThat(dto.getIsMinor()).isTrue();
        assertThat(dto.getPhotoCount()).isEqualTo(2);

        // Garante que nenhuma signed URL foi solicitada na listagem
        verify(storageService, never()).createSignedUrl(anyString(), anyString(), anyInt());
    }

    @Test
    @DisplayName("Deve retornar detalhes completos com Signed URLs de 900s e fotos ordenadas")
    void getCandidateDetail_Success() {
        when(candidateRepository.findWithPhotosById(candidateId)).thenReturn(Optional.of(candidate));
        when(storageService.createSignedUrl(eq("candidates-uploads"), eq("cand-test/photo1.jpg"), eq(900)))
                .thenReturn("https://supabase.co/signed/cand-test/photo1.jpg?token=123");
        when(storageService.createSignedUrl(eq("candidates-uploads"), eq("cand-test/photo2.jpg"), eq(900)))
                .thenReturn("https://supabase.co/signed/cand-test/photo2.jpg?token=456");

        CandidateDetailAdminDto detail = adminCandidateQueryService.getCandidateDetail(candidateId);

        assertThat(detail).isNotNull();
        assertThat(detail.getId()).isEqualTo(candidateId);
        assertThat(detail.getFullName()).isEqualTo("Gisele Bündchen");
        assertThat(detail.getPhotos()).hasSize(2);

        // Validação da ordem displayOrder ASC
        assertThat(detail.getPhotos().get(0).getDisplayOrder()).isEqualTo(1);
        assertThat(detail.getPhotos().get(0).getSignedUrl()).isEqualTo("https://supabase.co/signed/cand-test/photo1.jpg?token=123");
        assertThat(detail.getPhotos().get(0).getExpiresInSeconds()).isEqualTo(900);

        assertThat(detail.getPhotos().get(1).getDisplayOrder()).isEqualTo(2);
        assertThat(detail.getPhotos().get(1).getSignedUrl()).isEqualTo("https://supabase.co/signed/cand-test/photo2.jpg?token=456");
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao consultar detalhes de ID inexistente")
    void getCandidateDetail_NotFound_ThrowsException() {
        when(candidateRepository.findWithPhotosById(candidateId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminCandidateQueryService.getCandidateDetail(candidateId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Candidatura não encontrada com ID: " + candidateId);
    }

    @Test
    @DisplayName("Deve executar double-delete seguro: remoção física no storage e posterior exclusão relacional")
    void deleteCandidate_Success() {
        when(candidateRepository.findWithPhotosById(candidateId)).thenReturn(Optional.of(candidate));

        adminCandidateQueryService.deleteCandidate(candidateId);

        // 1. Deleção física de cada arquivo no bucket privado
        verify(storageService).deleteFile("candidates-uploads", "cand-test/photo1.jpg");
        verify(storageService).deleteFile("candidates-uploads", "cand-test/photo2.jpg");

        // 2. Deleção relacional no banco
        verify(candidateRepository).delete(candidate);
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao tentar deletar ID inexistente")
    void deleteCandidate_NotFound_ThrowsException() {
        when(candidateRepository.findWithPhotosById(candidateId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminCandidateQueryService.deleteCandidate(candidateId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Candidatura não encontrada com ID: " + candidateId);

        verify(storageService, never()).deleteFile(anyString(), anyString());
        verify(candidateRepository, never()).delete(any(Candidate.class));
    }
}
