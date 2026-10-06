package com.wbscouting.api.controller.publicapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wbscouting.api.dto.ApiResponse;
import com.wbscouting.api.dto.CandidateSubmissionRequestDto;
import com.wbscouting.api.dto.CandidateSubmissionResponseDto;
import com.wbscouting.api.service.submission.CandidateSubmissionService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

@Slf4j
@RestController
@RequestMapping({"/public/candidates", "/api/v1/public/candidates"})
@RequiredArgsConstructor
public class PublicCandidateController {

    /** Serviço de negócio com protocolo WB-YYYYMMDD-XXXX + persistência em candidate_photos */
    private final CandidateSubmissionService submissionService;

    /** ObjectMapper para desserializar MANUALMENTE o part 'data' multipart (JSON embutido em Blob) */
    private final ObjectMapper objectMapper;

    /** Validator JSR-380 aplicado APÓS desserialização manual (equivalente ao @Valid do Spring) */
    private final Validator validator;

    @PostMapping(value = "/apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CandidateSubmissionResponseDto>> apply(
            @RequestPart("data") String dataJson,
            @RequestPart(value = "facePhoto", required = false) MultipartFile facePhoto,
            @RequestPart(value = "profilePhoto", required = false) MultipartFile profilePhoto,
            @RequestPart(value = "fullBodyPhoto", required = false) MultipartFile fullBodyPhoto
    ) {
        try {
            // 1) Blindagem: desserializa o Blob JSON ('data') manualmente p/ evitar que o Spring erre
            //    Content-Type=application/octet-stream automatico do FormData em navegadores modernos.
            CandidateSubmissionRequestDto dto = objectMapper.readValue(
                    dataJson,
                    CandidateSubmissionRequestDto.class
            );

            // 2) Aplica @Valid MANUALMENTE (Bean Validation JSR-380) - equivalente a "@Valid" em
            //    @RequestPart mas sem depender do HttpMessageConverter do Spring p/ multipart JSON.
            Set<ConstraintViolation<CandidateSubmissionRequestDto>> violations = validator.validate(dto);
            if (!violations.isEmpty()) {
                String firstMsg = violations.iterator().next().getMessage();
                log.warn("[APPLY] Payload invalido no part 'data': {} violations — primeira: {}",
                        violations.size(), firstMsg);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, firstMsg);
            }

            log.info("[APPLY] Candidatura recebida via /apply (multipart): nome='{}' email='{}'",
                    dto.getFullName(), dto.getEmail());

            // 3) Delega para o serviço (geração protocolo, upload storage, persistência DB).
            CandidateSubmissionResponseDto response = submissionService.submit(
                    dto,
                    facePhoto,
                    profilePhoto,
                    fullBodyPhoto
            );

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Candidatura enviada com sucesso.", response));

        } catch (ResponseStatusException ex) {
            // Propaga erros de validação já formatados pelo Spring (HttpStatus + motivo)
            throw ex;
        } catch (com.fasterxml.jackson.core.JacksonException ex) {
            // JSON mal formatado vindo do frontend (Blob 'data' quebrado)
            log.error("[APPLY] Falha de desserializacao no part 'data': {}", ex.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payload JSON do formulário está inválido. Revise os campos e tente novamente."
            );
        } catch (Exception ex) {
            // Falha de negócio do service (ex: falha upload bucket, null pointer, etc)
            log.error("[APPLY] Falha geral no submitApplication: {}", ex.getMessage(), ex);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Falha ao submeter candidatura. Por favor, revise seus dados e tente novamente."
            );
        }
    }
}
