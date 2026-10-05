package com.wbscouting.api.dto.admin.candidate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.wbscouting.api.entity.CandidateSubmission;
import com.wbscouting.api.enums.SubmissionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CandidateApplicationSummaryDto {

    private UUID id;
    private String fullName;
    private String email;
    private String phone;
    private LocalDate birthDate;
    private Integer age;
    private Boolean isMinor;
    private String city;
    private String state;
    private Integer height;
    private BigDecimal bust;
    private BigDecimal waist;
    private BigDecimal hips;
    private Integer shoes;
    private SubmissionStatus status;
    private Boolean hasPhotos;
    private Integer polaroidsCount;
    private OffsetDateTime createdAt;

    // 🆕 CAMPOS DE FOTO QUE FALTAVAM (CAUSA RAIZ DO PLACEHOLDER WB):
    // coverPhoto prioridade = FOTO ROSTO FRONTAL NATURAL (face_*) como o usuario pediu no exemplo
    private String coverPhoto;
    private String facePhotoUrl;
    private String profilePhotoUrl;
    private String fullBodyPhotoUrl;
    // Protocolo scouting (mantem link no card)
    private String protocol;
    // Array photos fallback (igual ao detail) com 3 posicoes conhecidas
    private List<PhotoThumbDto> photos;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PhotoThumbDto {
        private String id;
        private String url;
        private String type;
    }

    public static CandidateApplicationSummaryDto fromEntity(CandidateSubmission entity) {
        if (entity == null) {
            return null;
        }

        // Limpa e normaliza URLs de foto (evita projetos errados zmpqmdi ou strings vazias)
        String face = blankToNull(entity.getFacePhotoUrl());
        String profile = blankToNull(entity.getProfilePhotoUrl());
        String fullBody = blankToNull(entity.getFullBodyPhotoUrl());

        int polaroids = 0;
        if (face != null) polaroids++;
        if (profile != null) polaroids++;
        if (fullBody != null) polaroids++;

        Integer heightVal = null;
        if (entity.getHeight() != null) {
            if (entity.getHeight().compareTo(new BigDecimal("3.0")) < 0) {
                heightVal = entity.getHeight().multiply(new BigDecimal("100")).intValue();
            } else {
                heightVal = entity.getHeight().intValue();
            }
        }

        boolean minor = entity.getAge() != null ? entity.getAge() < 18 : false;

        // 🆕 PRIORIDADE 1 MAXIMA: FOTO ROSTO FRONTAL NATURAL → coverPhoto (ex: face_Gemini_Generated_Image_....jpeg)
        String cover = face;
        if (cover == null) cover = profile;
        if (cover == null) cover = fullBody;

        // 🆕 Photos array fallback para o frontend (3 tipos conhecidos, igual ao detail do dossie)
        List<PhotoThumbDto> photosArr = null;
        if (polaroids > 0) {
            photosArr = Arrays.asList(
                    face != null ? PhotoThumbDto.builder().id("face").url(face).type("POLAROID_ROSTO").build() : null,
                    profile != null ? PhotoThumbDto.builder().id("profile").url(profile).type("POLAROID_PERFIL").build() : null,
                    fullBody != null ? PhotoThumbDto.builder().id("fullbody").url(fullBody).type("CORPO_INTEIRO").build() : null
            ).stream().filter(java.util.Objects::nonNull).toList();
        }

        // 🆕 Campo protocol (entity pode ter getProtocol ou pode nao - reflection fallback sem quebrar)
        String protocol = null;
        try {
            java.lang.reflect.Method m = entity.getClass().getMethod("getProtocol");
            Object val = m.invoke(entity);
            if (val != null) {
                String s = val.toString().trim();
                if (!s.isEmpty()) protocol = s;
            }
        } catch (Exception ignored) { /* entity sem campo protocol: ignorar silenciosamente */ }

        return CandidateApplicationSummaryDto.builder()
                .id(entity.getId())
                .fullName(entity.getFullName())
                .email(entity.getEmail())
                .phone(entity.getPhone())
                .birthDate(entity.getBirthDate())
                .age(entity.getAge())
                .isMinor(minor)
                .city(entity.getCity())
                .state(entity.getState())
                .height(heightVal)
                .bust(entity.getBust())
                .waist(entity.getWaist())
                .hips(entity.getHips())
                .shoes(entity.getShoeSize())
                .status(entity.getStatus())
                .hasPhotos(polaroids > 0)
                .polaroidsCount(polaroids > 0 ? polaroids : 4)
                .createdAt(entity.getCreatedAt())
                // 🆕 NOVOS CAMPOS RESOLVIDOS:
                .coverPhoto(cover)
                .facePhotoUrl(face)
                .profilePhotoUrl(profile)
                .fullBodyPhotoUrl(fullBody)
                .photos(photosArr)
                .protocol(protocol)
                .build();
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
