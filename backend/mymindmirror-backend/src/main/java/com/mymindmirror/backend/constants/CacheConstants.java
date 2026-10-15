package com.mymindmirror.backend.constants;

import java.util.Set;

public final class CacheConstants {

    private CacheConstants() {
        // Private constructor to prevent instantiation
    }

    public static final String USER_FULL_PROFILE = "userFullProfile";
    public static final String USER_PREFERENCES_DTO = "userPreferencesDto";
    public static final String GAMIFICATION_STATS = "gamificationStats";
    public static final String KEY_PHRASE_FREQUENCIES = "keyPhraseFrequencies";
    public static final String API_KEY_STATUS = "apiKeyStatus";

    /**
     * Returns a set of all cache names for initialisation (e.g., in CacheConfig).
     */
    public static Set<String> allCacheNames() {
        return Set.of(
                USER_FULL_PROFILE,
                USER_PREFERENCES_DTO,
                GAMIFICATION_STATS,
                KEY_PHRASE_FREQUENCIES,
                API_KEY_STATUS
        );
    }
}