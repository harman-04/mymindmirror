package com.mymindmirror.backend;

import com.mymindmirror.backend.constants.CacheConstants;
import com.mymindmirror.backend.enums.GamificationAction;
import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.model.UserStats;
import com.mymindmirror.backend.payload.request.JournalEntryRequest;
import com.mymindmirror.backend.payload.response.UserStatsResponse;
import com.mymindmirror.backend.repository.UserStatsRepository;
import com.mymindmirror.backend.service.GamificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Gamification flow — XP, badges, streaks, cache")
class GamificationFlowIT extends AbstractIntegrationTest {

    @Autowired private GamificationService gamificationService;
    @Autowired private UserStatsRepository userStatsRepository;
    @Autowired private CacheManager cacheManager;

    private User alice;
    private User bob;
    private String aliceJwt;

    @BeforeEach
    void setUpGamificationUsers() throws Exception {
        aliceJwt = registerAndLogin("alice", "password123");
        alice = userRepository.findByUsername("alice").orElseThrow();

        bob = new User();
        bob.setUsername("bob");
        bob.setEmail("bob@test.com");
        bob.setPasswordHash("placeholder-hash");
        bob = userRepository.save(bob);

        clearGamificationCache();
    }

    private void clearGamificationCache() {
        Cache cache = cacheManager.getCache(CacheConstants.GAMIFICATION_STATS);
        if (cache != null) {
            cache.clear();
        }
    }

    private UserStats reloadStats(User user) {
        return userStatsRepository.findByUser(user).orElseThrow();
    }

    private void seedStats(User user, int currentStreak, int longestStreak,
                           LocalDate lastActive, int xp, int journalCount, int taskCount) {
        UserStats seed = new UserStats();
        seed.setUser(user);
        seed.setCurrentStreak(currentStreak);
        seed.setLongestStreak(longestStreak);
        seed.setLastActiveDate(lastActive);
        seed.setExperiencePoints(xp);
        seed.setLevel(1);
        seed.setBadgesJson("[]");
        seed.setTotalTasksCompleted(taskCount);
        seed.setTotalJournalEntries(journalCount);
        seed.setTotalChats(0);
        seed.setSchedulesGenerated(0);
        userStatsRepository.save(seed);
    }

    // =================================================================
    // Initialization
    // =================================================================

    @Test
    @DisplayName("First activity initializes stats with correct defaults")
    void firstActivity_initializesStats() {
        gamificationService.recordActivity(alice, GamificationAction.JOURNAL);

        UserStats stats = reloadStats(alice);
        assertThat(stats.getExperiencePoints()).isEqualTo(50);   // XP_PER_JOURNAL
        assertThat(stats.getTotalJournalEntries()).isEqualTo(1);
        assertThat(stats.getCurrentStreak()).isEqualTo(1);
        assertThat(stats.getLongestStreak()).isEqualTo(1);
        assertThat(stats.getLevel()).isEqualTo(1);
        assertThat(stats.getLastActiveDate()).isEqualTo(LocalDate.now());
    }

    // =================================================================
    // XP per action type
    // =================================================================

    @Test
    @DisplayName("XP awarded per action type matches the constants")
    void xpAwardedPerActionType() {
        // 50 (journal) + 10 (milestone) + 30 (schedule) + 5 (task create) = 95
        gamificationService.recordActivity(alice, GamificationAction.JOURNAL);
        gamificationService.recordActivity(alice, GamificationAction.MILESTONE_CREATE);
        gamificationService.recordActivity(alice, GamificationAction.SCHEDULE);
        gamificationService.recordActivity(alice, GamificationAction.TASK_CREATE);

        assertThat(reloadStats(alice).getExperiencePoints()).isEqualTo(95);
    }

    // =================================================================
    // Level up
    // =================================================================

    @Test
    @DisplayName("Level = 1 + XP / 500 — reaches level 2 at 500 XP")
    void levelIncreasesAt500Xp() {
        for (int i = 0; i < 10; i++) {
            gamificationService.recordActivity(alice, GamificationAction.JOURNAL);
        }

        UserStats stats = reloadStats(alice);
        assertThat(stats.getExperiencePoints()).isEqualTo(500);
        assertThat(stats.getLevel()).isEqualTo(2);
    }

    // =================================================================
    // Streak logic
    // =================================================================

    @Test
    @DisplayName("Streak increments when last activity was yesterday")
    void streakIncrementsOnConsecutiveDay() {
        seedStats(alice, 2, 5, LocalDate.now().minusDays(1), 100, 2, 0);

        gamificationService.recordActivity(alice, GamificationAction.JOURNAL);

        UserStats stats = reloadStats(alice);
        assertThat(stats.getCurrentStreak()).isEqualTo(3);
        assertThat(stats.getLongestStreak()).isEqualTo(5);   // 3 < 5, longest unchanged
    }

