package com.wbscouting.api.service.candidate;

import com.wbscouting.api.config.SupabaseProperties;
import com.wbscouting.api.dto.admin.candidate.CandidateDetailAdminDto;
import com.wbscouting.api.dto.admin.candidate.CandidateListItemAdminDto;
import com.wbscouting.api.dto.admin.candidate.CandidateNotesUpdateDto;
import com.wbscouting.api.dto.admin.candidate.CandidateStatusUpdateDto;
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
import org.springframework.data.domain.PageRequest;
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
class AdminCandidateServiceImplTest {

    @Mock
    private CandidateRepository candidateRepository;

    @Mock
    private CandidatePhotoRepository candidatePhotoRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private SupabaseProperties supabaseProperties;

    @InjectMocks
    private AdminCandidateServiceImpl adminCandidateService;

    private UUID candidateId;
    private Candidate candidate;
    private CandidatePhoto photo1;
    private CandidatePhoto photo2;

    @BeforeEach
    void setUp() {
        candidateId = UUID.randomUUID();

        photo1 = CandidatePhoto.builder()
                .id(UUID.randomUUID())
                .storagePath("cand-1/photo1.jpg")
                .displayOrder(1)
                .build();

        photo2 = CandidatePhoto.builder()
                .id(UUID.randomUUID())
                .storagePath("cand-1/photo2.jpg")
                .displayOrder(2)
                .build();

        candidate = Candidate.builder()
                .id(candidateId)
                .fullName("Mariana Silva")
                .email("mariana@exemplo.com")
                .phone("11988887777")
                .birthDate(LocalDate.now().minusYears(17)) // Menor de idade
                .age(17)
                .gender("FEMALE")
                .heightCm(new BigDecimal("178.00"))
                .city("São Paulo")
                .state("SP")
                .status(CandidateStatus.PENDING)
                .internalNotes("Perfil promissor")
                .photos(new ArrayList<>(List.of(photo2, photo1))) // Ordem invertida para testar ordenação
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        photo1.setCandidate(candidate);
        photo2.setCandidate(candidate);
    }

    @Test
    @DisplayName("Deve listar candidaturas com projeção compacta e contagem agregada de fotos")
    void listCandidates_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Candidate> candidatePage = new PageImpl<>(List.of(candidate), pageable, 1);

        when(candidateRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(candidatePage);
        when(candidatePhotoRepository.countPhotosByCandidateIds(List.of(candidateId)))
                .thenReturn(List.<Object[]>of(new Object[]{candidateId, 2L}));

        Page<CandidateListItemAdminDto> result = adminCandidateService.listCandidates(
                CandidateStatus.PENDING, "Mariana", "FEMALE", true, pageable
        );

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);

