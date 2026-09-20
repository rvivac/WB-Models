package com.wbscouting.api.service.submission;

import com.wbscouting.api.dto.CandidateSubmissionResponseDto;
import com.wbscouting.api.dto.submission.CandidateStatusUpdateDto;
import com.wbscouting.api.entity.CandidateSubmission;
import com.wbscouting.api.enums.SubmissionGender;
import com.wbscouting.api.enums.SubmissionStatus;
import com.wbscouting.api.event.CandidateApprovedEvent;
import com.wbscouting.api.exception.ResourceNotFoundException;
import com.wbscouting.api.repository.CandidateSubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CandidateSubmissionAdminServiceTest {

    @Mock
    private CandidateSubmissionRepository repository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CandidateSubmissionAdminServiceImpl adminService;

    private UUID sampleId;
    private CandidateSubmission sampleSubmission;

    @BeforeEach
    void setUp() {
        sampleId = UUID.randomUUID();
        sampleSubmission = CandidateSubmission.builder()
                .id(sampleId)
                .protocol("WB-20260920-ABC123")
                .fullName("Carol Trentini")
                .email("carol@trentini.com")
                .phone("+5554999990000")
                .birthDate(LocalDate.of(2002, 7, 6))
                .age(24)
                .gender(SubmissionGender.FEMALE)
                .city("Panambi")
                .state("RS")
                .height(new BigDecimal("1.80"))
                .status(SubmissionStatus.PENDING)
                .facePhotoUrl("https://storage/face.jpg")
                .profilePhotoUrl("https://storage/profile.jpg")
                .fullBodyPhotoUrl("https://storage/body.jpg")
                .createdAt(OffsetDateTime.now())
                .build();
    }

    @Test
    @DisplayName("listSubmissions - Deve retornar página mapeada para DTO")
    void shouldListSubmissions() {
        Page<CandidateSubmission> page = new PageImpl<>(List.of(sampleSubmission));
        when(repository.findAll(nullable(Specification.class), any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        Page<CandidateSubmissionResponseDto> result = adminService.listSubmissions(null, PageRequest.of(0, 10));

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getFullName()).isEqualTo("Carol Trentini");
    }

    @Test
    @DisplayName("getSubmissionById - Sucesso quando a candidatura existir")
    void shouldGetSubmissionById() {
        when(repository.findById(sampleId)).thenReturn(Optional.of(sampleSubmission));

        CandidateSubmissionResponseDto result = adminService.getSubmissionById(sampleId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(sampleId);
        assertThat(result.getProtocol()).isEqualTo("WB-20260920-ABC123");
    }

    @Test
    @DisplayName("getSubmissionById - Lança ResourceNotFoundException quando não existir")
    void shouldThrowWhenNotFound() {
        when(repository.findById(sampleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.getSubmissionById(sampleId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Candidatura não encontrada");
    }

    @Test
    @DisplayName("updateSubmissionStatus - Transição para APPROVED grava auditoria e publica evento")
    void shouldApproveAndPublishEvent() {
        when(repository.findById(sampleId)).thenReturn(Optional.of(sampleSubmission));
        when(repository.save(any(CandidateSubmission.class))).thenAnswer(inv -> inv.getArgument(0));

        CandidateStatusUpdateDto updateDto = CandidateStatusUpdateDto.builder()
                .status(SubmissionStatus.APPROVED)
                .feedbackNotes("Aprovada para agendamento de casting presencial.")
                .build();

        CandidateSubmissionResponseDto result = adminService.updateSubmissionStatus(sampleId, updateDto, "Booker Lucas");

        assertThat(result.getStatus()).isEqualTo(SubmissionStatus.APPROVED);
        assertThat(result.getReviewedBy()).isEqualTo("Booker Lucas");
        assertThat(result.getReviewedAt()).isNotNull();
        assertThat(result.getFeedbackNotes()).isEqualTo("Aprovada para agendamento de casting presencial.");

        verify(eventPublisher, times(1)).publishEvent(any(CandidateApprovedEvent.class));
    }

    @Test
    @DisplayName("updateSubmissionStatus - Transição para REJECTED não publica evento de aprovação")
    void shouldRejectWithoutPublishingApprovedEvent() {
        when(repository.findById(sampleId)).thenReturn(Optional.of(sampleSubmission));
        when(repository.save(any(CandidateSubmission.class))).thenAnswer(inv -> inv.getArgument(0));

        CandidateStatusUpdateDto updateDto = CandidateStatusUpdateDto.builder()
                .status(SubmissionStatus.REJECTED)
                .feedbackNotes("Perfil não adequado para o casting atual.")
                .build();

        CandidateSubmissionResponseDto result = adminService.updateSubmissionStatus(sampleId, updateDto, "Booker Lucas");

        assertThat(result.getStatus()).isEqualTo(SubmissionStatus.REJECTED);
        verify(eventPublisher, never()).publishEvent(any(CandidateApprovedEvent.class));
    }
}
