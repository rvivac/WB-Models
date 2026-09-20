package com.wbscouting.api.service.submission;

import com.wbscouting.api.dto.CandidateSubmissionRequestDto;
import com.wbscouting.api.dto.CandidateSubmissionResponseDto;
import com.wbscouting.api.entity.CandidateSubmission;
import com.wbscouting.api.enums.SubmissionGender;
import com.wbscouting.api.enums.SubmissionStatus;
import com.wbscouting.api.exception.BusinessException;
import com.wbscouting.api.repository.CandidateSubmissionRepository;
import com.wbscouting.api.service.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CandidateSubmissionServiceTest {

    @Mock
    private CandidateSubmissionRepository repository;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private CandidateSubmissionServiceImpl submissionService;

    private CandidateSubmissionRequestDto adultDto;
    private CandidateSubmissionRequestDto minorDto;
    private MockMultipartFile validJpegFace;
    private MockMultipartFile validPngProfile;
    private MockMultipartFile validJpegBody;

    @BeforeEach
    void setUp() {
        adultDto = CandidateSubmissionRequestDto.builder()
                .fullName("Alessandra Ambrosio")
                .email("alessandra@ambrosio.com")
                .phone("+5511999997777")
                .birthDate(LocalDate.now().minusYears(22))
                .gender(SubmissionGender.FEMALE)
                .city("Erechim")
                .state("RS")
                .height(new BigDecimal("1.78"))
                .bust(new BigDecimal("86"))
                .waist(new BigDecimal("61"))
                .hips(new BigDecimal("89"))
                .instagramHandle("@alessandra")
                .lgpdConsent(true)
                .build();

        minorDto = CandidateSubmissionRequestDto.builder()
                .fullName("Nova Promessa")
                .email("nova@promessa.com")
                .phone("+5511999991111")
                .birthDate(LocalDate.now().minusYears(15))
                .gender(SubmissionGender.FEMALE)
                .city("São Paulo")
                .state("SP")
                .height(new BigDecimal("1.74"))
                .lgpdConsent(true)
                .build();

        byte[] jpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16, 'J', 'F', 'I', 'F'};
        byte[] pngBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

        validJpegFace = new MockMultipartFile("facePhoto", "face.jpg", "image/jpeg", jpegBytes);
        validPngProfile = new MockMultipartFile("profilePhoto", "profile.png", "image/png", pngBytes);
        validJpegBody = new MockMultipartFile("fullBodyPhoto", "body.jpg", "image/jpeg", jpegBytes);
    }

    @Test
    @DisplayName("submit - Sucesso com candidato maior de idade")
    void shouldSubmitSuccessfullyWhenAdult() {
        when(storageService.getPublicUrl(any(), any())).thenReturn("https://storage.supabase.co/file.jpg");
        when(repository.save(any(CandidateSubmission.class))).thenAnswer(invocation -> {
            CandidateSubmission sub = invocation.getArgument(0);
            sub.setId(UUID.randomUUID());
            return sub;
        });

        CandidateSubmissionResponseDto response = submissionService.submit(
                adultDto, validJpegFace, validPngProfile, validJpegBody
        );

        assertThat(response).isNotNull();
        assertThat(response.getId()).isNotNull();
        assertThat(response.getProtocol()).startsWith("WB-");
        assertThat(response.getStatus()).isEqualTo(SubmissionStatus.PENDING);

        verify(storageService, times(3)).uploadFile(any(), any(), any());
        verify(repository, times(1)).save(any(CandidateSubmission.class));
    }

    @Test
    @DisplayName("submit - Rejeita menor de 18 anos sem dados do responsável")
    void shouldRejectMinorWithoutGuardian() {
        assertThatThrownBy(() -> submissionService.submit(
                minorDto, validJpegFace, validPngProfile, validJpegBody
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Para candidatos menores de 18 anos, o nome do responsável legal é obrigatório.");

        verifyNoInteractions(storageService);
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("submit - Sucesso com menor de 18 anos com dados válidos do responsável")
    void shouldSubmitSuccessfullyWhenMinorWithValidGuardian() {
        minorDto.setGuardianName("Maria Promessa");
        minorDto.setGuardianPhone("+5511999990000");
        minorDto.setGuardianEmail("maria@promessa.com");

        when(storageService.getPublicUrl(any(), any())).thenReturn("https://storage.supabase.co/file.jpg");
        when(repository.save(any(CandidateSubmission.class))).thenAnswer(invocation -> {
            CandidateSubmission sub = invocation.getArgument(0);
            sub.setId(UUID.randomUUID());
            return sub;
        });

        CandidateSubmissionResponseDto response = submissionService.submit(
                minorDto, validJpegFace, validPngProfile, validJpegBody
        );

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(SubmissionStatus.PENDING);
        verify(repository, times(1)).save(any(CandidateSubmission.class));
    }

    @Test
    @DisplayName("submit - Rejeita arquivo que excede 5MB")
    void shouldRejectFileExceeding5MB() {
        byte[] largeBytes = new byte[5 * 1024 * 1024 + 10];
        largeBytes[0] = (byte) 0xFF;
        largeBytes[1] = (byte) 0xD8;
        largeBytes[2] = (byte) 0xFF;
        MockMultipartFile largeFile = new MockMultipartFile("facePhoto", "face.jpg", "image/jpeg", largeBytes);

        assertThatThrownBy(() -> submissionService.submit(
                adultDto, largeFile, validPngProfile, validJpegBody
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("excede o limite máximo permitido de 5 MB");
    }

    @Test
    @DisplayName("submit - Rejeita arquivo com cabeçalho adulterado")
    void shouldRejectCorruptMagicBytes() {
        byte[] fakeBytes = new byte[]{1, 2, 3, 4, 5, 6, 7, 8};
        MockMultipartFile corruptFile = new MockMultipartFile("facePhoto", "face.jpg", "image/jpeg", fakeBytes);

        assertThatThrownBy(() -> submissionService.submit(
                adultDto, corruptFile, validPngProfile, validJpegBody
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cabeçalho de arquivo adulterado");
    }
}
