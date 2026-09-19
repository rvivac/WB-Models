package com.wbscouting.api.constant;

import java.util.Set;

public final class ContentSectionKey {

    private ContentSectionKey() {}

    public static final String HOME_HERO = "HOME_HERO";
    public static final String ABOUT_US = "ABOUT_US";
    public static final String CONTACT_INFO = "CONTACT_INFO";
    public static final String SOCIAL_LINKS = "SOCIAL_LINKS";
    public static final String TERMS_PRIVACY = "TERMS_PRIVACY";

    public static final Set<String> CANONICAL_KEYS = Set.of(
            HOME_HERO,
            ABOUT_US,
            CONTACT_INFO,
            SOCIAL_LINKS,
            TERMS_PRIVACY
    );
}
