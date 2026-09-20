package com.wbscouting.api.service.submission;

import com.wbscouting.api.config.SupabaseProperties;
import com.wbscouting.api.dto.CandidateSubmissionRequestDto;
import com.wbscouting.api.dto.CandidateSubmissionResponseDto;
import com.wbscouting.api.entity.CandidateSubmission;
import com.wbscouting.api.enums.SubmissionGender;
import com.wbscouting.api.enums.SubmissionStatus;
import com.wbscouting.api.repository.CandidateSubmissionRepository;
import com.wbscouting.api.service.storage.SupabaseStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.client.RestClient;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidateSubmissionFallbackTest {

    @Mock
    private CandidateSubmissionRepository repository;

    private SupabaseProperties properties;
    private SupabaseStorageService storageService;
    private CandidateSubmissionServiceImpl submissionService;

    private final Path testDir = Paths.get("target/test-submission-fallback");

    private MockMultipartFile facePhoto;
    private MockMultipartFile profilePhoto;
    private MockMultipartFile fullBodyPhoto;

    @BeforeEach
    void setUp() {
        properties = new SupabaseProperties();
        properties.setUrl("https://dummy.supabase.co");
        properties.setServiceRoleKey("dummy-key");
        properties.getStorage().setLocalFallback(true);
        properties.getStorage().setLocalDir(testDir.toString());
        properties.getStorage().setLocalBaseUrl("http://localhost:8080");

        RestClient restClient = RestClient.builder().baseUrl(properties.getUrl()).build();
        storageService = new SupabaseStorageService(restClient, properties);

        submissionService = new CandidateSubmissionServiceImpl(repository, storageService);

        byte[] jpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16, 'J', 'F', 'I', 'F'};
        facePhoto = new MockMultipartFile("facePhoto", "face.jpg", "image/jpeg", jpegBytes);
        profilePhoto = new MockMultipartFile("profilePhoto", "profile.jpg", "image/jpeg", jpegBytes);
        fullBodyPhoto = new MockMultipartFile("fullBodyPhoto", "body.jpg", "image/jpeg", jpegBytes);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (Files.exists(testDir)) {
            Files.walk(testDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    @Test
    @DisplayName("Deve submeter candidatura com sucesso salvando fotos localmente quando Supabase tiver chave dummy")
    void shouldSubmitCandidateWithLocalFallback() {
        CandidateSubmissionRequestDto request = CandidateSubmissionRequestDto.builder()
                .fullName("Camila Queiroz")
                .email("camila@test.com")
                .phone("+5511999991111")
                .birthDate(LocalDate.of(1993, 6, 27))
                .gender(SubmissionGender.FEMALE)
                .city("Ribeirão Preto")
                .state("SP")
                .height(new BigDecimal("1.78"))
                .bust(new BigDecimal("85"))
                .waist(new BigDecimal("60"))
                .hips(new BigDecimal("89"))
                .shoeSize(37)
                .eyeColor("Castanho")
                .hairColor("Castanho")
                .instagramHandle("camilaqueiroz")
                .lgpdConsent(true)
                .build();

        when(repository.save(any(CandidateSubmission.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CandidateSubmissionResponseDto response = submissionService.submit(
                request,
                facePhoto,
                profilePhoto,
                fullBodyPhoto
        );

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(SubmissionStatus.PENDING);
        assertThat(response.getProtocol()).startsWith("WB-");

        ArgumentCaptor<CandidateSubmission> captor = ArgumentCaptor.forClass(CandidateSubmission.class);
        verify(repository).save(captor.capture());
        CandidateSubmission saved = captor.getValue();

        assertThat(saved.getFacePhotoUrl()).contains("/api/v1/storage/local/candidates-uploads/submissions/");
        assertThat(saved.getProfilePhotoUrl()).contains("/api/v1/storage/local/candidates-uploads/submissions/");
        assertThat(saved.getFullBodyPhotoUrl()).contains("/api/v1/storage/local/candidates-uploads/submissions/");

        // Verifica que os arquivos foram fisicamente gravados no disco
        assertThat(Files.exists(testDir.resolve("candidates-uploads"))).isTrue();
    }
}
