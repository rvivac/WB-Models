package com.wbscouting.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wbscouting.api.dto.candidate.CandidateApplyRequestDto;
import com.wbscouting.api.exception.InvalidApplicationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StringToCandidateApplyRequestDtoConverter implements Converter<String, CandidateApplyRequestDto> {

    private final ObjectMapper objectMapper;

    @Override
    public CandidateApplyRequestDto convert(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(source, CandidateApplyRequestDto.class);
        } catch (Exception e) {
            log.error("Falha ao desserializar JSON de CandidateApplyRequestDto a partir de string multipart", e);
            throw new InvalidApplicationException("Payload de candidato em formato JSON inválido: " + e.getMessage(), e);
        }
    }
}
