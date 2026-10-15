package com.mymindmirror.backend;

import com.mymindmirror.backend.constants.CacheConstants;
import com.mymindmirror.backend.service.JournalPersistenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Cache behavior — @Cacheable and @CacheEvict end-to-end")
class CacheBehaviorIT extends AbstractIntegrationTest {

    /**
     * Spy on the underlying persistence layer. Real methods run by default;
     * we only count how many times the DB was hit.
     */
    @MockitoSpyBean private JournalPersistenceService journalPersistenceService;

    /** Spy on the real Redis-backed CacheManager. Real by default; overridden only in the Redis-down test. */
    @MockitoSpyBean private CacheManager cacheManager;

    private String aliceJwt;
    private String bobJwt;

    @BeforeEach
    void setUpUsers() throws Exception {
        aliceJwt = registerAndLogin("alice", "password123");
        bobJwt = registerAndLogin("bob", "password123");

        // Clear every cache we're going to touch, so each test starts clean.
        clearCache(CacheConstants.KEY_PHRASE_FREQUENCIES);
        clearCache(CacheConstants.USER_FULL_PROFILE);
        clearCache(CacheConstants.USER_PREFERENCES_DTO);
        clearCache(CacheConstants.API_KEY_STATUS);
        clearCache(CacheConstants.GAMIFICATION_STATS);
    }

    private void clearCache(String name) {
        Cache cache = cacheManager.getCache(name);
        if (cache != null) {
            cache.clear();
        }
    }

    // =================================================================
    // @Cacheable — second call served from cache (spy count stays at 1)
    // =================================================================

    @Test
    @DisplayName("@Cacheable: second GET /key-phrases is served from cache")
    void keyPhrases_secondCallServedFromCache() throws Exception {
        // First call — populates cache
        mockMvc.perform(get("/api/journal/key-phrases")
                        .header("Authorization", "Bearer " + aliceJwt))
                .andExpect(status().isOk());

        // Second call — served from cache, DB spy should NOT be invoked again
        mockMvc.perform(get("/api/journal/key-phrases")
                        .header("Authorization", "Bearer " + aliceJwt))
                .andExpect(status().isOk());

        verify(journalPersistenceService, times(1)).findByUser(any());
    }

    // =================================================================
    // @CacheEvict — saving a journal entry invalidates the key-phrase cache
    // =================================================================

    @Test
    @DisplayName("@CacheEvict: POST /journal evicts the key-phrase cache")
    void saveJournalEntry_evictsKeyPhraseCache() throws Exception {
        // Warm the cache (DB hit #1)
        mockMvc.perform(get("/api/journal/key-phrases")
                        .header("Authorization", "Bearer " + aliceJwt))
                .andExpect(status().isOk());

        verify(journalPersistenceService, times(1)).findByUser(any());

        // Save a new entry — @CacheEvict fires
        mockMvc.perform(post("/api/journal")
                        .header("Authorization", "Bearer " + aliceJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rawText\":\"Today I tested caching.\"}"))
                .andExpect(status().isCreated());

        // Next cacheable call must hit the DB again (DB hit #2)
        mockMvc.perform(get("/api/journal/key-phrases")
                        .header("Authorization", "Bearer " + aliceJwt))
                .andExpect(status().isOk());

        verify(journalPersistenceService, times(2)).findByUser(any());
    }

    // =================================================================
    // Cache key isolation — Bob's call isn't served from Alice's cache
    // =================================================================

    @Test
    @DisplayName("Cache key isolation: Bob's cache is separate from Alice's")
    void cacheKeyIsolation_betweenUsers() throws Exception {
        // Alice populates her cache (DB hit #1)
        mockMvc.perform(get("/api/journal/key-phrases")
                        .header("Authorization", "Bearer " + aliceJwt))
                .andExpect(status().isOk());

        // Bob calls — MUST hit DB independently (DB hit #2), not reuse Alice's cache
        mockMvc.perform(get("/api/journal/key-phrases")
                        .header("Authorization", "Bearer " + bobJwt))
                .andExpect(status().isOk());

        // Exactly 2 DB hits total — proves isolation (would be 1 if keys collided)
        verify(journalPersistenceService, times(2)).findByUser(any());
    }

    // =================================================================
    // Redis down → 503 with cache-specific message
    // =================================================================

    @Test
    @DisplayName("Redis down: cacheable call returns 503 with cache-specific message")
    void redisDown_returns503() throws Exception {
        doThrow(new RedisConnectionFailureException("Redis down"))
                .when(cacheManager).getCache(anyString());

        MvcResult result = mockMvc.perform(get("/api/journal/key-phrases")
                        .header("Authorization", "Bearer " + aliceJwt))
                .andExpect(status().isServiceUnavailable())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("Cache service is temporarily unavailable");
    }
}