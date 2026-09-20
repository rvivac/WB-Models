package com.wbscouting.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelPublicDetailDto {

    private UUID id;
    private String fullName;
    private String slug;
    private String gender;
    private LocalDate birthDate;
    private Integer height;
    private Integer bust;
    private Integer waist;
    private Integer hips;
    private Integer shoeSize;
    private String eyeColor;
    private String hairColor;
    private String profilePhotoUrl;
    private String compositeUrl;       // URL pública do composite no Supabase Storage
    private String instagramHandle;     // Handle (@usuario) ou URL completa
    private List<String> bookPhotos;
    private List<String> polaroids;
    private Boolean isStar;
}
