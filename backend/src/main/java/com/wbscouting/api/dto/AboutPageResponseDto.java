package com.wbscouting.api.dto;

public record AboutPageResponseDto(
        String title,          // Suporte legado
        String headline,       // Título Principal
        String pageTitle,      // Alias para headline
        String quote,          // Frase de destaque
        String heroQuote,      // Alias para quote
        String sectionTitle,   // Ex: "Nossa Filosofia"
        String manifestoTitle, // Alias para sectionTitle
        String body,           // Corpo editorial completo
        String manifestoText   // Alias para body
) {
    public AboutPageResponseDto(
            String headline,
            String pageTitle,
            String quote,
            String heroQuote,
            String sectionTitle,
            String manifestoTitle,
            String body,
            String manifestoText
    ) {
        this(
                headline != null && !headline.isBlank() ? headline : pageTitle,
                headline,
                pageTitle != null && !pageTitle.isBlank() ? pageTitle : headline,
                quote,
                heroQuote != null && !heroQuote.isBlank() ? heroQuote : quote,
                sectionTitle,
                manifestoTitle != null && !manifestoTitle.isBlank() ? manifestoTitle : sectionTitle,
                body,
                manifestoText != null && !manifestoText.isBlank() ? manifestoText : body
        );
    }
}
