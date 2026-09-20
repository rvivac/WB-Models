package com.wbscouting.api.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "supabase")
public class SupabaseProperties {

    /**
     * URL base do projeto Supabase (ex: https://xyzcompany.supabase.co)
     */
    private String url;

    /**
     * Chave de serviço administrativa (Service Role Key) para operações com bypass de RLS
     */
    private String serviceRoleKey;

    /**
     * Chave anônima pública (Anon Key)
     */
    private String anonKey;

    /**
     * Chave genérica de API (supabase.key) alternativa
     */
    private String key;

    /**
     * Configurações de armazenamento e fallback local
     */
    private Storage storage = new Storage();

    /**
     * Mapeamento dos nomes dos buckets provisionados
     */
    private Buckets buckets = new Buckets();

    public String getEffectiveKey() {
        if (org.springframework.util.StringUtils.hasText(serviceRoleKey) && !isDummy(serviceRoleKey)) {
            return serviceRoleKey.trim();
        }
        if (org.springframework.util.StringUtils.hasText(key) && !isDummy(key)) {
            return key.trim();
        }
        if (org.springframework.util.StringUtils.hasText(serviceRoleKey)) {
            return serviceRoleKey.trim();
        }
        if (org.springframework.util.StringUtils.hasText(key)) {
            return key.trim();
        }
        return "dummy-key";
    }

    public boolean isKeyConfigured() {
        String effective = getEffectiveKey();
        return org.springframework.util.StringUtils.hasText(effective) && !isDummy(effective);
    }

    public static boolean isDummy(String val) {
        if (val == null || val.isBlank()) {
            return true;
        }
        String trimmed = val.trim();
        return "dummy-key".equalsIgnoreCase(trimmed) || "your-service-role-key".equalsIgnoreCase(trimmed);
    }

    @Getter
    @Setter
    public static class Storage {
        private boolean localFallback = false;
        private String localDir = "uploads";
        private String localBaseUrl = "http://localhost:8080";
    }

    @Getter
    @Setter
    public static class Buckets {
        private String siteAssets = "site-assets";
        private String modelsMedia = "models-media";
        private String candidatesUploads = "candidates-uploads";
    }
}
