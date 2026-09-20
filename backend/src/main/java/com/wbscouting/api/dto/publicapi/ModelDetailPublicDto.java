package com.wbscouting.api.dto.publicapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.wbscouting.api.enums.GenderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ModelDetailPublicDto {

    private UUID id;
    private String stageName;
    private GenderType gender;
    private Boolean isStar;
    private String city;
    private String nationality;
    private Integer age;
    private String instagramUrl;
    private String instagramHandle;
    private String compositeUrl;

    // Medidas biométricas (apenas preenchidas / não nulas)
    private Integer heightCm;
    private BigDecimal bustChestCm;
    private BigDecimal waistCm;
    private BigDecimal hipsCm;
    private String dressSize;
    private String shoeSize;
    private String eyeColor;
    private String hairColor;

    // Mídias categorizadas
    private List<ModelMediaPublicItemDto> bookPhotos;
    private List<ModelMediaPublicItemDto> polaroids;
    private ModelMediaPublicItemDto composite;

    public String getCompositeUrl() {
        if (compositeUrl != null && !compositeUrl.isBlank()) {
            return compositeUrl;
        }
        return composite != null ? composite.getFileUrl() : null;
    }

    public String getInstagramHandle() {
        if (instagramHandle != null && !instagramHandle.isBlank()) {
            return instagramHandle;
        }
        if (instagramUrl == null || instagramUrl.isBlank()) {
            return null;
        }
        String clean = instagramUrl.trim();
        if (clean.startsWith("http://") || clean.startsWith("https://")) {
            clean = clean.replaceAll("/+$", "");
            int slash = clean.lastIndexOf('/');
            if (slash >= 0 && slash < clean.length() - 1) {
                return "@" + clean.substring(slash + 1).replace("@", "");
            }
            return clean;
        }
        return clean.startsWith("@") ? clean : "@" + clean;
    }
}