    @Test
    @DisplayName("Streak resets to 1 after a gap of 2+ days")
    void streakResetsAfterGap() {
        seedStats(alice, 5, 10, LocalDate.now().minusDays(3), 100, 5, 0);

        gamificationService.recordActivity(alice, GamificationAction.JOURNAL);

        UserStats stats = reloadStats(alice);
        assertThat(stats.getCurrentStreak()).isEqualTo(1);
        assertThat(stats.getLongestStreak()).isEqualTo(10);  // historic max preserved
    }

    @Test
    @DisplayName("Streak badge THREE_DAY_STREAK fires at streak = 3")
    void badgeThreeDayStreakAt3Days() {
        seedStats(alice, 2, 2, LocalDate.now().minusDays(1), 100, 2, 0);

        gamificationService.recordActivity(alice, GamificationAction.JOURNAL);

        UserStats stats = reloadStats(alice);
        assertThat(stats.getCurrentStreak()).isEqualTo(3);
        assertThat(stats.getBadgesJson()).contains("THREE_DAY_STREAK");
    }

    // =================================================================
    // Badges
    // =================================================================

    @Test
    @DisplayName("First journal action awards FIRST_THOUGHT badge")
    void firstJournal_awardsFirstThoughtBadge() {
        gamificationService.recordActivity(alice, GamificationAction.JOURNAL);

        assertThat(reloadStats(alice).getBadgesJson()).contains("FIRST_THOUGHT");
    }

    @Test
    @DisplayName("awardRoadmapCompletedBadge sets ROADMAP_FINISHER")
    void awardRoadmapCompletedBadge_setsFinisher() {
        gamificationService.awardRoadmapCompletedBadge(alice);

        assertThat(reloadStats(alice).getBadgesJson()).contains("ROADMAP_FINISHER");
    }

    // =================================================================
    // HTTP — end-to-end
    // =================================================================

    @Test
    @DisplayName("HTTP: creating a journal entry awards 50 XP end-to-end")
    void httpCreateJournal_awardsXp() throws Exception {
        mockMvc.perform(post("/api/journal")
                        .header("Authorization", "Bearer " + aliceJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new JournalEntryRequest("Testing gamification end-to-end"))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/gamification/stats")
                        .header("Authorization", "Bearer " + aliceJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.experiencePoints").value(50))
                .andExpect(jsonPath("$.level").value(1))
                .andExpect(jsonPath("$.totalJournalEntries").value(1))
                .andExpect(jsonPath("$.currentStreak").value(1));
    }

    @Test
    @DisplayName("HTTP: fresh user without any activity returns zeroed stats")
    void httpFreshUser_returnsZeroedStats() throws Exception {
        mockMvc.perform(get("/api/gamification/stats")
                        .header("Authorization", "Bearer " + aliceJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.experiencePoints").value(0))
                .andExpect(jsonPath("$.level").value(1))
                .andExpect(jsonPath("$.currentStreak").value(0))
                .andExpect(jsonPath("$.badges").isArray());
    }

    // =================================================================
    // Cache behavior
    // =================================================================

    @Test
    @DisplayName("Cache is invalidated and refreshed after recording activity")
    public void cacheInvalidated_afterActivity() throws Exception {
        String token = registerAndLogin("cacheuser", "password123");
        User user = userRepository.findByUsername("cacheuser").orElseThrow();

        // 1. Fetch initial stats to populate the cache
        UserStatsResponse initialStats = gamificationService.getUserStats(user);
        assertThat(initialStats).isNotNull();

        // 2. Perform an activity (triggers @CacheEvict using correct action)
        gamificationService.recordActivity(user, GamificationAction.JOURNAL);

        // 3. Verify using record accessor methods (experiencePoints() instead of getExperiencePoints())
        UserStatsResponse refreshedStats = gamificationService.getUserStats(user);
        assertThat(refreshedStats).isNotNull();
        assertThat(refreshedStats.experiencePoints()).isGreaterThan(initialStats.experiencePoints());
    }
    // =================================================================
    // User isolation
    // =================================================================

    @Test
    @DisplayName("Stats are isolated per user")
    void statsIsolatedPerUser() {
        gamificationService.recordActivity(alice, GamificationAction.JOURNAL);       // +50
        gamificationService.recordActivity(bob, GamificationAction.MILESTONE_CREATE); // +10

        assertThat(reloadStats(alice).getExperiencePoints()).isEqualTo(50);
        assertThat(reloadStats(bob).getExperiencePoints()).isEqualTo(10);
    }
}