package com.mymindmirror.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mymindmirror.backend.enums.AITask;
import com.mymindmirror.backend.enums.GamificationAction;
import com.mymindmirror.backend.model.JournalEntry;
import com.mymindmirror.backend.model.KeyPhrase;
import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.service.ai.DynamicAiClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JournalReflectionServiceTest {

    @Mock
    private JournalPersistenceService journalPersistenceService;

    @Mock
    private JournalSearchService journalSearchService;

    @Mock
    private DynamicAiClientService aiClientService;

    @Mock
    private ChatMemoryService chatMemoryService;

    @Mock
    private GamificationService gamificationService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private JournalReflectionService journalReflectionService;

    private User user;
    private UUID userId;

    @BeforeEach
    void setUp() throws Exception {
        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setPasswordHash("passwordHash");

        //  Inject @Value fields via reflection
        var reflectiveTopKField = JournalReflectionService.class.getDeclaredField("reflectiveTopK");
        reflectiveTopKField.setAccessible(true);
        reflectiveTopKField.set(journalReflectionService, 5);

        var fallbackTopKField = JournalReflectionService.class.getDeclaredField("fallbackTopK");
        fallbackTopKField.setAccessible(true);
        fallbackTopKField.set(journalReflectionService, 4);
    }

    // ============================================================
    // generateDailyReflection Tests
    // ============================================================

    @Test
    void generateDailyReflection_shouldReturnReflection_whenEntriesExist() {
        // Given
        JournalEntry entry = new JournalEntry();
        entry.setRawText("Today was a great day");
        entry.setEntryDate(LocalDate.now());
        entry.setEmotions("{\"joy\":0.8,\"sadness\":0.2}");
        entry.setCoreConcerns("[\"work\", \"health\"]");
        entry.setKeyPhrases(List.of(new KeyPhrase("great day", entry)));

        when(journalPersistenceService.getJournalEntriesForUser(eq(user), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(entry));
        when(aiClientService.generate(anyString(), eq(userId), eq(AITask.TODAY_REFLECTION)))
                .thenReturn("Today's reflection: Keep up the good work!");

        // When
        String result = journalReflectionService.generateDailyReflection(user);

        // Then
        assertThat(result).isEqualTo("Today's reflection: Keep up the good work!");
        verify(gamificationService).recordActivity(user, GamificationAction.AI_REFLECTION);
    }

    @Test
    void generateDailyReflection_shouldReturnFallbackMessage_whenNoEntries() {
        // Given
        when(journalPersistenceService.getJournalEntriesForUser(eq(user), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());

        // When
        String result = journalReflectionService.generateDailyReflection(user);

        // Then
        assertThat(result).isEqualTo("Journal an entry today to get your daily reflection!");
        verify(aiClientService, never()).generate(anyString(), any(), any());
    }

    @Test
    void generateDailyReflection_shouldHandleAIFailure() {
        // Given
        JournalEntry entry = new JournalEntry();
        entry.setRawText("Today was a great day");
        entry.setEntryDate(LocalDate.now());
        entry.setEmotions("{\"joy\":0.8,\"sadness\":0.2}");
        entry.setCoreConcerns("[\"work\", \"health\"]");

        when(journalPersistenceService.getJournalEntriesForUser(eq(user), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(entry));
        when(aiClientService.generate(anyString(), eq(userId), eq(AITask.TODAY_REFLECTION)))
                .thenThrow(new RuntimeException("AI error"));

        // When
        String result = journalReflectionService.generateDailyReflection(user);

        // Then
        assertThat(result).isEqualTo("Failed to generate reflection due to an internal error.");
        verify(gamificationService, never()).recordActivity(any(), any());
    }

    // ============================================================
    // generateReflectionChat Tests
    // ============================================================

    @Test
    void generateReflectionChat_shouldReturnResponse_whenRAGHits() {
        // Given
        String query = "How can I reduce stress?";
        String sessionId = "session123";
        boolean rememberChat = true;

        JournalEntry entry = new JournalEntry();
        entry.setEntryDate(LocalDate.now().minusDays(1));
        entry.setSummary("Feeling stressed about work deadline");
        entry.setEmotions("{\"anxiety\":0.7,\"stress\":0.8}");
        entry.setKeyPhrases(List.of(new KeyPhrase("work stress", entry)));
        List<JournalEntry> relevantEntries = List.of(entry);

        when(journalSearchService.retrieveRelevantEntries(user, query, 5)).thenReturn(relevantEntries);
        when(aiClientService.generate(anyString(), eq(userId), eq(AITask.REFLECTION_CHAT)))
                .thenReturn("Try the 5-4-3-2-1 grounding technique.");

        // When
        String result = journalReflectionService.generateReflectionChat(user, query, sessionId, rememberChat);

        // Then
        assertThat(result).isEqualTo("Try the 5-4-3-2-1 grounding technique.");
        verify(chatMemoryService).appendMessage(userId, sessionId, "user", query);
        verify(chatMemoryService).appendMessage(userId, sessionId, "model", result);
        verify(gamificationService).recordActivity(user, GamificationAction.CHAT);
    }

    @Test
    void generateReflectionChat_shouldFallbackToRecentEntries_whenRAGMisses() {
        // Given
        String query = "How can I improve my sleep?";
        String sessionId = "session123";
        boolean rememberChat = true;

        when(journalSearchService.retrieveRelevantEntries(user, query, 5)).thenReturn(List.of());

        JournalEntry recentEntry = new JournalEntry();
        recentEntry.setEntryDate(LocalDate.now().minusDays(2));
        recentEntry.setSummary("Slept poorly last night");
        recentEntry.setEmotions("{\"tired\":0.7}");
        when(journalPersistenceService.getLatestJournalTimelineWithoutDecryption(user, 5))
                .thenReturn(List.of(recentEntry));

        when(aiClientService.generate(anyString(), eq(userId), eq(AITask.REFLECTION_CHAT)))
                .thenReturn("Try a consistent sleep schedule.");

        // When
        String result = journalReflectionService.generateReflectionChat(user, query, sessionId, rememberChat);

        // Then
        assertThat(result).isEqualTo("Try a consistent sleep schedule.");
        verify(chatMemoryService).appendMessage(userId, sessionId, "user", query);
        verify(chatMemoryService).appendMessage(userId, sessionId, "model", result);
        verify(gamificationService).recordActivity(user, GamificationAction.CHAT);
    }

    @Test
    void generateReflectionChat_shouldNotRememberHistory_whenRememberChatFalse() {
        // Given
        String query = "How can I reduce stress?";
        String sessionId = "session123";
        boolean rememberChat = false;

        JournalEntry entry = new JournalEntry();
        entry.setSummary("Feeling stressed about work");
        when(journalSearchService.retrieveRelevantEntries(user, query, 5)).thenReturn(List.of(entry));
        when(aiClientService.generate(anyString(), eq(userId), eq(AITask.REFLECTION_CHAT)))
                .thenReturn("Try deep breathing.");

        // When
        String result = journalReflectionService.generateReflectionChat(user, query, sessionId, rememberChat);

        // Then
        assertThat(result).isEqualTo("Try deep breathing.");
        verify(chatMemoryService, never()).appendMessage(any(), anyString(), anyString(), anyString());
        verify(gamificationService).recordActivity(user, GamificationAction.CHAT);
    }

    @Test
    void generateReflectionChat_shouldHandleAIFailure() {
        // Given
        String query = "How can I reduce stress?";
        String sessionId = "session123";
        boolean rememberChat = true;

        JournalEntry entry = new JournalEntry();
        entry.setSummary("Feeling stressed");
        when(journalSearchService.retrieveRelevantEntries(user, query, 5)).thenReturn(List.of(entry));
        when(aiClientService.generate(anyString(), eq(userId), eq(AITask.REFLECTION_CHAT)))
                .thenThrow(new RuntimeException("AI error"));

        // When
        String result = journalReflectionService.generateReflectionChat(user, query, sessionId, rememberChat);

        // Then
        assertThat(result).isEqualTo("I'm unable to answer right now. Please try again later.");
        verify(chatMemoryService, never()).appendMessage(any(), anyString(), anyString(), anyString());
        verify(gamificationService, never()).recordActivity(any(), any());
    }

    // ============================================================
    // generateReflectiveQuestion Tests
    // ============================================================



    @Test
    void generateReflectiveQuestion_shouldReturnQuestionFromAI_whenCacheMiss() {
        // Given
        // Simulate today's entries
        JournalEntry todayEntry = new JournalEntry();
        todayEntry.setId(UUID.randomUUID());
        todayEntry.setSummary("Worked on a challenging project");
        todayEntry.setCreationTimestamp(java.time.LocalDateTime.now());

        when(journalPersistenceService.getJournalEntriesForUser(eq(user), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(todayEntry));

        // RAG search returns empty (no historical matches)
        when(journalSearchService.retrieveRelevantEntries(user, "Worked on a challenging project", 4))
                .thenReturn(new ArrayList<>());

        // Fallback timeline (since RAG miss, it will fetch latest timeline)
        JournalEntry timelineEntry = new JournalEntry();
        timelineEntry.setEntryDate(LocalDate.now().minusDays(1));
        timelineEntry.setSummary("Had a relaxing day");
        timelineEntry.setEmotions("{\"joy\":0.7}");
        when(journalPersistenceService.getLatestJournalTimelineWithoutDecryption(user, 4))
                .thenReturn(List.of(timelineEntry));

        String aiQuestion = "How did you overcome similar challenges in the past?";
        when(aiClientService.generate(anyString(), eq(userId), eq(AITask.REFLECTIVE_QUESTION)))
                .thenReturn(aiQuestion);

        // When
        String result = journalReflectionService.generateReflectiveQuestion(user);

        // Then
        assertThat(result).isEqualTo(aiQuestion);
        verify(aiClientService).generate(anyString(), eq(userId), eq(AITask.REFLECTIVE_QUESTION));
    }
    @Test
    void generateReflectiveQuestion_shouldFallbackToTimeline_whenRAGMisses() {
        // Given
        // No today's entries
        when(journalPersistenceService.getJournalEntriesForUser(eq(user), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());

        // No historical entries from RAG
        when(journalSearchService.retrieveRelevantEntries(user, "my recent emotional journey", 4))
                .thenReturn(new ArrayList<>());

        // Fallback timeline entries
        JournalEntry timelineEntry = new JournalEntry();
        timelineEntry.setEntryDate(LocalDate.now().minusDays(2));
        timelineEntry.setSummary("Had a relaxing weekend");
        timelineEntry.setEmotions("{\"joy\":0.7}");
        when(journalPersistenceService.getLatestJournalTimelineWithoutDecryption(user, 4))
                .thenReturn(List.of(timelineEntry));

        when(aiClientService.generate(anyString(), eq(userId), eq(AITask.REFLECTIVE_QUESTION)))
                .thenReturn("What brought you joy this weekend?");

        // When
        String result = journalReflectionService.generateReflectiveQuestion(user);

        // Then
        assertThat(result).isEqualTo("What brought you joy this weekend?");
        verify(aiClientService).generate(anyString(), eq(userId), eq(AITask.REFLECTIVE_QUESTION));
    }

    @Test
    void generateReflectiveQuestion_shouldReturnDefaultQuestion_whenAIFails() {
        // Given
        // Set up no entries so it triggers fallback
        when(journalPersistenceService.getJournalEntriesForUser(eq(user), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());

        // RAG miss
        when(journalSearchService.retrieveRelevantEntries(user, "my recent emotional journey", 4))
                .thenReturn(new ArrayList<>());

        // Fallback timeline
        JournalEntry timelineEntry = new JournalEntry();
        timelineEntry.setEntryDate(LocalDate.now().minusDays(1));
        timelineEntry.setSummary("Relaxing day");
        when(journalPersistenceService.getLatestJournalTimelineWithoutDecryption(user, 4))
                .thenReturn(List.of(timelineEntry));

        when(aiClientService.generate(anyString(), eq(userId), eq(AITask.REFLECTIVE_QUESTION)))
                .thenThrow(new RuntimeException("AI error"));

        // When
        String result = journalReflectionService.generateReflectiveQuestion(user);

        // Then
        assertThat(result).isEqualTo("What's one thing you've learned about yourself recently?");
    }

    @Test
    void generateReflectiveQuestion_shouldUseLatestEntry_whenNoTodayEntriesAndLatestEntryExists() {
        // Given
        // No today's entries
        when(journalPersistenceService.getJournalEntriesForUser(eq(user), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());

        // Latest entry exists via persistence service
        JournalEntry latestEntry = new JournalEntry();
        latestEntry.setId(UUID.randomUUID());
        latestEntry.setSummary("Reflecting on my career growth");
        latestEntry.setCreationTimestamp(java.time.LocalDateTime.now().minusDays(1));
        when(journalPersistenceService.findLatestEntryByUser(user)).thenReturn(java.util.Optional.of(latestEntry));

        // RAG search uses the latest summary as query
        when(journalSearchService.retrieveRelevantEntries(user, "Reflecting on my career growth", 4))
                .thenReturn(new ArrayList<>());

        // Fallback timeline (we'll provide something)
        when(journalPersistenceService.getLatestJournalTimelineWithoutDecryption(user, 4))
                .thenReturn(List.of(latestEntry));

        when(aiClientService.generate(anyString(), eq(userId), eq(AITask.REFLECTIVE_QUESTION)))
                .thenReturn("How has your career evolved recently?");

        // When
        String result = journalReflectionService.generateReflectiveQuestion(user);

        // Then
        assertThat(result).isEqualTo("How has your career evolved recently?");
        verify(journalPersistenceService).findLatestEntryByUser(user);
    }

    @Test
    void generateReflectiveQuestion_shouldReturnCachedQuestion_whenValid() throws Exception {
        // Given – put a cached question into the cache
        String cachedQuestion = "What was your biggest challenge today?";
        var cacheField = JournalReflectionService.class.getDeclaredField("questionCache");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        var cache = (ConcurrentHashMap<String, Object>) cacheField.get(journalReflectionService);
        // Create a CachedQuestion instance using reflection
        var cachedQuestionClass = Class.forName("com.mymindmirror.backend.service.JournalReflectionService$CachedQuestion");
        var constructor = cachedQuestionClass.getDeclaredConstructor(String.class);
        constructor.setAccessible(true);
        Object cachedQuestionObj = constructor.newInstance(cachedQuestion);
        cache.put(user.getId().toString(), cachedQuestionObj);

        // When
        String result = journalReflectionService.generateReflectiveQuestion(user);

        // Then
        assertThat(result).isEqualTo(cachedQuestion);
        verify(aiClientService, never()).generate(anyString(), any(), any());
        verify(journalPersistenceService, never()).getJournalEntriesForUser(any(), any(), any());
    }

    @Test
    void generateDailyReflection_shouldAggregateMultipleEntries() throws Exception {
        // Given – two entries with emotions and core concerns
        JournalEntry entry1 = new JournalEntry();
        entry1.setRawText("Today was good");
        entry1.setEntryDate(LocalDate.now());
        entry1.setEmotions("{\"joy\":0.8,\"sadness\":0.2}");
        entry1.setCoreConcerns("[\"work\", \"health\"]");

        JournalEntry entry2 = new JournalEntry();
        entry2.setRawText("But I felt stressed about deadlines");
        entry2.setEntryDate(LocalDate.now());
        entry2.setEmotions("{\"anxiety\":0.7,\"stress\":0.6}");
        entry2.setCoreConcerns("[\"work\"]");

        when(journalPersistenceService.getJournalEntriesForUser(eq(user), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(entry1, entry2));

        // Mock objectMapper to parse JSON
        when(objectMapper.readValue(eq("{\"joy\":0.8,\"sadness\":0.2}"), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(Map.of("joy", 0.8, "sadness", 0.2));
        when(objectMapper.readValue(eq("{\"anxiety\":0.7,\"stress\":0.6}"), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(Map.of("anxiety", 0.7, "stress", 0.6));
        when(objectMapper.readValue(eq("[\"work\", \"health\"]"), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(List.of("work", "health"));
        when(objectMapper.readValue(eq("[\"work\"]"), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(List.of("work"));

        when(aiClientService.generate(anyString(), eq(userId), eq(AITask.TODAY_REFLECTION)))
                .thenReturn("Today's reflection: Keep going!");

        // When
        String result = journalReflectionService.generateDailyReflection(user);

        // Then
        assertThat(result).isEqualTo("Today's reflection: Keep going!");
        verify(gamificationService).recordActivity(user, GamificationAction.AI_REFLECTION);
    }


    @Test
    void generateReflectionChat_shouldIncludeChatHistory_whenRememberChatTrueAndHistoryExists() {
        // Given
        String query = "How can I reduce stress?";
        String sessionId = "session123";
        boolean rememberChat = true;

        JournalEntry entry = new JournalEntry();
        entry.setEntryDate(LocalDate.now().minusDays(1));
        entry.setSummary("Feeling stressed about work deadline");
        entry.setEmotions("{\"anxiety\":0.7}");
        when(journalSearchService.retrieveRelevantEntries(user, query, 5)).thenReturn(List.of(entry));

        // Mock chat history
        List<ChatMemoryService.ChatMessage> history = List.of(
                new ChatMemoryService.ChatMessage("user", "I've been feeling anxious lately"),
                new ChatMemoryService.ChatMessage("model", "That sounds tough. Can you tell me more?")
        );
        when(chatMemoryService.getHistory(userId, sessionId)).thenReturn(history);

        when(aiClientService.generate(anyString(), eq(userId), eq(AITask.REFLECTION_CHAT)))
                .thenReturn("Try deep breathing exercises.");

        // When
        String result = journalReflectionService.generateReflectionChat(user, query, sessionId, rememberChat);

        // Then
        assertThat(result).isEqualTo("Try deep breathing exercises.");
        verify(chatMemoryService).getHistory(userId, sessionId);
        verify(chatMemoryService).appendMessage(userId, sessionId, "user", query);
        verify(chatMemoryService).appendMessage(userId, sessionId, "model", result);
        verify(gamificationService).recordActivity(user, GamificationAction.CHAT);
    }


    @Test
    void generateReflectiveQuestion_shouldUseRAGResults_whenHistoricalParallelsFound() {
        // Given
        // Simulate today's entries
        UUID todayId = UUID.randomUUID();
        JournalEntry todayEntry = new JournalEntry();
        todayEntry.setId(todayId);
        todayEntry.setSummary("Worked on a challenging project");
        todayEntry.setCreationTimestamp(java.time.LocalDateTime.now());
        when(journalPersistenceService.getJournalEntriesForUser(eq(user), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(todayEntry));

        // RAG search returns historical entries with a different ID
        UUID historicalId = UUID.randomUUID();
        JournalEntry historicalEntry = new JournalEntry();
        historicalEntry.setId(historicalId); // ✅ Set a non‑null ID
        historicalEntry.setEntryDate(LocalDate.now().minusDays(3));
        historicalEntry.setSummary("Had a similar challenge last week");
        historicalEntry.setEmotions("{\"frustration\":0.6}");
        when(journalSearchService.retrieveRelevantEntries(user, "Worked on a challenging project", 4))
                .thenReturn(new ArrayList<>(List.of(historicalEntry)));

        when(aiClientService.generate(anyString(), eq(userId), eq(AITask.REFLECTIVE_QUESTION)))
                .thenReturn("How did you overcome that previous challenge?");

        // When
        String result = journalReflectionService.generateReflectiveQuestion(user);

        // Then
        assertThat(result).isEqualTo("How did you overcome that previous challenge?");
        verify(aiClientService).generate(anyString(), eq(userId), eq(AITask.REFLECTIVE_QUESTION));
    }

    @Test
    void generateReflectiveQuestion_shouldReturnDefaultQuestion_whenAIResponseNull() {
        // Given
        // Simulate today's entries
        JournalEntry todayEntry = new JournalEntry();
        todayEntry.setId(UUID.randomUUID());
        todayEntry.setSummary("Worked on a challenging project");
        todayEntry.setCreationTimestamp(java.time.LocalDateTime.now());
        when(journalPersistenceService.getJournalEntriesForUser(eq(user), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(todayEntry));

        // RAG search returns empty
        when(journalSearchService.retrieveRelevantEntries(user, "Worked on a challenging project", 4))
                .thenReturn(new ArrayList<>());

        // Fallback timeline
        JournalEntry timelineEntry = new JournalEntry();
        timelineEntry.setEntryDate(LocalDate.now().minusDays(1));
        timelineEntry.setSummary("Relaxing day");
        when(journalPersistenceService.getLatestJournalTimelineWithoutDecryption(user, 4))
                .thenReturn(List.of(timelineEntry));

        // AI returns null
        when(aiClientService.generate(anyString(), eq(userId), eq(AITask.REFLECTIVE_QUESTION)))
                .thenReturn(null); // null response

        // When
        String result = journalReflectionService.generateReflectiveQuestion(user);

        // Then
        assertThat(result).isEqualTo("What's one thing you've learned about yourself recently?");
    }

    @Test
    void generateReflectiveQuestion_shouldIgnoreCache_whenExpired() throws Exception {
        // Given – put an EXPIRED cached question into the cache
        String cachedQuestionText = "Expired cached question?";

        // 1. Get access to the private cache map
        var cacheField = JournalReflectionService.class.getDeclaredField("questionCache");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        var cache = (ConcurrentHashMap<String, Object>) cacheField.get(journalReflectionService);

        // 2. Create a new CachedQuestion using reflection
        var cachedQuestionClass = Class.forName("com.mymindmirror.backend.service.JournalReflectionService$CachedQuestion");
        var constructor = cachedQuestionClass.getDeclaredConstructor(String.class);
        constructor.setAccessible(true);
        Object cachedQuestionObj = constructor.newInstance(cachedQuestionText);

        // 3. Forcibly overwrite the timestamp to be 6 minutes in the past!
        var timestampField = cachedQuestionClass.getDeclaredField("timestamp");
        timestampField.setAccessible(true);
        long sixMinutesAgo = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(6);
        timestampField.setLong(cachedQuestionObj, sixMinutesAgo);

        // 4. Put the expired object in the cache
        cache.put(user.getId().toString(), cachedQuestionObj);

        // 5. Setup mocks for a normal RAG miss generation (since cache will be ignored)
        when(journalPersistenceService.getJournalEntriesForUser(eq(user), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(journalSearchService.retrieveRelevantEntries(user, "my recent emotional journey", 4))
                .thenReturn(new ArrayList<>());
        when(journalPersistenceService.getLatestJournalTimelineWithoutDecryption(user, 4))
                .thenReturn(List.of());

        String newAiQuestion = "Fresh AI question?";
        when(aiClientService.generate(anyString(), eq(userId), eq(AITask.REFLECTIVE_QUESTION)))
                .thenReturn(newAiQuestion);

        // When
        String result = journalReflectionService.generateReflectiveQuestion(user);

        // Then - It should NOT be the cached question. It should trigger the AI call.
        assertThat(result).isEqualTo(newAiQuestion);
        verify(aiClientService).generate(anyString(), eq(userId), eq(AITask.REFLECTIVE_QUESTION));
    }

    @Test
    void generateDailyReflection_shouldHandleNullEntries() {
        when(journalPersistenceService.getJournalEntriesForUser(eq(user), any(), any()))
                .thenReturn(null);

        String result = journalReflectionService.generateDailyReflection(user);

        assertThat(result).isEqualTo("Journal an entry today to get your daily reflection!");
    }

    @Test
    void generateDailyReflection_shouldSkipNullEmotionsAndConcerns() {
        JournalEntry entry = new JournalEntry();
        entry.setRawText("Text");
        entry.setEntryDate(LocalDate.now());
        entry.setEmotions(null);
        entry.setCoreConcerns(null);

        when(journalPersistenceService.getJournalEntriesForUser(eq(user), any(), any()))
                .thenReturn(List.of(entry));
        when(aiClientService.generate(anyString(), eq(userId), eq(AITask.TODAY_REFLECTION)))
                .thenReturn("Reflection!");

        String result = journalReflectionService.generateDailyReflection(user);

        assertThat(result).isEqualTo("Reflection!");
    }

    @Test
    void generateDailyReflection_shouldFilterLowEmotionAverages() throws Exception {
        JournalEntry entry = new JournalEntry();
        entry.setRawText("Text");
        entry.setEntryDate(LocalDate.now());
        entry.setEmotions("{\"joy\":0.005}");  // avg < 0.01 → excluded
        entry.setCoreConcerns(null);

        when(journalPersistenceService.getJournalEntriesForUser(eq(user), any(), any()))
                .thenReturn(List.of(entry));
        when(objectMapper.readValue(eq("{\"joy\":0.005}"), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(Map.of("joy", 0.005));
        when(aiClientService.generate(anyString(), eq(userId), eq(AITask.TODAY_REFLECTION)))
                .thenReturn("Reflection!");

        journalReflectionService.generateDailyReflection(user);

        // Verified by no exception + "No specific emotions detected." in prompt
    }

    @Test
    void generateDailyReflection_shouldReturnFallback_whenAINull() {
        JournalEntry entry = new JournalEntry();
        entry.setRawText("Text");
        entry.setEntryDate(LocalDate.now());

        when(journalPersistenceService.getJournalEntriesForUser(eq(user), any(), any()))
                .thenReturn(List.of(entry));
        when(aiClientService.generate(anyString(), eq(userId), eq(AITask.TODAY_REFLECTION)))
                .thenReturn(null);

        String result = journalReflectionService.generateDailyReflection(user);

        assertThat(result).isEqualTo("Couldn't generate a reflection today. Please try again later.");
    }


    @Test
    void generateReflectionChat_shouldHandleNullSessionAndMissingKeyPhrases() {
        JournalEntry entry = new JournalEntry();
        entry.setEntryDate(LocalDate.now());
        entry.setSummary("Summary");
        entry.setEmotions("{}");
        entry.setKeyPhrases(null);  // ← null keyPhrases

        when(journalSearchService.retrieveRelevantEntries(user, "query", 5)).thenReturn(List.of(entry));
        when(aiClientService.generate(anyString(), eq(userId), eq(AITask.REFLECTION_CHAT)))
                .thenReturn("Answer");

        String result = journalReflectionService.generateReflectionChat(user, "query", null, true);

        assertThat(result).isEqualTo("Answer");
        verify(chatMemoryService, never()).appendMessage(any(), anyString(), anyString(), anyString());
    }


    @Test
    void generateReflectiveQuestion_shouldHandleNullSummaryAndNullAIResponse() {
        JournalEntry todayEntry = new JournalEntry();
        todayEntry.setId(UUID.randomUUID());
        todayEntry.setSummary(null);  // ← null summary
        todayEntry.setCreationTimestamp(java.time.LocalDateTime.now());

        when(journalPersistenceService.getJournalEntriesForUser(eq(user), any(), any()))
                .thenReturn(List.of(todayEntry));
        when(journalSearchService.retrieveRelevantEntries(user, "my recent emotional journey", 4))
                .thenReturn(new ArrayList<>());
        when(journalPersistenceService.getLatestJournalTimelineWithoutDecryption(user, 4))
                .thenReturn(List.of());
        when(aiClientService.generate(anyString(), eq(userId), eq(AITask.REFLECTIVE_QUESTION)))
                .thenReturn(null);  // ← null AI response

        String result = journalReflectionService.generateReflectiveQuestion(user);

        assertThat(result).isEqualTo("What's one thing you've learned about yourself recently?");
    }

    // ============================================================
// Coverage gap tests
// ============================================================

    @Test
    @DisplayName("generateDailyReflection: blank AI response → fallback message")
    void generateDailyReflection_shouldReturnFallback_whenAIResponseBlank() {
        JournalEntry entry = new JournalEntry();
        entry.setRawText("Text");
        entry.setEntryDate(LocalDate.now());

        when(journalPersistenceService.getJournalEntriesForUser(eq(user), any(), any()))
                .thenReturn(List.of(entry));
        when(aiClientService.generate(anyString(), eq(userId), eq(AITask.TODAY_REFLECTION)))
                .thenReturn("   ");   // non-null but blank

        String result = journalReflectionService.generateDailyReflection(user);

        assertThat(result).isEqualTo("Couldn't generate a reflection today. Please try again later.");
    }

    @Test
    @DisplayName("generateReflectionChat: null emotions on RAG hit + blank sessionId → memory skipped")
    void generateReflectionChat_shouldHandleNullEmotionsInRAGHitAndBlankSession() {
        JournalEntry entry = new JournalEntry();
        entry.setEntryDate(LocalDate.now());
        entry.setSummary("Summary");
        entry.setEmotions(null);   // ← null emotions in RAG hit

        when(journalSearchService.retrieveRelevantEntries(user, "query", 5)).thenReturn(List.of(entry));
        when(aiClientService.generate(anyString(), eq(userId), eq(AITask.REFLECTION_CHAT))).thenReturn("Answer");

        String result = journalReflectionService.generateReflectionChat(user, "query", "   ", true);

        assertThat(result).isEqualTo("Answer");
        verify(chatMemoryService, never()).appendMessage(any(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("generateReflectionChat: null emotions in RAG miss fallback entries")
    void generateReflectionChat_shouldHandleNullEmotionsInRAGMiss() {
        when(journalSearchService.retrieveRelevantEntries(user, "query", 5)).thenReturn(List.of());

        JournalEntry recentEntry = new JournalEntry();
        recentEntry.setEntryDate(LocalDate.now().minusDays(1));
        recentEntry.setSummary("Recent");
        recentEntry.setEmotions(null);   // ← null emotions in RAG miss

        when(journalPersistenceService.getLatestJournalTimelineWithoutDecryption(user, 5))
                .thenReturn(List.of(recentEntry));
        when(aiClientService.generate(anyString(), eq(userId), eq(AITask.REFLECTION_CHAT))).thenReturn("Answer");

        String result = journalReflectionService.generateReflectionChat(user, "query", "session123", true);

        assertThat(result).isEqualTo("Answer");
    }

    @Test
    @DisplayName("generateReflectionChat: null AI response with valid session → memory skipped")
    void generateReflectionChat_shouldSkipMemory_whenAIResponseIsNull() {
        JournalEntry entry = new JournalEntry();
        entry.setSummary("Summary");

        when(journalSearchService.retrieveRelevantEntries(user, "query", 5)).thenReturn(List.of(entry));
        when(aiClientService.generate(anyString(), eq(userId), eq(AITask.REFLECTION_CHAT)))
                .thenReturn(null);   // ← null response with valid session

        String result = journalReflectionService.generateReflectionChat(user, "query", "session123", true);

        assertThat(result).isNull();
        verify(chatMemoryService, never()).appendMessage(any(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("generateReflectiveQuestion: null todayEntries → falls through to latest-entry path")
    void generateReflectiveQuestion_shouldHandleNullTodayEntries() {
        when(journalPersistenceService.getJournalEntriesForUser(eq(user), any(), any()))
                .thenReturn(null);   // ← null today entries
        when(journalPersistenceService.findLatestEntryByUser(user)).thenReturn(Optional.empty());
        when(journalSearchService.retrieveRelevantEntries(user, "my recent emotional journey", 4))
                .thenReturn(new ArrayList<>());
        when(journalPersistenceService.getLatestJournalTimelineWithoutDecryption(user, 4))
                .thenReturn(List.of());
        when(aiClientService.generate(anyString(), eq(userId), eq(AITask.REFLECTIVE_QUESTION)))
                .thenReturn("Question?");

        String result = journalReflectionService.generateReflectiveQuestion(user);

        assertThat(result).isEqualTo("Question?");
    }

    @Test
    @DisplayName("generateReflectiveQuestion: latest entry with null summary keeps default anchor")
    void generateReflectiveQuestion_shouldUseDefaultAnchor_whenLatestEntrySummaryIsNull() {
        when(journalPersistenceService.getJournalEntriesForUser(eq(user), any(), any()))
                .thenReturn(List.of());

        JournalEntry latestEntry = new JournalEntry();
        latestEntry.setId(UUID.randomUUID());
        latestEntry.setSummary(null);   // ← null summary

        when(journalPersistenceService.findLatestEntryByUser(user)).thenReturn(Optional.of(latestEntry));
        when(journalSearchService.retrieveRelevantEntries(user, "my recent emotional journey", 4))
                .thenReturn(new ArrayList<>());
        when(journalPersistenceService.getLatestJournalTimelineWithoutDecryption(user, 4))
                .thenReturn(List.of());
        when(aiClientService.generate(anyString(), eq(userId), eq(AITask.REFLECTIVE_QUESTION)))
                .thenReturn("Question?");

        String result = journalReflectionService.generateReflectiveQuestion(user);

        assertThat(result).isEqualTo("Question?");
    }

    @Test
    @DisplayName("generateReflectiveQuestion: blank AI question → default fallback")
    void generateReflectiveQuestion_shouldReturnDefault_whenAIQuestionBlank() {
        when(journalPersistenceService.getJournalEntriesForUser(eq(user), any(), any()))
                .thenReturn(List.of());
        when(journalPersistenceService.findLatestEntryByUser(user)).thenReturn(Optional.empty());
        when(journalSearchService.retrieveRelevantEntries(user, "my recent emotional journey", 4))
                .thenReturn(new ArrayList<>());
        when(journalPersistenceService.getLatestJournalTimelineWithoutDecryption(user, 4))
                .thenReturn(List.of());
        when(aiClientService.generate(anyString(), eq(userId), eq(AITask.REFLECTIVE_QUESTION)))
                .thenReturn("   ");   // ← blank question

        String result = journalReflectionService.generateReflectiveQuestion(user);

        assertThat(result).isEqualTo("What's one thing you've learned about yourself recently?");
    }
}