package com.wbscouting.api.service.storage;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    /**
     * Realiza o upload de um arquivo para o bucket e caminho especificados.
     *
     * @param bucket Nome do bucket de destino (ex: site-assets, models-media, candidates-uploads)
     * @param path Caminho relativo / prefixo do objeto no bucket
     * @param file Arquivo multipart enviado pelo cliente
     * @return Caminho relativo do objeto armazenado no bucket ou identificador de acesso
     */
    String uploadFile(String bucket, String path, MultipartFile file);

    /**
     * Remove um arquivo do bucket especificado.
     *
     * @param bucket Nome do bucket
     * @param path Caminho do objeto a ser removido
     */
    void deleteFile(String bucket, String path);

    /**
     * Constrói a URL pública direta para acesso ao arquivo no Supabase Storage.
     * Aplicável aos buckets públicos (ex: site-assets, models-media).
     *
     * @param bucket Nome do bucket público
     * @param path Caminho do objeto
     * @return URL pública absoluta do arquivo
     */
    String getPublicUrl(String bucket, String path);

    /**
     * Helper FALLBACK para URLs públicas gravadas incompletas no banco (ex: "/public/models-media/" sem path).
     * Remonta a URL combinando bucket + filePath real do objeto quando a fileUrl do banco estiver quebrada.
     * Também impede URLs que terminem com "/" (inválidas, causam HTTP 400).
     *
     * @param bucket Nome do bucket (ex: models-media)
     * @param filePath Caminho relativo do arquivo no bucket (coluna file_path da model_media)
     * @param fileUrlFromDb Valor cru salvo na coluna file_url (pode ser null ou incompleto)
     * @return URL pública final válida, ou null se não houver informação suficiente.
     */
    String resolvePublicUrlFromFields(String bucket, String filePath, String fileUrlFromDb);

    /**
     * Gera uma Signed URL com expiração temporal para acesso autenticado a objetos privados.
     * Aplicável primordialmente ao bucket privado 'candidates-uploads' (conformidade LGPD).
     *
     * @param bucket Nome do bucket privado
     * @param path Caminho do objeto
     * @param expiresInSeconds Tempo de vida da URL em segundos
     * @return URL assinada completa para visualização temporária
     */
    String createSignedUrl(String bucket, String path, int expiresInSeconds);
}
