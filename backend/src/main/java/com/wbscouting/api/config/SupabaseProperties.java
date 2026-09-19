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
     * Mapeamento dos nomes dos buckets provisionados
     */
    private Buckets buckets = new Buckets();

    @Getter
    @Setter
    public static class Buckets {
        private String siteAssets = "site-assets";
        private String modelsMedia = "models-media";
        private String candidatesUploads = "candidates-uploads";
    }
}
