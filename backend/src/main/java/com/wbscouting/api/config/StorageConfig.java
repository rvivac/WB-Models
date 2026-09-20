package com.wbscouting.api.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class StorageConfig {

    private final SupabaseProperties supabaseProperties;

    @Bean
    public RestClient supabaseStorageRestClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(15));
        requestFactory.setReadTimeout(Duration.ofSeconds(60));

        String baseUrl = supabaseProperties.getUrl();
        if (baseUrl != null && baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        String effectiveKey = supabaseProperties.getEffectiveKey();

        return RestClient.builder()
                .baseUrl(baseUrl != null ? baseUrl : "")
                .requestFactory(requestFactory)
                .defaultHeader("apikey", effectiveKey)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + effectiveKey)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
