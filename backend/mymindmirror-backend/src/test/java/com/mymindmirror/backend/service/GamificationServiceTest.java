package com.mymindmirror.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mymindmirror.backend.enums.GamificationAction;
import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.model.UserStats;
import com.mymindmirror.backend.payload.response.UserStatsResponse;
import com.mymindmirror.backend.repository.UserStatsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GamificationServiceTest {

    @Mock
    private UserStatsRepository userStatsRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private GamificationService gamificationService;

    private User user;
    private UserStats stats;

    @BeforeEach
    void setUp() throws Exception {
        user = new User();
        user.setId(java.util.UUID.randomUUID());
        user.setUsername("testuser");

        stats = new UserStats();
        stats.setUser(user);
        stats.setCurrentStreak(0);
        stats.setLongestStreak(0);
        stats.setLastActiveDate(null);
        stats.setBadgesJson("[]");
        stats.setTotalTasksCompleted(0);
        stats.setExperiencePoints(0);
        stats.setLevel(1);
        stats.setTotalJournalEntries(0);
        stats.setTotalChats(0);
        stats.setSchedulesGenerated(0);

        //  Use lenient() to avoid UnnecessaryStubbingException
        lenient().when(objectMapper.readValue(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(new HashSet<>());
        lenient().when(objectMapper.writeValueAsString(any(Set.class)))
                .thenReturn("[\"FAKE_BADGE\"]");
    }

    // ========== initializeStats Tests ==========

    @Test
    void initializeStats_shouldCreateAndSaveStatsWithDefaults() {
        when(userStatsRepository.save(any(UserStats.class))).thenAnswer(i -> i.getArgument(0));

        UserStats result = gamificationService.initializeStats(user);

        assertThat(result.getUser()).isEqualTo(user);
        assertThat(result.getCurrentStreak()).isEqualTo(0);
        assertThat(result.getLongestStreak()).isEqualTo(0);
        assertThat(result.getLastActiveDate()).isNull();
        assertThat(result.getBadgesJson()).isEqualTo("[]");
        assertThat(result.getTotalTasksCompleted()).isEqualTo(0);
        assertThat(result.getExperiencePoints()).isEqualTo(0);
        assertThat(result.getLevel()).isEqualTo(1);
        assertThat(result.getTotalJournalEntries()).isEqualTo(0);
        assertThat(result.getTotalChats()).isEqualTo(0);
        assertThat(result.getSchedulesGenerated()).isEqualTo(0);
        verify(userStatsRepository).save(any(UserStats.class));
    }

    // ========== recordActivity Tests ==========

    @Test
    void recordActivity_shouldIncrementStreak_whenActivityOnConsecutiveDay() {
        LocalDate today = LocalDate.now();
        stats.setLastActiveDate(today.minusDays(1));
        stats.setCurrentStreak(2);
        when(userStatsRepository.findByUser(user)).thenReturn(Optional.of(stats));
        when(userStatsRepository.save(any(UserStats.class))).thenAnswer(i -> i.getArgument(0));

        gamificationService.recordActivity(user, GamificationAction.TASK);

        assertThat(stats.getCurrentStreak()).isEqualTo(3);
        assertThat(stats.getLastActiveDate()).isEqualTo(today);
        verify(userStatsRepository).save(stats);
    }

    @Test
    void recordActivity_shouldResetStreak_whenGapInDays() {
        LocalDate today = LocalDate.now();
        stats.setLastActiveDate(today.minusDays(2));
        stats.setCurrentStreak(5);
        when(userStatsRepository.findByUser(user)).thenReturn(Optional.of(stats));
        when(userStatsRepository.save(any(UserStats.class))).thenAnswer(i -> i.getArgument(0));

        gamificationService.recordActivity(user, GamificationAction.JOURNAL);

        assertThat(stats.getCurrentStreak()).isEqualTo(1);
        assertThat(stats.getLastActiveDate()).isEqualTo(today);
        verify(userStatsRepository).save(stats);
    }

    @Test
    void recordActivity_shouldUpdateLongestStreak_whenExceeds() {
        LocalDate today = LocalDate.now();
        stats.setLastActiveDate(today.minusDays(1));
        stats.setCurrentStreak(5);
        stats.setLongestStreak(5);
        when(userStatsRepository.findByUser(user)).thenReturn(Optional.of(stats));
        when(userStatsRepository.save(any(UserStats.class))).thenAnswer(i -> i.getArgument(0));

        gamificationService.recordActivity(user, GamificationAction.TASK);

        assertThat(stats.getLongestStreak()).isEqualTo(6);
        verify(userStatsRepository).save(stats);
    }

    @Test
    void recordActivity_shouldAwardStreakBadges() throws Exception {
        LocalDate today = LocalDate.now();
        stats.setLastActiveDate(today.minusDays(1));
        stats.setCurrentStreak(2);
        when(userStatsRepository.findByUser(user)).thenReturn(Optional.of(stats));
        when(userStatsRepository.save(any(UserStats.class))).thenAnswer(i -> i.getArgument(0));
        when(objectMapper.writeValueAsString(any(Set.class))).thenReturn("[\"THREE_DAY_STREAK\"]");

        gamificationService.recordActivity(user, GamificationAction.TASK);

        ArgumentCaptor<UserStats> captor = ArgumentCaptor.forClass(UserStats.class);
        verify(userStatsRepository).save(captor.capture());
        UserStats saved = captor.getValue();
        assertThat(saved.getBadgesJson()).contains("THREE_DAY_STREAK");
    }

    @Test
    void recordActivity_shouldAwardBadgeForTaskCompletion() throws Exception {
        // Given
        stats.setTotalTasksCompleted(0);
        when(userStatsRepository.findByUser(user)).thenReturn(Optional.of(stats));
        when(userStatsRepository.save(any(UserStats.class))).thenAnswer(i -> i.getArgument(0));
        when(objectMapper.writeValueAsString(any(Set.class))).thenReturn("[\"FIRST_STEP\"]");

        // When
        gamificationService.recordActivity(user, GamificationAction.TASK);

        // Then
        assertThat(stats.getTotalTasksCompleted()).isEqualTo(1);
        assertThat(stats.getExperiencePoints()).isEqualTo(20); // XP_PER_TASK_COMPLETE = 20
        ArgumentCaptor<UserStats> captor = ArgumentCaptor.forClass(UserStats.class);
        verify(userStatsRepository).save(captor.capture());
        UserStats saved = captor.getValue();
        assertThat(saved.getBadgesJson()).contains("FIRST_STEP");
    }

    @Test
    void recordActivity_shouldAwardJournalBadge() throws Exception {
        // Given
        stats.setTotalJournalEntries(0);
        when(userStatsRepository.findByUser(user)).thenReturn(Optional.of(stats));
        when(userStatsRepository.save(any(UserStats.class))).thenAnswer(i -> i.getArgument(0));
        when(objectMapper.writeValueAsString(any(Set.class))).thenReturn("[\"FIRST_THOUGHT\"]");

        // When
        gamificationService.recordActivity(user, GamificationAction.JOURNAL);

        // Then
        assertThat(stats.getTotalJournalEntries()).isEqualTo(1);
        assertThat(stats.getExperiencePoints()).isEqualTo(50); // XP_PER_JOURNAL = 50
        ArgumentCaptor<UserStats> captor = ArgumentCaptor.forClass(UserStats.class);
        verify(userStatsRepository).save(captor.capture());
        UserStats saved = captor.getValue();
        assertThat(saved.getBadgesJson()).contains("FIRST_THOUGHT");
    }

    @Test
    void recordActivity_shouldAwardChatBadge() throws Exception {
        // Given
        stats.setTotalChats(0);
        when(userStatsRepository.findByUser(user)).thenReturn(Optional.of(stats));
        when(userStatsRepository.save(any(UserStats.class))).thenAnswer(i -> i.getArgument(0));
        when(objectMapper.writeValueAsString(any(Set.class))).thenReturn("[\"FIRST_CHAT\"]");

        // When
        gamificationService.recordActivity(user, GamificationAction.CHAT);

        // Then
        assertThat(stats.getTotalChats()).isEqualTo(1);
        assertThat(stats.getExperiencePoints()).isEqualTo(15); // XP_PER_CHAT = 15
        ArgumentCaptor<UserStats> captor = ArgumentCaptor.forClass(UserStats.class);
        verify(userStatsRepository).save(captor.capture());
        UserStats saved = captor.getValue();
        assertThat(saved.getBadgesJson()).contains("FIRST_CHAT");
    }

    @Test
    void recordActivity_shouldAwardScheduleBadge() throws Exception {
        // Given
        stats.setSchedulesGenerated(0);
        when(userStatsRepository.findByUser(user)).thenReturn(Optional.of(stats));
        when(userStatsRepository.save(any(UserStats.class))).thenAnswer(i -> i.getArgument(0));
        when(objectMapper.writeValueAsString(any(Set.class))).thenReturn("[\"TIME_LORD\"]");

        // When
        gamificationService.recordActivity(user, GamificationAction.SCHEDULE);

        // Then
        assertThat(stats.getSchedulesGenerated()).isEqualTo(1);
        assertThat(stats.getExperiencePoints()).isEqualTo(30); // XP_PER_SCHEDULE = 30
        ArgumentCaptor<UserStats> captor = ArgumentCaptor.forClass(UserStats.class);
        verify(userStatsRepository).save(captor.capture());
        UserStats saved = captor.getValue();
        assertThat(saved.getBadgesJson()).contains("TIME_LORD");
    }

    @Test
    void recordActivity_shouldAwardTaskCreateBadge() throws Exception {
        // Given
        when(userStatsRepository.findByUser(user)).thenReturn(Optional.of(stats));
        when(userStatsRepository.save(any(UserStats.class))).thenAnswer(i -> i.getArgument(0));

        // When
        gamificationService.recordActivity(user, GamificationAction.TASK_CREATE);

        // Then
        assertThat(stats.getExperiencePoints()).isEqualTo(5); // XP_PER_TASK_CREATE = 5
        verify(userStatsRepository).save(stats);
    }

    @Test
    void recordActivity_shouldAwardMilestoneCreateBadge() throws Exception {
        // Given
        when(userStatsRepository.findByUser(user)).thenReturn(Optional.of(stats));
        when(userStatsRepository.save(any(UserStats.class))).thenAnswer(i -> i.getArgument(0));
        when(objectMapper.writeValueAsString(any(Set.class))).thenReturn("[\"VISIONARY\"]");

        // When
        gamificationService.recordActivity(user, GamificationAction.MILESTONE_CREATE);

        // Then
        assertThat(stats.getExperiencePoints()).isEqualTo(10); // XP_PER_MILESTONE_CREATE = 10
        ArgumentCaptor<UserStats> captor = ArgumentCaptor.forClass(UserStats.class);
        verify(userStatsRepository).save(captor.capture());
        UserStats saved = captor.getValue();
        assertThat(saved.getBadgesJson()).contains("VISIONARY");
    }

    @Test
    void recordActivity_shouldAwardRoadmapGenerateBadge() throws Exception {
        // Given
        when(userStatsRepository.findByUser(user)).thenReturn(Optional.of(stats));
        when(userStatsRepository.save(any(UserStats.class))).thenAnswer(i -> i.getArgument(0));
        when(objectMapper.writeValueAsString(any(Set.class))).thenReturn("[\"ARCHITECT\"]");

        // When
        gamificationService.recordActivity(user, GamificationAction.ROADMAP_GENERATE);

        // Then
        assertThat(stats.getExperiencePoints()).isEqualTo(30); // XP_PER_ROADMAP = 30
        ArgumentCaptor<UserStats> captor = ArgumentCaptor.forClass(UserStats.class);
        verify(userStatsRepository).save(captor.capture());
        UserStats saved = captor.getValue();
        assertThat(saved.getBadgesJson()).contains("ARCHITECT");
    }

    @Test
    void recordActivity_shouldAwardAIInsightBadge() throws Exception {
        // Given
        when(userStatsRepository.findByUser(user)).thenReturn(Optional.of(stats));
        when(userStatsRepository.save(any(UserStats.class))).thenAnswer(i -> i.getArgument(0));

        // When
        gamificationService.recordActivity(user, GamificationAction.AI_INSIGHT);

        // Then
        assertThat(stats.getExperiencePoints()).isEqualTo(20); // XP_PER_INSIGHT = 20
        verify(userStatsRepository).save(stats);
    }

    @Test
    void recordActivity_shouldAwardElaborateBadge() throws Exception {
        // Given
        when(userStatsRepository.findByUser(user)).thenReturn(Optional.of(stats));
        when(userStatsRepository.save(any(UserStats.class))).thenAnswer(i -> i.getArgument(0));
        when(objectMapper.writeValueAsString(any(Set.class))).thenReturn("[\"DEEP_DIVER\"]");

        // When
        gamificationService.recordActivity(user, GamificationAction.ELABORATE_TASK);

        // Then
        assertThat(stats.getExperiencePoints()).isEqualTo(5); // XP_PER_ELABORATE = 5
        ArgumentCaptor<UserStats> captor = ArgumentCaptor.forClass(UserStats.class);
        verify(userStatsRepository).save(captor.capture());
        UserStats saved = captor.getValue();
        assertThat(saved.getBadgesJson()).contains("DEEP_DIVER");
    }

    @Test
    void recordActivity_shouldCalculateLevelCorrectly() {
        // Given
        stats.setExperiencePoints(1500); // level = 1 + 1500/500 = 4
        when(userStatsRepository.findByUser(user)).thenReturn(Optional.of(stats));
        when(userStatsRepository.save(any(UserStats.class))).thenAnswer(i -> i.getArgument(0));

        // When
        gamificationService.recordActivity(user, GamificationAction.TASK);

        // Then
        assertThat(stats.getLevel()).isEqualTo(4);
        verify(userStatsRepository).save(stats);
    }

    @Test
    void recordActivity_shouldHandleBadgesJsonParsingFailure() {
        // Given
        stats.setBadgesJson("invalid json");
        when(userStatsRepository.findByUser(user)).thenReturn(Optional.of(stats));
        when(userStatsRepository.save(any(UserStats.class))).thenAnswer(i -> i.getArgument(0));

        // When
        gamificationService.recordActivity(user, GamificationAction.TASK);

        // Then – should not throw, and badges should be reset to empty
        // We can't verify internal set directly, but we can check that save is called and badgesJson is updated to a valid JSON array.
        ArgumentCaptor<UserStats> captor = ArgumentCaptor.forClass(UserStats.class);
        verify(userStatsRepository).save(captor.capture());
        UserStats saved = captor.getValue();
        assertThat(saved.getBadgesJson()).startsWith("[");
        assertThat(saved.getBadgesJson()).endsWith("]");
    }

    // ========== getUserStats Tests ==========

    @Test
    void getUserStats_shouldReturnStats_whenExists() throws Exception {
        // Given
        stats.setCurrentStreak(5);
        stats.setLongestStreak(7);
        stats.setLastActiveDate(LocalDate.now());
        stats.setBadgesJson("[\"FIRST_STEP\"]");
        stats.setTotalTasksCompleted(10);
        stats.setExperiencePoints(500);
        stats.setLevel(2);
        stats.setTotalJournalEntries(3);
        stats.setTotalChats(4);
        stats.setSchedulesGenerated(1);

        when(userStatsRepository.findByUser(user)).thenReturn(Optional.of(stats));
        when(objectMapper.readValue(eq("[\"FIRST_STEP\"]"), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(new HashSet<>(Set.of("FIRST_STEP")));

        // When
        UserStatsResponse response = gamificationService.getUserStats(user);

        // Then
        assertThat(response.currentStreak()).isEqualTo(5);
        assertThat(response.longestStreak()).isEqualTo(7);
        assertThat(response.lastActiveDate()).isEqualTo(LocalDate.now());
        assertThat(response.badges()).containsExactly("FIRST_STEP");
        assertThat(response.totalTasksCompleted()).isEqualTo(10);
        assertThat(response.experiencePoints()).isEqualTo(500);
        assertThat(response.level()).isEqualTo(2);
        assertThat(response.totalJournalEntries()).isEqualTo(3);
        assertThat(response.totalChats()).isEqualTo(4);
        assertThat(response.schedulesGenerated()).isEqualTo(1);
    }

    @Test
    void getUserStats_shouldInitializeStats_whenNotFound() {
        // Given
        when(userStatsRepository.findByUser(user)).thenReturn(Optional.empty());
        when(userStatsRepository.save(any(UserStats.class))).thenAnswer(i -> i.getArgument(0));

        // When
        UserStatsResponse response = gamificationService.getUserStats(user);

        // Then
        assertThat(response.currentStreak()).isEqualTo(0);
        assertThat(response.longestStreak()).isEqualTo(0);
        assertThat(response.lastActiveDate()).isNull();
        assertThat(response.badges()).isEmpty();
        assertThat(response.totalTasksCompleted()).isEqualTo(0);
        assertThat(response.experiencePoints()).isEqualTo(0);
        assertThat(response.level()).isEqualTo(1);
        assertThat(response.totalJournalEntries()).isEqualTo(0);
        assertThat(response.totalChats()).isEqualTo(0);
        assertThat(response.schedulesGenerated()).isEqualTo(0);
        verify(userStatsRepository).save(any(UserStats.class));
    }

    // ========== awardRoadmapCompletedBadge Tests ==========

    @Test
    void awardRoadmapCompletedBadge_shouldAddBadge_whenNotPresent() throws Exception {
        // Given
        stats.setBadgesJson("[]");
        when(userStatsRepository.findByUser(user)).thenReturn(Optional.of(stats));
        when(userStatsRepository.save(any(UserStats.class))).thenAnswer(i -> i.getArgument(0));
        when(objectMapper.writeValueAsString(any(Set.class))).thenReturn("[\"ROADMAP_FINISHER\"]");

        // When
        gamificationService.awardRoadmapCompletedBadge(user);

        // Then
        ArgumentCaptor<UserStats> captor = ArgumentCaptor.forClass(UserStats.class);
        verify(userStatsRepository).save(captor.capture());
        UserStats saved = captor.getValue();
        assertThat(saved.getBadgesJson()).contains("ROADMAP_FINISHER");
    }

    @Test
    void awardRoadmapCompletedBadge_shouldNotDuplicate_whenAlreadyPresent() throws Exception {
        // Given
        stats.setBadgesJson("[\"ROADMAP_FINISHER\"]");
        when(userStatsRepository.findByUser(user)).thenReturn(Optional.of(stats));

        // 🔥 FIX: Override the default mock to return a Set that actually contains the badge!
        when(objectMapper.readValue(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(new HashSet<>(Set.of("ROADMAP_FINISHER")));

        // When
        gamificationService.awardRoadmapCompletedBadge(user);

        // Then – badges.add() returns false, so the save block is skipped
        verify(userStatsRepository, never()).save(any(UserStats.class));
    }

    @Test
    void awardRoadmapCompletedBadge_shouldInitializeStats_whenNotFound() throws Exception {
        // Given
        when(userStatsRepository.findByUser(user)).thenReturn(Optional.empty());
        when(userStatsRepository.save(any(UserStats.class))).thenAnswer(i -> i.getArgument(0));
        when(objectMapper.writeValueAsString(any(Set.class))).thenReturn("[\"ROADMAP_FINISHER\"]");

        // When
        gamificationService.awardRoadmapCompletedBadge(user);

        // Then – should initialize and add badge
        verify(userStatsRepository, atLeastOnce()).save(any(UserStats.class));
    }

    // ========== Additional Coverage Tests ==========

    @Test
    void recordActivity_shouldAwardAI_ReflectionBadge() throws Exception {
        when(userStatsRepository.findByUser(user)).thenReturn(Optional.of(stats));
        when(userStatsRepository.save(any(UserStats.class))).thenAnswer(i -> i.getArgument(0));
        when(objectMapper.writeValueAsString(any(Set.class))).thenReturn("[\"INTROSPECTIVE\"]");

        gamificationService.recordActivity(user, GamificationAction.AI_REFLECTION);

        assertThat(stats.getExperiencePoints()).isEqualTo(15); // XP_PER_REFLECTION
        ArgumentCaptor<UserStats> captor = ArgumentCaptor.forClass(UserStats.class);
        verify(userStatsRepository).save(captor.capture());
        UserStats saved = captor.getValue();
        assertThat(saved.getBadgesJson()).contains("INTROSPECTIVE");
    }

    @Test
    void recordActivity_shouldHandleJsonProcessingException_whenWritingBadges() throws Exception {
        // Given – badgesChanged will be true (FIRST_STEP badge awarded)
        stats.setTotalTasksCompleted(0);
        when(userStatsRepository.findByUser(user)).thenReturn(Optional.of(stats));
        when(userStatsRepository.save(any(UserStats.class))).thenAnswer(i -> i.getArgument(0));
        // Simulate JSON processing failure when writing badges
        when(objectMapper.writeValueAsString(any(Set.class))).thenThrow(new JsonProcessingException("Mock exception") {});

        // When
        gamificationService.recordActivity(user, GamificationAction.TASK);

        // Then – should not throw; badgesJson should remain unchanged (or be set to previous value)
        // The catch block logs the error and does NOT update badgesJson.
        // Since badgesChanged is true, it tries to write, fails, and silently continues.
        // We can verify that badgesJson is still "[]" (the initial value) and save is still called.
        ArgumentCaptor<UserStats> captor = ArgumentCaptor.forClass(UserStats.class);
        verify(userStatsRepository).save(captor.capture());
        UserStats saved = captor.getValue();
        assertThat(saved.getBadgesJson()).isEqualTo("[]"); // not updated due to error
    }

    @Test
    void getUserStats_shouldHandleJsonParsingFailure_whenReadingBadges() throws Exception {
        // Given – stats have invalid badges JSON
        stats.setBadgesJson("invalid json");
        when(userStatsRepository.findByUser(user)).thenReturn(Optional.of(stats));
        // Simulate read failure – return null to trigger the catch block
        when(objectMapper.readValue(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenThrow(new RuntimeException("Mock parsing failure"));

        // When
        UserStatsResponse response = gamificationService.getUserStats(user);

        // Then – should return empty badges set (the fallback)
        assertThat(response.badges()).isEmpty();
    }

    @Test
    void awardRoadmapCompletedBadge_shouldHandleJsonProcessingException_whenWritingBadges() throws Exception {
        // Given – stats without ROADMAP_FINISHER badge
        stats.setBadgesJson("[]");
        when(userStatsRepository.findByUser(user)).thenReturn(Optional.of(stats));
        // Simulate JSON processing failure
        when(objectMapper.writeValueAsString(any(Set.class))).thenThrow(new JsonProcessingException("Mock exception") {});

        // When
        gamificationService.awardRoadmapCompletedBadge(user);

        // Then – should NOT save (because exception prevented it)
        verify(userStatsRepository, never()).save(any(UserStats.class));
        // And badgesJson should remain unchanged (still "[]")
        assertThat(stats.getBadgesJson()).isEqualTo("[]");
    }

    @Test
    void recordActivity_shouldInitializeStats_whenNotFound() {
        // 🔥 Force empty to trigger the .orElseGet(() -> initializeStats(user)) lambda
        when(userStatsRepository.findByUser(user)).thenReturn(Optional.empty());

        // Mock save to return the argument so the method doesn't crash
        when(userStatsRepository.save(any(UserStats.class))).thenAnswer(i -> i.getArgument(0));

        gamificationService.recordActivity(user, GamificationAction.TASK);

        // Verify save was called (it will actually be called twice: once in initializeStats, once at the end)
        verify(userStatsRepository, atLeast(1)).save(any(UserStats.class));
    }

    @Test
    void getBadgesSet_shouldHandleNullAndBlankJson() {
        // 1. Test Null JSON (covers != null branch)
        stats.setBadgesJson(null);
        when(userStatsRepository.findByUser(user)).thenReturn(Optional.of(stats));
        UserStatsResponse response1 = gamificationService.getUserStats(user);
        assertThat(response1.badges()).isEmpty();

        // 2. Test Blank JSON (covers !isBlank() branch)
        stats.setBadgesJson("   ");
        UserStatsResponse response2 = gamificationService.getUserStats(user);
        assertThat(response2.badges()).isEmpty();
    }


    @Test
    void recordActivity_shouldGrantHighTierBadges_whenThresholdsCrossed() throws Exception {
        LocalDate today = LocalDate.now();
        stats.setLastActiveDate(today.minusDays(1)); // Normal consecutive day
        stats.setCurrentStreak(29); // Will become 30 to grant THIRTY_DAY_LEGEND
        stats.setLongestStreak(50); // Keeps longest streak safe from updating

        // Set counters exactly to 1 below the highest thresholds
        stats.setTotalTasksCompleted(49); // Will become 50
        stats.setTotalJournalEntries(4);  // Will become 5
        stats.setTotalChats(19);          // Will become 20

        stats.setBadgesJson("[]");

        when(userStatsRepository.findByUser(user)).thenReturn(Optional.of(stats));
        when(userStatsRepository.save(any(UserStats.class))).thenAnswer(i -> i.getArgument(0));

        // Use a real set for tracking what gets added
        Set<String> dynamicBadges = new HashSet<>();
        when(objectMapper.readValue(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(dynamicBadges);
        when(objectMapper.writeValueAsString(any())).thenReturn("[\"MOCK_JSON\"]");

        // Execute actions to push counters over the edge
        gamificationService.recordActivity(user, GamificationAction.TASK);
        gamificationService.recordActivity(user, GamificationAction.JOURNAL);
        gamificationService.recordActivity(user, GamificationAction.CHAT);

        // Verify the set received all the high-tier badges
        assertThat(dynamicBadges).contains(
                "PRODUCTIVITY_NINJA", // Task 50
                "REFLECTIVE_SOUL",    // Journal 5
                "AI_WHISPERER",       // Chat 20
                "THIRTY_DAY_LEGEND"   // Streak 30
        );

        // Also verify the mid-tier thresholds
        stats.setTotalTasksCompleted(9); // Push to 10
        stats.setCurrentStreak(6);       // Push to 7
        gamificationService.recordActivity(user, GamificationAction.TASK);
        assertThat(dynamicBadges).contains("TASK_MASTER", "SEVEN_DAY_STREAK");
    }

    @Test
    void recordActivity_shouldHandleSameDayActivity_andPreventDuplicateBadges() throws Exception {
        LocalDate today = LocalDate.now();

        // 1. Cover Line 74 (same day, so streak does NOT increment)
        stats.setLastActiveDate(today);
        stats.setCurrentStreak(35);

        // 2. Cover Line 82 (current <= longest, so longest does NOT update)
        stats.setLongestStreak(50);

        // 3. Set counters high enough to trigger all the >= thresholds
        stats.setTotalTasksCompleted(55);
        stats.setTotalJournalEntries(10);
        stats.setSchedulesGenerated(5);
        stats.setTotalChats(25);

        // 4. Pre-fill the badges JSON so badges.add() ALWAYS returns false
        String allBadges = "[\"FIRST_STEP\", \"TASK_MASTER\", \"PRODUCTIVITY_NINJA\", " +
                "\"FIRST_THOUGHT\", \"REFLECTIVE_SOUL\", " +
                "\"TIME_LORD\", \"FIRST_CHAT\", \"AI_WHISPERER\", " +
                "\"VISIONARY\", \"ARCHITECT\", \"INTROSPECTIVE\", \"DEEP_DIVER\", " +
                "\"THREE_DAY_STREAK\", \"SEVEN_DAY_STREAK\", \"THIRTY_DAY_LEGEND\"]";
        stats.setBadgesJson(allBadges);

        when(userStatsRepository.findByUser(user)).thenReturn(Optional.of(stats));
        when(userStatsRepository.save(any(UserStats.class))).thenAnswer(i -> i.getArgument(0));

        // Mock the object mapper to return the fully populated set
        Set<String> populatedBadges = new HashSet<>(Set.of(
                "FIRST_STEP", "TASK_MASTER", "PRODUCTIVITY_NINJA",
                "FIRST_THOUGHT", "REFLECTIVE_SOUL", "TIME_LORD", "FIRST_CHAT", "AI_WHISPERER",
                "VISIONARY", "ARCHITECT", "INTROSPECTIVE", "DEEP_DIVER",
                "THREE_DAY_STREAK", "SEVEN_DAY_STREAK", "THIRTY_DAY_LEGEND"
        ));
        when(objectMapper.readValue(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(populatedBadges);

        // Execute every possible action to run through all switch cases with pre-existing badges
        gamificationService.recordActivity(user, GamificationAction.TASK);
        gamificationService.recordActivity(user, GamificationAction.JOURNAL);
        gamificationService.recordActivity(user, GamificationAction.SCHEDULE);
        gamificationService.recordActivity(user, GamificationAction.CHAT);
        gamificationService.recordActivity(user, GamificationAction.MILESTONE_CREATE);
        gamificationService.recordActivity(user, GamificationAction.ROADMAP_GENERATE);
        gamificationService.recordActivity(user, GamificationAction.AI_REFLECTION);
        gamificationService.recordActivity(user, GamificationAction.ELABORATE_TASK);

        // Verify Streak didn't increment because it was the same day (Line 74)
        assertThat(stats.getCurrentStreak()).isEqualTo(35);
        // Verify Longest Streak didn't update because 35 < 50 (Line 82)
        assertThat(stats.getLongestStreak()).isEqualTo(50);

        // Since NO new badges were added, writeValueAsString should NEVER be called!
        verify(objectMapper, never()).writeValueAsString(any());
    }
}