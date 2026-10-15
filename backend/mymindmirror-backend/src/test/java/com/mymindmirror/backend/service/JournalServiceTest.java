package com.mymindmirror.backend.service;

import com.mymindmirror.backend.model.JournalEntry;
import com.mymindmirror.backend.model.KeyPhrase;
import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.payload.response.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JournalServiceTest {

    @Mock
    private JournalPersistenceService journalPersistenceService;

    @Mock
    private JournalSearchService journalSearchService;

    @Mock
    private JournalReflectionService journalReflectionService;

    @Mock
    private AnomalyDetectionService anomalyDetectionService;

    @InjectMocks
    private JournalService journalService;

    private User user;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        user.setUsername("testuser");
    }

    // ========== getTopKeyPhrases Tests (Aggregation Logic) ==========

    @Test
    void getTopKeyPhrases_shouldAggregateAndSortCorrectly() {
        // Given
        LocalDate start = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now();
        int limit = 5;

        JournalEntry entry1 = new JournalEntry();
        JournalEntry entry2 = new JournalEntry();
        KeyPhrase kp1 = new KeyPhrase("happy", null);
        KeyPhrase kp2 = new KeyPhrase("happy", null);
        KeyPhrase kp3 = new KeyPhrase("sad", null);
        entry1.setKeyPhrases(List.of(kp1, kp2));
        entry2.setKeyPhrases(List.of(kp3));
        List<JournalEntry> entries = List.of(entry1, entry2);

        when(journalPersistenceService.getEntriesForTrendsWithoutDecryption(user, start, end))
                .thenReturn(entries);

        // When
        Map<String, Long> result = journalService.getTopKeyPhrases(user, start, end, limit);

        // Then – sorted by frequency descending
        assertThat(result).containsExactly(
                Map.entry("happy", 2L),
                Map.entry("sad", 1L)
        );
        // Also verify that the limit is applied
        Map<String, Long> limited = journalService.getTopKeyPhrases(user, start, end, 1);
        assertThat(limited).containsExactly(Map.entry("happy", 2L));
    }

    @Test
    void getTopKeyPhrases_shouldReturnEmptyMap_whenNoKeyPhrases() {
        // Given
        JournalEntry entry = new JournalEntry();
        entry.setKeyPhrases(null);

        //  Wrap 'user' in eq() so all arguments are matchers
        when(journalPersistenceService.getEntriesForTrendsWithoutDecryption(eq(user), any(), any()))
                .thenReturn(List.of(entry));

        // When
        Map<String, Long> result = journalService.getTopKeyPhrases(user, LocalDate.now(), LocalDate.now(), 5);

        // Then
        assertThat(result).isEmpty();
    }

    // ========== getKeyPhraseFrequencies Tests ==========

    @Test
    void getKeyPhraseFrequencies_shouldAggregateKeyPhrases() {
        // Given
        JournalEntry entry1 = new JournalEntry();
        KeyPhrase kp1 = new KeyPhrase("happy", null);
        KeyPhrase kp2 = new KeyPhrase("happy", null);
        KeyPhrase kp3 = new KeyPhrase("sad", null);
        entry1.setKeyPhrases(List.of(kp1, kp2, kp3));
        JournalEntry entry2 = new JournalEntry();
        KeyPhrase kp4 = new KeyPhrase("happy", null);
        entry2.setKeyPhrases(List.of(kp4));
        List<JournalEntry> entries = List.of(entry1, entry2);

        when(journalPersistenceService.findByUser(user)).thenReturn(entries);

        // When
        Map<String, Long> result = journalService.getKeyPhraseFrequencies(user);

        // Then
        assertThat(result).containsExactly(
                Map.entry("happy", 3L),
                Map.entry("sad", 1L)
        );
        verify(journalPersistenceService).findByUser(user);
    }

    @Test
    void getKeyPhraseFrequencies_shouldReturnEmptyMap_whenNoKeyPhrases() {
        // Given
        JournalEntry entry = new JournalEntry();
        entry.setKeyPhrases(null);
        when(journalPersistenceService.findByUser(user)).thenReturn(List.of(entry));

        // When
        Map<String, Long> result = journalService.getKeyPhraseFrequencies(user);

        // Then
        assertThat(result).isEmpty();
    }

    // ========== Delegation Tests ==========

    @Test
    void saveJournalEntry_shouldDelegate() {
        String rawText = "Hello";
        JournalEntry expected = new JournalEntry();
        when(journalPersistenceService.saveJournalEntry(user, rawText)).thenReturn(expected);

        JournalEntry result = journalService.saveJournalEntry(user, rawText);
        assertThat(result).isSameAs(expected);
        verify(journalPersistenceService).saveJournalEntry(user, rawText);
    }

    @Test
    void updateJournalEntry_shouldDelegate() {
        UUID entryId = UUID.randomUUID();
        String updatedText = "Updated";
        JournalEntry expected = new JournalEntry();
        when(journalPersistenceService.updateJournalEntry(entryId, user, updatedText)).thenReturn(expected);

        JournalEntry result = journalService.updateJournalEntry(entryId, user, updatedText);
        assertThat(result).isSameAs(expected);
        verify(journalPersistenceService).updateJournalEntry(entryId, user, updatedText);
    }

    @Test
    void deleteJournalEntry_shouldDelegate() {
        UUID entryId = UUID.randomUUID();
        journalService.deleteJournalEntry(entryId, user);
        verify(journalPersistenceService).deleteJournalEntry(entryId, user);
    }

    @Test
    void getJournalEntriesForUser_shouldDelegate() {
        LocalDate start = LocalDate.now().minusDays(5);
        LocalDate end = LocalDate.now();
        List<JournalEntry> expected = List.of(new JournalEntry());
        when(journalPersistenceService.getJournalEntriesForUser(user, start, end)).thenReturn(expected);

        List<JournalEntry> result = journalService.getJournalEntriesForUser(user, start, end);
        assertThat(result).isSameAs(expected);
        verify(journalPersistenceService).getJournalEntriesForUser(user, start, end);
    }

    @Test
    void getJournalEntryById_shouldDelegate() {
        UUID entryId = UUID.randomUUID();
        JournalEntry expected = new JournalEntry();
        when(journalPersistenceService.getJournalEntryById(entryId)).thenReturn(Optional.of(expected));

        Optional<JournalEntry> result = journalService.getJournalEntryById(entryId);
        assertThat(result).isPresent().containsSame(expected);
        verify(journalPersistenceService).getJournalEntryById(entryId);
    }

    @Test
    void getMoodDataForChart_shouldDelegate() {
        LocalDate start = LocalDate.now().minusDays(5);
        LocalDate end = LocalDate.now();
        List<MoodDataResponse> expected = List.of(new MoodDataResponse(LocalDate.now(), 0.5));
        when(journalPersistenceService.getMoodDataForChart(user, start, end)).thenReturn(expected);

        List<MoodDataResponse> result = journalService.getMoodDataForChart(user, start, end);
        assertThat(result).isSameAs(expected);
        verify(journalPersistenceService).getMoodDataForChart(user, start, end);
    }

    @Test
    void getDailyAggregatedDataForUser_shouldDelegate() {
        LocalDate start = LocalDate.now().minusDays(5);
        LocalDate end = LocalDate.now();
        List<DailyAggregatedDataResponse> expected = List.of(new DailyAggregatedDataResponse(LocalDate.now(), 0.7, 100L));
        when(journalPersistenceService.getDailyAggregatedDataForUser(user, start, end)).thenReturn(expected);

        List<DailyAggregatedDataResponse> result = journalService.getDailyAggregatedDataForUser(user, start, end);
        assertThat(result).isSameAs(expected);
        verify(journalPersistenceService).getDailyAggregatedDataForUser(user, start, end);
    }

    @Test
    void findByUser_shouldDelegate() {
        List<JournalEntry> expected = List.of(new JournalEntry());
        when(journalPersistenceService.findByUser(user)).thenReturn(expected);

        List<JournalEntry> result = journalService.findByUser(user);
        assertThat(result).isSameAs(expected);
        verify(journalPersistenceService).findByUser(user);
    }

    @Test
    void getJournalEntriesPageResponse_shouldDelegate() {
        LocalDate start = LocalDate.now().minusDays(5);
        LocalDate end = LocalDate.now();
        Pageable pageable = mock(Pageable.class);
        PageResponse<JournalEntryResponse> expected = PageResponse.<JournalEntryResponse>builder().build();
        when(journalPersistenceService.getJournalEntriesPageResponse(user, start, end, pageable)).thenReturn(expected);

        PageResponse<JournalEntryResponse> result = journalService.getJournalEntriesPageResponse(user, start, end, pageable);
        assertThat(result).isSameAs(expected);
        verify(journalPersistenceService).getJournalEntriesPageResponse(user, start, end, pageable);
    }

    // Search delegation
    @Test
    void searchJournalEntriesByKeyword_shouldDelegate() {
        String keyword = "test";
        List<JournalEntry> expected = List.of(new JournalEntry());
        when(journalSearchService.searchJournalEntriesByKeyword(user, keyword)).thenReturn(expected);

        List<JournalEntry> result = journalService.searchJournalEntriesByKeyword(user, keyword);
        assertThat(result).isSameAs(expected);
        verify(journalSearchService).searchJournalEntriesByKeyword(user, keyword);
    }

    @Test
    void searchJournalEntriesByMoodScore_shouldDelegate() {
        Double min = 0.0, max = 1.0;
        List<JournalEntry> expected = List.of(new JournalEntry());
        when(journalSearchService.searchJournalEntriesByMoodScore(user, min, max)).thenReturn(expected);

        List<JournalEntry> result = journalService.searchJournalEntriesByMoodScore(user, min, max);
        assertThat(result).isSameAs(expected);
        verify(journalSearchService).searchJournalEntriesByMoodScore(user, min, max);
    }

    @Test
    void searchJournalEntriesSemantically_shouldDelegate() {
        String concept = "stress";
        List<JournalEntry> expected = List.of(new JournalEntry());
        when(journalSearchService.searchJournalEntriesSemantically(user, concept)).thenReturn(expected);

        List<JournalEntry> result = journalService.searchJournalEntriesSemantically(user, concept);
        assertThat(result).isSameAs(expected);
        verify(journalSearchService).searchJournalEntriesSemantically(user, concept);
    }

    // Reflection delegation
    @Test
    void generateDailyReflection_shouldDelegate() {
        String expected = "Reflection";
        when(journalReflectionService.generateDailyReflection(user)).thenReturn(expected);

        String result = journalService.generateDailyReflection(user);
        assertThat(result).isSameAs(expected);
        verify(journalReflectionService).generateDailyReflection(user);
    }

    @Test
    void generateReflectionChat_shouldDelegate() {
        String query = "query";
        String sessionId = "session";
        boolean rememberChat = true;
        String expected = "Response";
        when(journalReflectionService.generateReflectionChat(user, query, sessionId, rememberChat)).thenReturn(expected);

        String result = journalService.generateReflectionChat(user, query, sessionId, rememberChat);
        assertThat(result).isSameAs(expected);
        verify(journalReflectionService).generateReflectionChat(user, query, sessionId, rememberChat);
    }

    @Test
    void generateReflectiveQuestion_shouldDelegate() {
        String expected = "Question?";
        when(journalReflectionService.generateReflectiveQuestion(user)).thenReturn(expected);

        String result = journalService.generateReflectiveQuestion(user);
        assertThat(result).isSameAs(expected);
        verify(journalReflectionService).generateReflectiveQuestion(user);
    }

    // Anomaly detection
    @Test
    void runAnomalyDetection_shouldDelegate() {
        List<DailyAggregatedDataResponse> data = List.of();
        AnomalyDetectionResponse expected = new AnomalyDetectionResponse(
                List.of(), "No data provided for anomaly detection.");
        when(anomalyDetectionService.detectAnomalies(data)).thenReturn(expected);

        AnomalyDetectionResponse result = journalService.runAnomalyDetection(data);
        assertThat(result).isSameAs(expected);
        verify(anomalyDetectionService).detectAnomalies(data);
    }
}