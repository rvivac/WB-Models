package com.wbscouting.api.service.content;

import com.wbscouting.api.dto.content.AssetUploadResponseDto;
import com.wbscouting.api.dto.content.SiteContentAdminDto;
import com.wbscouting.api.dto.content.SiteContentPublicDto;
import com.wbscouting.api.dto.content.SiteContentUpdateRequestDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface SiteContentService {

    /**
     * Retorna o conteúdo público desempacotado para a seção no idioma solicitado (com fallback para PT).
     */
    SiteContentPublicDto getPublicContent(String sectionKey, String lang);

    /**
     * Retorna um mapa consolidado de todas as seções ativas para cache no frontend.
     */
    Map<String, Map<String, Object>> getAllPublicContent(String lang);

    /**
     * Lista todas as seções e seus respectivos payloads em ambos os idiomas (Painel CMS).
     */
    List<SiteContentAdminDto> getAllAdminContent();

    /**
     * Atualiza ou insere (upsert) os conteúdos de uma seção com ambos os idiomas e metadados.
     */
    SiteContentAdminDto updateContent(String sectionKey, SiteContentUpdateRequestDto dto, UUID adminId);

    /**
     * Realiza o upload de mídia institucional (imagens até 10MB, vídeos até 25MB) para o bucket 'site-assets'.
     */
    AssetUploadResponseDto uploadAsset(MultipartFile file, String folder);
}