        CandidateListItemAdminDto dto = result.getContent().get(0);
        assertThat(dto.getId()).isEqualTo(candidateId);
        assertThat(dto.getFullName()).isEqualTo("Mariana Silva");
        assertThat(dto.getAge()).isEqualTo(17);
        assertThat(dto.getIsMinor()).isTrue();
        assertThat(dto.getPhotoCount()).isEqualTo(2);
        assertThat(dto.getStatus()).isEqualTo(CandidateStatus.PENDING);
    }

    @Test
    @DisplayName("Deve retornar página vazia quando nenhuma candidatura for encontrada")
    void listCandidates_Empty() {
        Pageable pageable = PageRequest.of(0, 10);
        when(candidateRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(Page.empty(pageable));

        Page<CandidateListItemAdminDto> result = adminCandidateService.listCandidates(
                null, null, null, null, pageable
        );

        assertThat(result).isEmpty();
        verify(candidatePhotoRepository, never()).countPhotosByCandidateIds(anyList());
    }

    @Test
    @DisplayName("Deve retornar detalhes da candidatura com fotos acompanhadas de Signed URLs de 900s e ordenadas")
    void getCandidateDetail_Success() {
        when(candidateRepository.findWithPhotosById(candidateId)).thenReturn(Optional.of(candidate));
        when(storageService.createSignedUrl(eq("candidates-uploads"), eq("cand-1/photo1.jpg"), eq(900)))
                .thenReturn("https://supabase.co/signed/cand-1/photo1.jpg?token=abc");
        when(storageService.createSignedUrl(eq("candidates-uploads"), eq("cand-1/photo2.jpg"), eq(900)))
                .thenReturn("https://supabase.co/signed/cand-1/photo2.jpg?token=def");

        CandidateDetailAdminDto detail = adminCandidateService.getCandidateDetail(candidateId);

        assertThat(detail).isNotNull();
        assertThat(detail.getId()).isEqualTo(candidateId);
        assertThat(detail.getFullName()).isEqualTo("Mariana Silva");
        assertThat(detail.getIsMinor()).isTrue();
        assertThat(detail.getPhotos()).hasSize(2);

        // Verifica ordenação por displayOrder ASC
        assertThat(detail.getPhotos().get(0).getDisplayOrder()).isEqualTo(1);
        assertThat(detail.getPhotos().get(0).getSignedUrl()).isEqualTo("https://supabase.co/signed/cand-1/photo1.jpg?token=abc");
        assertThat(detail.getPhotos().get(0).getExpiresInSeconds()).isEqualTo(900);

        assertThat(detail.getPhotos().get(1).getDisplayOrder()).isEqualTo(2);
        assertThat(detail.getPhotos().get(1).getSignedUrl()).isEqualTo("https://supabase.co/signed/cand-1/photo2.jpg?token=def");
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao buscar detalhes de ID inexistente")
    void getCandidateDetail_NotFound_ThrowsException() {
        when(candidateRepository.findWithPhotosById(candidateId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminCandidateService.getCandidateDetail(candidateId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Candidatura não encontrada com ID: " + candidateId);
    }

    @Test
    @DisplayName("Deve atualizar status da candidatura com sucesso")
    void updateStatus_Success() {
        when(candidateRepository.findWithPhotosById(candidateId)).thenReturn(Optional.of(candidate));
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CandidateStatusUpdateDto dto = CandidateStatusUpdateDto.builder()
                .status(CandidateStatus.APPROVED)
                .build();

        CandidateDetailAdminDto result = adminCandidateService.updateStatus(candidateId, dto);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(CandidateStatus.APPROVED);
        assertThat(candidate.getStatus()).isEqualTo(CandidateStatus.APPROVED);
        verify(candidateRepository).save(candidate);
    }

    @Test
    @DisplayName("Deve atualizar anotações internas da candidatura com sucesso")
    void updateNotes_Success() {
        when(candidateRepository.findWithPhotosById(candidateId)).thenReturn(Optional.of(candidate));
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CandidateNotesUpdateDto dto = CandidateNotesUpdateDto.builder()
                .notes("Aprovada para casting de passarela")
                .build();

        CandidateDetailAdminDto result = adminCandidateService.updateNotes(candidateId, dto);

        assertThat(result).isNotNull();
        assertThat(result.getInternalNotes()).isEqualTo("Aprovada para casting de passarela");
        assertThat(candidate.getInternalNotes()).isEqualTo("Aprovada para casting de passarela");
        verify(candidateRepository).save(candidate);
    }

    @Test
    @DisplayName("Deve excluir candidatura removendo primeiro os arquivos físicos no Storage e depois no banco")
    void deleteCandidate_Success() {
        when(candidateRepository.findWithPhotosById(candidateId)).thenReturn(Optional.of(candidate));

        adminCandidateService.deleteCandidate(candidateId);

        // Valida double-delete seguro: deleção no Storage primeiro para cada foto
        verify(storageService).deleteFile("candidates-uploads", "cand-1/photo1.jpg");
        verify(storageService).deleteFile("candidates-uploads", "cand-1/photo2.jpg");

        // Depois deleção relacional da entidade
        verify(candidateRepository).delete(candidate);
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao tentar deletar ID inexistente")
    void deleteCandidate_NotFound_ThrowsException() {
        when(candidateRepository.findWithPhotosById(candidateId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminCandidateService.deleteCandidate(candidateId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Candidatura não encontrada com ID: " + candidateId);

        verify(storageService, never()).deleteFile(anyString(), anyString());
        verify(candidateRepository, never()).delete(any(Candidate.class));
    }
}
