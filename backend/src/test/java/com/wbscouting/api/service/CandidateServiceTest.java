package com.wbscouting.api.service;

import com.wbscouting.api.config.MailProperties;
import com.wbscouting.api.dto.CandidateApplicationDto;
import com.wbscouting.api.dto.CandidateDTO;
import com.wbscouting.api.entity.Candidate;
import com.wbscouting.api.repository.CandidateRepository;
import com.wbscouting.api.service.email.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CandidateServiceTest {

    @Mock
    private CandidateRepository candidateRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private MailProperties mailProperties;

    @InjectMocks
    private CandidateService candidateService;

    @BeforeEach
    void setUp() {
        when(mailProperties.getAgencyNotificationEmail()).thenReturn("contato@wbscouting.com");
    }

    @Test
    @DisplayName("Deve salvar candidatura e disparar notificações assíncronas para agência e candidato")
    void shouldSubmitApplicationAndDispatchEmails() {
        Candidate savedCandidate = Candidate.builder()
                .id(UUID.randomUUID())
                .fullName("Mariana Rios")
                .email("mariana@exemplo.com")
                .phone("(11) 97777-6666")
                .age(19)
                .gender("Feminino")
                .heightCm(new BigDecimal("179.00"))
                .photos(new ArrayList<>())
                .createdAt(OffsetDateTime.now())
                .build();

        when(candidateRepository.save(any(Candidate.class))).thenReturn(savedCandidate);

        CandidateDTO.ApplicationRequest request = CandidateDTO.ApplicationRequest.builder()
                .fullName("Mariana Rios")
                .email("mariana@exemplo.com")
                .phone("(11) 97777-6666")
                .age(19)
                .gender("Feminino")
                .heightCm(new BigDecimal("179.00"))
                .lgpdAccepted(true)
                .photos(List.of())
                .build();

        CandidateDTO.Response response = candidateService.submitApplication(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(savedCandidate.getId());

        // Verifica que disparou para a agência
        verify(emailService, times(1)).sendCandidateApplicationNotification(
                eq("contato@wbscouting.com"),
                any(CandidateApplicationDto.class)
        );

        // Verifica que disparou cópia para o candidato
        verify(emailService, times(1)).sendCandidateApplicationNotification(
                eq("mariana@exemplo.com"),
                any(CandidateApplicationDto.class)
        );
    }
}
