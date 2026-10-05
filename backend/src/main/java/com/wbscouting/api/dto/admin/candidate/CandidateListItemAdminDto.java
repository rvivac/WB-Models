package com.wbscouting.api.dto.admin.candidate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.wbscouting.api.enums.CandidateStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CandidateListItemAdminDto {

    private UUID id;
    private String fullName;
    private String email;
    private String phone;
    private String gender;
    private LocalDate birthDate;
    private Integer age;
    private Boolean isMinor;
    private String city;
    private String state;
    private BigDecimal heightCm;
    private BigDecimal bustCm;
    private BigDecimal waistCm;
    private BigDecimal hipsCm;
    private Integer shoeSize;
    private CandidateStatus status;
    private Integer photoCount;
    private OffsetDateTime createdAt;
    private String protocol;

    // 🆕 FOTOS (agora enviadas tambem na LISTAGEM, nao so no detalhe!)
    private String coverPhoto;
    private String facePhotoUrl;
    private String profilePhotoUrl;
    private String fullBodyPhotoUrl;
    private List<CandidatePhotoSignedDto> photos;
}
