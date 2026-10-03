package com.wbscouting.api.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SupabaseProperties - Prioridade de Chave Efetiva e Resolução de Buckets")
class SupabasePropertiesEffectiveKeyTest {

    @ParameterizedTest
    @CsvSource(delimiterString = "|", value = {
            // serviceRoleKey | key | anonKey | expectedEffectiveKey | isKeyConfigured
            "sr-valid-123    | k-valid-456    | anon-789    | sr-valid-123   | true",
            "dummy-key       | k-valid-456    | anon-789    | k-valid-456    | true",
            "dummy-key       | dummy-key      | anon-789    | dummy-key      | false",
            "your-service-role-key | k-valid  | anon-x      | k-valid        | true",
            "dummy-key       | dummy-key      | dummy-key   | dummy-key      | false",
            "                |                |             | dummy-key      | false",
            "dummy-key       |                | anon-valido | dummy-key      | false",
            "                | k-valido       |             | k-valido       | true",
    })
    @DisplayName("getEffectiveKey() deve respeitar prioridade serviceRoleKey > key e tratar dummy (anonKey não participa)")
    void deveResolverChaveEfetivaComPrioridadeCorreta(String serviceRoleKey, String key, String anonKey,
                                                       String expectedKey, boolean expectedConfigured) {
        SupabaseProperties props = new SupabaseProperties();
        props.setServiceRoleKey(blankToNull(serviceRoleKey));
        props.setKey(blankToNull(key));
        props.setAnonKey(blankToNull(anonKey));

        String effectiveKey = props.getEffectiveKey();
        boolean isConfigured = props.isKeyConfigured();

        assertEquals(expectedKey.trim(), effectiveKey,
                "Chave efetiva divergente para combinação sr=" + serviceRoleKey + " / key=" + key + " / anon=" + anonKey);
        assertEquals(expectedConfigured, isConfigured,
                "isKeyConfigured divergente (esperado " + expectedConfigured + ")");
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Resolução de buckets deve retornar valores configurados OU fallback padrão")
    void deveResolverBucketsComFallback() {
        SupabaseProperties propsVazios = new SupabaseProperties();
        assertEquals("site-assets", propsVazios.resolveBucketSiteAssets());
        assertEquals("models-media", propsVazios.resolveBucketModelsMedia());
        assertEquals("candidates-uploads", propsVazios.resolveBucketCandidates());

        SupabaseProperties propsCustom = new SupabaseProperties();
        SupabaseProperties.Buckets b = new SupabaseProperties.Buckets();
        b.setSiteAssets("meu-site-assets");
        b.setModelsMedia("meu-models-media");
        b.setCandidatesUploads("meu-cand-uploads");
        propsCustom.setBuckets(b);

        assertEquals("meu-site-assets", propsCustom.resolveBucketSiteAssets());
        assertEquals("meu-models-media", propsCustom.resolveBucketModelsMedia());
        assertEquals("meu-cand-uploads", propsCustom.resolveBucketCandidates());
    }

    @org.junit.jupiter.api.Test
    @DisplayName("isValidBucket deve reconhecer buckets padrão e customizados")
    void deveValidarPertencimentoDeBuckets() {
        SupabaseProperties props = new SupabaseProperties();
        assertTrue(props.isValidBucket("site-assets"));
        assertTrue(props.isValidBucket("models-media"));
        assertTrue(props.isValidBucket("candidates-uploads"));
        assertTrue(props.isValidBucket("  MODELS-MEDIA  "));
        assertFalse(props.isValidBucket("qualquer-outro"));
        assertFalse(props.isValidBucket(null));
        assertFalse(props.isValidBucket("   "));
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String trimmed = s.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
