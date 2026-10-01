package com.mymindmirror.backend.service;

import com.mymindmirror.backend.model.JournalEntry;
import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.repository.JournalEntryRepository;
import com.mymindmirror.backend.util.EncryptionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JournalSearchServiceTest {

    @Mock
    private JournalEntryRepository journalEntryRepository;

    @Mock
    private EmbeddingGenerationService embeddingGenerationService;

    @Mock
    private VectorStore vectorStore;

    @InjectMocks
    private JournalSearchService journalSearchService;

    private User user;
    private UUID userId;
    private String userSecret = "passwordHash";

    @BeforeEach
    void setUp() throws Exception {
        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setPasswordHash(userSecret);

        //  Set the similarityThresholdFallback via reflection
        var fallbackField = JournalSearchService.class.getDeclaredField("similarityThresholdFallback");
        fallbackField.setAccessible(true);
        fallbackField.set(journalSearchService, 0.50);
    }

    // ========== Keyword Search Tests ==========

    @Test
    void searchJournalEntriesByKeyword_shouldReturnMatchingEntries() {
        // Given
        String keyword = "watermelon";
        JournalEntry entry1 = new JournalEntry();
        entry1.setRawText("encrypted1");
        entry1.setUser(user);
        JournalEntry entry2 = new JournalEntry();
        entry2.setRawText("encrypted2");
        entry2.setUser(user);
        List<JournalEntry> allEntries = List.of(entry1, entry2);

        when(journalEntryRepository.findByUserOrderByCreationTimestampDesc(user)).thenReturn(allEntries);

        try (MockedStatic<EncryptionUtil> encryptionUtil = mockStatic(EncryptionUtil.class)) {
            encryptionUtil.when(() -> EncryptionUtil.decrypt("encrypted1", userSecret))
                    .thenReturn("Today I ate a watermelon");
            encryptionUtil.when(() -> EncryptionUtil.decrypt("encrypted2", userSecret))
                    .thenReturn("I had a stressful meeting");

            List<JournalEntry> result = journalSearchService.searchJournalEntriesByKeyword(user, keyword);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getRawText()).contains("watermelon");
        }
    }

    @Test
    void searchJournalEntriesByKeyword_shouldReturnEmpty_whenNoMatch() {
        // Given
        String keyword = "nonexistent";
        JournalEntry entry = new JournalEntry();
        entry.setRawText("encrypted");
        entry.setUser(user);
        when(journalEntryRepository.findByUserOrderByCreationTimestampDesc(user)).thenReturn(List.of(entry));

        try (MockedStatic<EncryptionUtil> encryptionUtil = mockStatic(EncryptionUtil.class)) {
            encryptionUtil.when(() -> EncryptionUtil.decrypt("encrypted", userSecret))
                    .thenReturn("Some random text");

            List<JournalEntry> result = journalSearchService.searchJournalEntriesByKeyword(user, keyword);

            assertThat(result).isEmpty();
        }
    }

    @Test
    void searchJournalEntriesByKeyword_shouldReturnEmpty_whenUserSecretNull() {
        // Given
        user.setPasswordHash(null);

        List<JournalEntry> result = journalSearchService.searchJournalEntriesByKeyword(user, "anything");

        assertThat(result).isEmpty();
        verify(journalEntryRepository, never()).findByUserOrderByCreationTimestampDesc(any());
    }

    // ========== Mood Search Tests ==========

    @Test
    void searchJournalEntriesByMoodScore_shouldReturnEntries() {
        // Given
        Double minMood = 0.5;
        Double maxMood = 1.0;
        JournalEntry entry = new JournalEntry();
        entry.setRawText("encrypted");
        entry.setUser(user);
        List<JournalEntry> entries = List.of(entry);

        when(journalEntryRepository.findByUserAndMoodScoreBetweenOrderByCreationTimestampDesc(user, minMood, maxMood))
                .thenReturn(entries);

        try (MockedStatic<EncryptionUtil> encryptionUtil = mockStatic(EncryptionUtil.class)) {
            encryptionUtil.when(() -> EncryptionUtil.decrypt("encrypted", userSecret))
                    .thenReturn("Decrypted text");

            List<JournalEntry> result = journalSearchService.searchJournalEntriesByMoodScore(user, minMood, maxMood);

            assertThat(result).hasSize(1);
            verify(journalEntryRepository).findByUserAndMoodScoreBetweenOrderByCreationTimestampDesc(user, minMood, maxMood);
        }
    }

    @Test
    void searchJournalEntriesByMoodScore_shouldReturnEntriesWithoutDecryption_whenUserSecretNull() {
        // Given
        user.setPasswordHash(null);
        Double minMood = 0.0;
        Double maxMood = 1.0;
        JournalEntry entry = new JournalEntry();
        entry.setRawText("encrypted");
        entry.setUser(user);
        List<JournalEntry> entries = List.of(entry);

        when(journalEntryRepository.findByUserAndMoodScoreBetweenOrderByCreationTimestampDesc(user, minMood, maxMood))
                .thenReturn(entries);

        List<JournalEntry> result = journalSearchService.searchJournalEntriesByMoodScore(user, minMood, maxMood);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRawText()).isEqualTo("encrypted");
        verify(journalEntryRepository).findByUserAndMoodScoreBetweenOrderByCreationTimestampDesc(user, minMood, maxMood);
    }

    // ========== Semantic Search Tests ==========

    @Test
    void searchJournalEntriesSemantically_shouldReturnRankedEntries() {
        // Given
        String concept = "stress";
        UUID entryId1 = UUID.randomUUID();
        UUID entryId2 = UUID.randomUUID();

        Document doc1 = Document.builder()
                .id(entryId1.toString())
                .text("doc1")
                .metadata(Map.of("entryId", entryId1.toString(), "userId", userId.toString()))
                .build();
        Document doc2 = Document.builder()
                .id(entryId2.toString())
                .text("doc2")
                .metadata(Map.of("entryId", entryId2.toString(), "userId", userId.toString()))
                .build();
        List<Document> documents = List.of(doc1, doc2);

        when(embeddingGenerationService.getVectorStore()).thenReturn(vectorStore);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(documents);

        JournalEntry entry1 = new JournalEntry();
        entry1.setId(entryId1);
        entry1.setRawText("encrypted1");
        entry1.setUser(user);
        JournalEntry entry2 = new JournalEntry();
        entry2.setId(entryId2);
        entry2.setRawText("encrypted2");
        entry2.setUser(user);
        when(journalEntryRepository.findByIdIn(List.of(entryId1, entryId2))).thenReturn(List.of(entry1, entry2));

        try (MockedStatic<EncryptionUtil> encryptionUtil = mockStatic(EncryptionUtil.class)) {
            encryptionUtil.when(() -> EncryptionUtil.decrypt("encrypted1", userSecret))
                    .thenReturn("Stressful meeting");
            encryptionUtil.when(() -> EncryptionUtil.decrypt("encrypted2", userSecret))
                    .thenReturn("Relaxing day");

            List<JournalEntry> result = journalSearchService.searchJournalEntriesSemantically(user, concept);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getId()).isEqualTo(entryId1);
            assertThat(result.get(1).getId()).isEqualTo(entryId2);
        }
    }

    @Test
    void searchJournalEntriesSemantically_shouldReturnEmpty_whenNoDocumentsFound() {
        // Given
        when(embeddingGenerationService.getVectorStore()).thenReturn(vectorStore);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        List<JournalEntry> result = journalSearchService.searchJournalEntriesSemantically(user, "anything");

        assertThat(result).isEmpty();
        verify(journalEntryRepository, never()).findByIdIn(any());
    }

    @Test
    void searchJournalEntriesSemantically_shouldReturnEmpty_whenUserSecretNull() {
        // Given
        user.setPasswordHash(null);

        List<JournalEntry> result = journalSearchService.searchJournalEntriesSemantically(user, "anything");

        assertThat(result).isEmpty();
        verify(embeddingGenerationService, never()).getVectorStore();
    }

    @Test
    void searchJournalEntriesSemantically_shouldThrow503_whenVectorStoreUnavailable() {
        when(embeddingGenerationService.getVectorStore()).thenThrow(new RuntimeException("Vector store error"));

        assertThatThrownBy(() -> journalSearchService.searchJournalEntriesSemantically(user, "concept"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Semantic search is temporarily unavailable");
    }

    // ========== Internal retrieveRelevantEntries Tests ==========

    @Test
    void retrieveRelevantEntries_shouldReturnRankedEntries() {
        // Given
        String query = "anxiety";
        int topK = 5;
        UUID entryId1 = UUID.randomUUID();
        UUID entryId2 = UUID.randomUUID();

        Document doc1 = Document.builder()
                .id(entryId1.toString())
                .text("doc1")
                .metadata(Map.of("entryId", entryId1.toString(), "userId", userId.toString()))
                .build();
        Document doc2 = Document.builder()
                .id(entryId2.toString())
                .text("doc2")
                .metadata(Map.of("entryId", entryId2.toString(), "userId", userId.toString()))
                .build();
        List<Document> documents = List.of(doc1, doc2);

        when(embeddingGenerationService.getVectorStore()).thenReturn(vectorStore);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(documents);

        JournalEntry entry1 = new JournalEntry();
        entry1.setId(entryId1);
        entry1.setUser(user);
        JournalEntry entry2 = new JournalEntry();
        entry2.setId(entryId2);
        entry2.setUser(user);
        when(journalEntryRepository.findByIdIn(List.of(entryId1, entryId2))).thenReturn(List.of(entry1, entry2));

        // When
        List<JournalEntry> result = journalSearchService.retrieveRelevantEntries(user, query, topK);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(entryId1);
        assertThat(result.get(1).getId()).isEqualTo(entryId2);
    }

    @Test
    void retrieveRelevantEntries_shouldReturnEmpty_whenNoDocumentsFound() {
        // Given
        when(embeddingGenerationService.getVectorStore()).thenReturn(vectorStore);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        // When
        List<JournalEntry> result = journalSearchService.retrieveRelevantEntries(user, "query", 5);

        // Then
        assertThat(result).isEmpty();
        verify(journalEntryRepository, never()).findByIdIn(any());
    }

    @Test
    void retrieveRelevantEntries_shouldReturnEmpty_whenDocumentsNull() {
        // Given
        when(embeddingGenerationService.getVectorStore()).thenReturn(vectorStore);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(null);

        // When
        List<JournalEntry> result = journalSearchService.retrieveRelevantEntries(user, "query", 5);

        // Then
        assertThat(result).isEmpty();
        verify(journalEntryRepository, never()).findByIdIn(any());
    }

    @Test
    void retrieveRelevantEntries_shouldHandleException() {
        // Given
        when(embeddingGenerationService.getVectorStore()).thenThrow(new RuntimeException("Vector store error"));

        // When
        List<JournalEntry> result = journalSearchService.retrieveRelevantEntries(user, "query", 5);

        // Then
        assertThat(result).isEmpty();
    }


    // ========== Edge Case & Short-Circuit Coverage Tests ==========

    @Test
    void searchMethods_shouldHandleEmptyUserSecretString() {
        // Covers: if (userSecret == null || userSecret.isEmpty()) -> isEmpty() branch
        user.setPasswordHash("");

        // Keyword Search
        assertThat(journalSearchService.searchJournalEntriesByKeyword(user, "test")).isEmpty();

        // Semantic Search
        assertThat(journalSearchService.searchJournalEntriesSemantically(user, "test")).isEmpty();

        // Mood Score Search
        JournalEntry entry = new JournalEntry();
        entry.setRawText("encrypted");
        when(journalEntryRepository.findByUserAndMoodScoreBetweenOrderByCreationTimestampDesc(user, 0.0, 1.0))
                .thenReturn(List.of(entry));

        List<JournalEntry> moodResult = journalSearchService.searchJournalEntriesByMoodScore(user, 0.0, 1.0);
        assertThat(moodResult).hasSize(1);
        assertThat(moodResult.get(0).getRawText()).isEqualTo("encrypted");
    }

    @Test
    void searchJournalEntriesByKeyword_shouldHandleNullDecryptionFallback() {
        // Covers the ternary fallback: decryptedText != null ? decryptedText : ""
        JournalEntry entry = new JournalEntry();
        entry.setId(UUID.randomUUID());
        entry.setRawText("encrypted");
        entry.setUser(user);

        when(journalEntryRepository.findByUserOrderByCreationTimestampDesc(user)).thenReturn(List.of(entry));

        try (MockedStatic<EncryptionUtil> encryptionUtil = mockStatic(EncryptionUtil.class)) {
            // Return null to trigger the false branch of the ternary operator
            encryptionUtil.when(() -> EncryptionUtil.decrypt("encrypted", userSecret)).thenReturn(null);

            List<JournalEntry> result = journalSearchService.searchJournalEntriesByKeyword(user, "anything");

            // "anything" won't match the fallback string "", so it safely returns empty
            assertThat(result).isEmpty();
        }
    }

    @Test
    void searchJournalEntriesSemantically_shouldReturnEmpty_whenDocumentsListIsNull() {
        // Covers: if (documents == null || documents.isEmpty()) -> documents == null branch
        when(embeddingGenerationService.getVectorStore()).thenReturn(vectorStore);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(null);

        List<JournalEntry> result = journalSearchService.searchJournalEntriesSemantically(user, "concept");

        assertThat(result).isEmpty();
        verify(journalEntryRepository, never()).findByIdIn(any());
    }

    @Test
    void searchJournalEntriesSemantically_shouldHandleMissingDatabaseEntries_AndNullDecryption() {
        // Covers: if (entry != null) false branch
        // Covers: decryptedText != null ? decryptedText : "" false branch
        UUID validId = UUID.randomUUID();
        UUID missingId = UUID.randomUUID();

        Document doc1 = Document.builder().id(validId.toString()).text("doc1")
                .metadata(Map.of("entryId", validId.toString(), "userId", userId.toString())).build();
        Document doc2 = Document.builder().id(missingId.toString()).text("doc2")
                .metadata(Map.of("entryId", missingId.toString(), "userId", userId.toString())).build();

        when(embeddingGenerationService.getVectorStore()).thenReturn(vectorStore);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(doc1, doc2));

        JournalEntry validEntry = new JournalEntry();
        validEntry.setId(validId);
        validEntry.setRawText("encrypted");
        validEntry.setUser(user);

        // The DB only returns 'validId'. 'missingId' is completely skipped.
        when(journalEntryRepository.findByIdIn(List.of(validId, missingId))).thenReturn(List.of(validEntry));

        try (MockedStatic<EncryptionUtil> encryptionUtil = mockStatic(EncryptionUtil.class)) {
            // Return null to trigger the ternary fallback
            encryptionUtil.when(() -> EncryptionUtil.decrypt("encrypted", userSecret)).thenReturn(null);

            List<JournalEntry> result = journalSearchService.searchJournalEntriesSemantically(user, "concept");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(validId);
            assertThat(result.get(0).getRawText()).isEqualTo(""); // Validates the empty string fallback
        }
    }

    @Test
    void retrieveRelevantEntries_shouldSkipMissingDatabaseEntries() {
        // Covers: if (entry != null) false branch inside retrieveRelevantEntries
        UUID validId = UUID.randomUUID();
        UUID missingId = UUID.randomUUID();

        Document doc1 = Document.builder().id(validId.toString()).text("doc1")
                .metadata(Map.of("entryId", validId.toString(), "userId", userId.toString())).build();
        Document doc2 = Document.builder().id(missingId.toString()).text("doc2")
                .metadata(Map.of("entryId", missingId.toString(), "userId", userId.toString())).build();

        when(embeddingGenerationService.getVectorStore()).thenReturn(vectorStore);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(doc1, doc2));

        JournalEntry validEntry = new JournalEntry();
        validEntry.setId(validId);

        when(journalEntryRepository.findByIdIn(List.of(validId, missingId))).thenReturn(List.of(validEntry));

        List<JournalEntry> result = journalSearchService.retrieveRelevantEntries(user, "query", 5);

        // Safely filtered out the missing DB entry
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(validId);
    }
}