package com.wbscouting.api.service.submission;

import com.wbscouting.api.repository.CandidateSubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProtocolGeneratorService {

    private static final String CHARSET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int SUFFIX_LENGTH = 4;
    private static final int MAX_ATTEMPTS = 10;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final CandidateSubmissionRepository repository;

    public String generateUniqueProtocol() {
        String datePart = LocalDate.now().format(DATE_FORMATTER);
        int attempts = 0;
        while (attempts < MAX_ATTEMPTS) {
            String candidate = String.format("WB-%s-%s", datePart, generateRandomSuffix());
            if (!repository.existsByProtocol(candidate)) {
                if (attempts > 0) {
                    log.warn("[PROTOCOL] {} colisao(oes) evitada(s) — retornando: {}", attempts, candidate);
                }
                return candidate;
            }
            attempts++;
            log.warn("[PROTOCOL] Tentativa {} — protocolo '{}' ja existe. Regenerando...", attempts, candidate);
        }
        throw new IllegalStateException("Falha ao gerar protocolo unico apos " + MAX_ATTEMPTS + " tentativas (" + datePart + ").");
    }

    private String generateRandomSuffix() {
        StringBuilder sb = new StringBuilder(SUFFIX_LENGTH);
        final int size = CHARSET.length();
        for (int i = 0; i < SUFFIX_LENGTH; i++) {
            sb.append(CHARSET.charAt(RANDOM.nextInt(size)));
        }
        return sb.toString();
    }
}
