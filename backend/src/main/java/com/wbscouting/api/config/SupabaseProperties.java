package com.wbscouting.api.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.Set;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "supabase")
public class SupabaseProperties {

    private static final String FALLBACK_SITE_ASSETS = "site-assets";
    private static final String FALLBACK_MODELS_MEDIA = "models-media";
    private static final String FALLBACK_CANDIDATES_UPLOADS = "candidates-uploads";
    private static final Set<String> KNOWN_BUCKETS = Set.of(FALLBACK_SITE_ASSETS, FALLBACK_MODELS_MEDIA, FALLBACK_CANDIDATES_UPLOADS);

    private String url;

    private String serviceRoleKey;

    private String anonKey;

    private String key;

    private Storage storage = new Storage();

    private Buckets buckets = new Buckets();

    /**
     * Resolve a chave efetiva de acesso ao Supabase com prioridade estrita:
     * 1. serviceRoleKey (bypass RLS / backend administrativo) — usada para uploads e
     *    operações privilegiadas de leitura.
     * 2. key (chave genérica de compatibilidade — se o usuário desejar centralizar
     *    a service role aqui).
     * A 'anonKey' NÃO participa desta cascata, pois é uma chave pública de leitura
     * (restrita pela RLS da role 'anon') e não tem permissão para escrita em Storage
     * nem para bypass de RLS em tabelas. Use getAnonKeyOrEmpty() para expô-la em
     * cenários específicos de leitura pública.
     * Retorna "dummy-key" apenas quando nenhuma chave válida foi configurada.
     */
    public String getEffectiveKey() {
        if (!isDummy(serviceRoleKey)) {
            return serviceRoleKey.trim();
        }
        if (!isDummy(key)) {
            return key.trim();
        }
        return "dummy-key";
    }

    public boolean isKeyConfigured() {
        return !isDummy(getEffectiveKey());
    }

    public String getAnonKeyOrEmpty() {
        return StringUtils.hasText(anonKey) ? anonKey.trim() : "";
    }

    public String resolveBucketSiteAssets() {
        if (buckets != null && StringUtils.hasText(buckets.getSiteAssets())) {
            return buckets.getSiteAssets().trim();
        }
        return FALLBACK_SITE_ASSETS;
    }

    public String resolveBucketModelsMedia() {
        if (buckets != null && StringUtils.hasText(buckets.getModelsMedia())) {
            return buckets.getModelsMedia().trim();
        }
        return FALLBACK_MODELS_MEDIA;
    }

    public String resolveBucketCandidates() {
        if (buckets != null && StringUtils.hasText(buckets.getCandidatesUploads())) {
            return buckets.getCandidatesUploads().trim();
        }
        return FALLBACK_CANDIDATES_UPLOADS;
    }

    public boolean isValidBucket(String bucket) {
        if (!StringUtils.hasText(bucket)) {
            return false;
        }
        String normalized = bucket.trim().toLowerCase();
        return KNOWN_BUCKETS.contains(normalized)
                || normalized.equals(resolveBucketSiteAssets().toLowerCase())
                || normalized.equals(resolveBucketModelsMedia().toLowerCase())
                || normalized.equals(resolveBucketCandidates().toLowerCase());
    }

    public static boolean isDummy(String val) {
        if (val == null || val.isBlank()) {
            return true;
        }
        String trimmed = val.trim();
        return "dummy-key".equalsIgnoreCase(trimmed)
                || "your-service-role-key".equalsIgnoreCase(trimmed)
                || "your-anon-key".equalsIgnoreCase(trimmed);
    }

    // ============================================================
    // 🔥 GETTERS/SETTERS EXPLICITOS (Lombok @Getter/@Setter nao processa no mvnw 3.6.3)
    // ============================================================
    public String getUrl() { return this.url; }
    public void setUrl(String url) { this.url = url; }
    public String getServiceRoleKey() { return this.serviceRoleKey; }
    public void setServiceRoleKey(String serviceRoleKey) { this.serviceRoleKey = serviceRoleKey; }
    public String getAnonKey() { return this.anonKey; }
    public void setAnonKey(String anonKey) { this.anonKey = anonKey; }
    public String getKey() { return this.key; }
    public void setKey(String key) { this.key = key; }
    public Storage getStorage() { return this.storage; }
    public void setStorage(Storage storage) { this.storage = storage; }
    public Buckets getBuckets() { return this.buckets; }
    public void setBuckets(Buckets buckets) { this.buckets = buckets; }

    public static class Storage {
        private boolean localFallback = false;
        private String localDir = "uploads";
        private String localBaseUrl = "http://localhost:8080";
        private String publicBaseUrl;

        public boolean isLocalFallback() { return localFallback; }
        public void setLocalFallback(boolean localFallback) { this.localFallback = localFallback; }
        public String getLocalDir() { return localDir; }
        public void setLocalDir(String localDir) { this.localDir = localDir; }
        public String getLocalBaseUrl() { return localBaseUrl; }
        public void setLocalBaseUrl(String localBaseUrl) { this.localBaseUrl = localBaseUrl; }
        public String getPublicBaseUrl() { return publicBaseUrl; }
        public void setPublicBaseUrl(String publicBaseUrl) { this.publicBaseUrl = publicBaseUrl; }
    }

    public static class Buckets {
        private String siteAssets = FALLBACK_SITE_ASSETS;
        private String modelsMedia = FALLBACK_MODELS_MEDIA;
        private String candidatesUploads = FALLBACK_CANDIDATES_UPLOADS;

        public String getSiteAssets() { return siteAssets; }
        public void setSiteAssets(String siteAssets) { this.siteAssets = siteAssets; }
        public String getModelsMedia() { return modelsMedia; }
        public void setModelsMedia(String modelsMedia) { this.modelsMedia = modelsMedia; }
        public String getCandidatesUploads() { return candidatesUploads; }
        public void setCandidatesUploads(String candidatesUploads) { this.candidatesUploads = candidatesUploads; }
    }
}
