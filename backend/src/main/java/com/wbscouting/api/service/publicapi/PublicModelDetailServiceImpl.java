package com.wbscouting.api.service.publicapi;

import com.wbscouting.api.dto.publicapi.ModelDetailPublicDto;
import com.wbscouting.api.dto.publicapi.ModelMediaPublicItemDto;
import com.wbscouting.api.entity.Model;
import com.wbscouting.api.entity.ModelMedia;
import com.wbscouting.api.enums.MediaType;
import com.wbscouting.api.exception.ResourceNotFoundException;
import com.wbscouting.api.repository.ModelMediaRepository;
import com.wbscouting.api.repository.ModelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicModelDetailServiceImpl implements PublicModelDetailService {

    private final ModelRepository modelRepository;
    private final ModelMediaRepository modelMediaRepository;

    @Override
    @Transactional(readOnly = true)
    public ModelDetailPublicDto getModelDetail(UUID id) {
        log.info("Buscando detalhes públicos do perfil do modelo ID: {}", id);

        Model model = modelRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Modelo não encontrado ou inativo"));

        List<ModelMedia> mediaList = modelMediaRepository.findByModelIdAndModelIsActiveTrueOrderByDisplayOrderAsc(id);

        List<ModelMediaPublicItemDto> bookPhotos = new ArrayList<>();
        List<ModelMediaPublicItemDto> polaroids = new ArrayList<>();
        ModelMediaPublicItemDto composite = null;

        for (ModelMedia m : mediaList) {
            if (Boolean.TRUE.equals(m.getIsActive())) {
                ModelMediaPublicItemDto item = ModelMediaPublicItemDto.builder()
                        .id(m.getId())
                        .fileUrl(m.getFileUrl())
                        .displayOrder(m.getDisplayOrder())
                        .isCover(m.getIsCover())
                        .build();

                if (m.getMediaType() == MediaType.BOOK) {
                    bookPhotos.add(item);
                } else if (m.getMediaType() == MediaType.POLAROID) {
                    polaroids.add(item);
                } else if (m.getMediaType() == MediaType.COMPOSITE) {
                    if (composite == null) {
                        composite = item;
                    }
                }
            }
        }

        Integer age = null;
        if (model.getBirthDate() != null) {
            age = Period.between(model.getBirthDate(), LocalDate.now()).getYears();
        }

        String compositeUrl = composite != null ? composite.getFileUrl() : null;
        String rawInstagram = model.getInstagramUrl();
        String instagramHandle = null;
        if (rawInstagram != null && !rawInstagram.isBlank()) {
            String clean = rawInstagram.trim();
            if (clean.startsWith("http://") || clean.startsWith("https://")) {
                clean = clean.replaceAll("/+$", "");
                int slash = clean.lastIndexOf('/');
                if (slash >= 0 && slash < clean.length() - 1) {
                    instagramHandle = "@" + clean.substring(slash + 1).replace("@", "");
                } else {
                    instagramHandle = clean;
                }
            } else {
                instagramHandle = clean.startsWith("@") ? clean : "@" + clean;
            }
        }

        return ModelDetailPublicDto.builder()
                .id(model.getId())
                .stageName(model.getStageName())
                .gender(model.getGender())
                .isStar(model.getIsStar())
                .city(model.getCity())
                .nationality(model.getNationality())
                .age(age)
                .instagramUrl(model.getInstagramUrl())
                .instagramHandle(instagramHandle)
                .heightCm(model.getHeightCm())
                .bustChestCm(model.getBustChestCm())
                .waistCm(model.getWaistCm())
                .hipsCm(model.getHipsCm())
                .dressSize(model.getDressSize())
                .shoeSize(model.getShoeSize())
                .eyeColor(model.getEyesColor())
                .hairColor(model.getHairColor())
                .bookPhotos(bookPhotos)
                .polaroids(polaroids)
                .composite(composite)
                .compositeUrl(compositeUrl)
                .build();
    }
}
