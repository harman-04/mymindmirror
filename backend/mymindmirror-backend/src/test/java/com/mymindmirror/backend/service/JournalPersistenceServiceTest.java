package com.mymindmirror.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mymindmirror.backend.mapper.JournalMapper;
import com.mymindmirror.backend.model.JournalEntry;
import com.mymindmirror.backend.model.KeyPhrase;
import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.repository.DailyJournalSummaryRepository;
import com.mymindmirror.backend.repository.JournalEntryRepository;
import com.mymindmirror.backend.util.EncryptionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class JournalPersistenceServiceTest {

    @Mock
    private JournalEntryRepository journalEntryRepository;

    @Mock
    private DailyJournalSummaryRepository dailyJournalSummaryRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private GamificationService gamificationService;

    @Mock
    private AsyncJournalAnalysisService asyncJournalAnalysisService;

    @Mock
    private EmbeddingGenerationService embeddingGenerationService;

    @Mock
    private UserService userService;

    @Mock
    private JournalMapper journalMapper;

    @InjectMocks
    private JournalPersistenceService journalPersistenceService;

    private User user;
    private UUID userId;
    private String rawText = "Hello world";
    private String encryptedText = "encrypted";
    private String decryptedText = "Hello world";

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setPasswordHash("passwordHash");
    }

    // ========== saveJournalEntry Tests ==========

    @Test
    void saveJournalEntry_shouldEncryptAndSaveAndTriggerAsyncAnalysis() {
        // Given
        try (MockedStatic<EncryptionUtil> encryptionUtil = mockStatic(EncryptionUtil.class)) {
            encryptionUtil.when(() -> EncryptionUtil.encrypt(rawText, user.getPasswordHash()))
                    .thenReturn(encryptedText);

            JournalEntry savedEntry = new JournalEntry();
            savedEntry.setId(UUID.randomUUID());
            savedEntry.setRawText(encryptedText);
            savedEntry.setUser(user);
            savedEntry.setEntryDate(LocalDate.now());
            savedEntry.setCreationTimestamp(LocalDateTime.now());

            //  Capture the entry passed to repository.save()
            ArgumentCaptor<JournalEntry> entryCaptor = ArgumentCaptor.forClass(JournalEntry.class);
            when(journalEntryRepository.save(entryCaptor.capture())).thenReturn(savedEntry);

            // When
            JournalEntry result = journalPersistenceService.saveJournalEntry(user, rawText);

            // Then
            JournalEntry capturedEntry = entryCaptor.getValue();
            assertThat(capturedEntry.getWordCount()).isEqualTo(2); // "Hello world" has 2 words
            verify(journalEntryRepository).save(any(JournalEntry.class));
            verify(gamificationService).recordActivity(eq(user), any());
            verify(asyncJournalAnalysisService).analyzeJournalEntryAsync(eq(result.getId()), eq(rawText), eq(userId));
        }
    }

    @Test
    void saveJournalEntry_shouldHandleEmptyRawText() {
        // Given
        try (MockedStatic<EncryptionUtil> encryptionUtil = mockStatic(EncryptionUtil.class)) {
            encryptionUtil.when(() -> EncryptionUtil.encrypt("", user.getPasswordHash()))
                    .thenReturn(encryptedText);

            JournalEntry savedEntry = new JournalEntry();
            savedEntry.setId(UUID.randomUUID());
            savedEntry.setRawText(encryptedText);
            when(journalEntryRepository.save(any(JournalEntry.class))).thenReturn(savedEntry);

            // When
            JournalEntry result = journalPersistenceService.saveJournalEntry(user, "");

            // Then
            assertThat(result.getWordCount()).isEqualTo(0);
        }
    }

    @Test
    void saveJournalEntry_shouldFallbackToPlainText_whenEncryptionFails() {
        // Given – encryption returns null (failure)
        try (MockedStatic<EncryptionUtil> encryptionUtil = mockStatic(EncryptionUtil.class)) {
            encryptionUtil.when(() -> EncryptionUtil.encrypt(rawText, user.getPasswordHash()))
                    .thenReturn(null);

            JournalEntry savedEntry = new JournalEntry();
            savedEntry.setId(UUID.randomUUID());
            savedEntry.setRawText(rawText); // plain text
            when(journalEntryRepository.save(any(JournalEntry.class))).thenReturn(savedEntry);

            // When
            JournalEntry result = journalPersistenceService.saveJournalEntry(user, rawText);

            // Then – should not throw, and should store plain text
            assertThat(result.getRawText()).isEqualTo(rawText);
            verify(journalEntryRepository).save(any(JournalEntry.class));
        }
    }

    // ========== updateJournalEntry Tests ==========

    @Test
    void updateJournalEntry_shouldUpdateAndReEncryptAndReAnalyze() {
        // Given
        UUID entryId = UUID.randomUUID();
        JournalEntry existingEntry = new JournalEntry();
        existingEntry.setId(entryId);
        existingEntry.setUser(user);
        existingEntry.setRawText("old encrypted");

        String updatedText = "New content";
        String newEncrypted = "new encrypted";

        try (MockedStatic<EncryptionUtil> encryptionUtil = mockStatic(EncryptionUtil.class)) {
            encryptionUtil.when(() -> EncryptionUtil.decrypt("old encrypted", user.getPasswordHash()))
                    .thenReturn("old text");
            encryptionUtil.when(() -> EncryptionUtil.encrypt(updatedText, user.getPasswordHash()))
                    .thenReturn(newEncrypted);

            when(journalEntryRepository.findByIdWithDetails(entryId)).thenReturn(Optional.of(existingEntry));
            when(journalEntryRepository.save(any(JournalEntry.class))).thenReturn(existingEntry);

            // When
            JournalEntry result = journalPersistenceService.updateJournalEntry(entryId, user, updatedText);

            // Then
            assertThat(result.getRawText()).isEqualTo(newEncrypted);
            assertThat(result.getWordCount()).isEqualTo(2); // "New content"
            verify(journalEntryRepository).save(existingEntry);
            verify(asyncJournalAnalysisService).analyzeJournalEntryAsync(eq(entryId), eq(updatedText), eq(userId));
        }
    }

    @Test
    void updateJournalEntry_shouldThrowException_whenEntryNotFound() {
        // Given
        UUID entryId = UUID.randomUUID();
        when(journalEntryRepository.findByIdWithDetails(entryId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> journalPersistenceService.updateJournalEntry(entryId, user, "new"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void updateJournalEntry_shouldThrowException_whenUserNotOwner() {
        // Given
        UUID entryId = UUID.randomUUID();
        User otherUser = new User();
        otherUser.setId(UUID.randomUUID());
        JournalEntry entry = new JournalEntry();
        entry.setId(entryId);
        entry.setUser(otherUser);

        when(journalEntryRepository.findByIdWithDetails(entryId)).thenReturn(Optional.of(entry));

        // When/Then
        assertThatThrownBy(() -> journalPersistenceService.updateJournalEntry(entryId, user, "new"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Journal entry not found");
    }

    // ========== deleteJournalEntry Tests ==========

    @Test
    void deleteJournalEntry_shouldDeleteAndRemoveEmbedding() {
        // Given
        UUID entryId = UUID.randomUUID();
        JournalEntry entry = new JournalEntry();
        entry.setId(entryId);
        entry.setUser(user);

        when(journalEntryRepository.findById(entryId)).thenReturn(Optional.of(entry));

        // When
        journalPersistenceService.deleteJournalEntry(entryId, user);

        // Then
        verify(journalEntryRepository).delete(entry);
        verify(embeddingGenerationService).deleteEmbedding(entryId);
    }

    @Test
    void deleteJournalEntry_shouldThrowException_whenUserNotOwner() {
        // Given
        UUID entryId = UUID.randomUUID();
        User otherUser = new User();
        otherUser.setId(UUID.randomUUID());
        JournalEntry entry = new JournalEntry();
        entry.setId(entryId);
        entry.setUser(otherUser);

        when(journalEntryRepository.findById(entryId)).thenReturn(Optional.of(entry));

        // When/Then
        assertThatThrownBy(() -> journalPersistenceService.deleteJournalEntry(entryId, user))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Journal entry not found");
    }

    // ========== getJournalEntriesForUser Tests ==========

    @Test
    void getJournalEntriesForUser_shouldReturnDecryptedEntries() {
        // Given
        LocalDate start = LocalDate.now().minusDays(5);
        LocalDate end = LocalDate.now();
        JournalEntry entry1 = new JournalEntry();
        entry1.setRawText("encrypted1");
        entry1.setUser(user);
        JournalEntry entry2 = new JournalEntry();
        entry2.setRawText("encrypted2");
        entry2.setUser(user);
        List<JournalEntry> entries = List.of(entry1, entry2);

        when(journalEntryRepository.findByUserAndEntryDateBetweenOrderByCreationTimestampDesc(user, start, end))
                .thenReturn(entries);

        try (MockedStatic<EncryptionUtil> encryptionUtil = mockStatic(EncryptionUtil.class)) {
            encryptionUtil.when(() -> EncryptionUtil.decrypt("encrypted1", user.getPasswordHash()))
                    .thenReturn("plain1");
            encryptionUtil.when(() -> EncryptionUtil.decrypt("encrypted2", user.getPasswordHash()))
                    .thenReturn("plain2");

            // When
            List<JournalEntry> result = journalPersistenceService.getJournalEntriesForUser(user, start, end);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getRawText()).isEqualTo("plain1");
            assertThat(result.get(1).getRawText()).isEqualTo("plain2");
        }
    }

    // ========== getJournalEntryById Tests ==========

    @Test
    void getJournalEntryById_shouldReturnDecryptedEntry() {
        // Given
        UUID entryId = UUID.randomUUID();
        JournalEntry entry = new JournalEntry();
        entry.setId(entryId);
        entry.setRawText("encrypted");
        entry.setUser(user);

        when(journalEntryRepository.findByIdWithDetails(entryId)).thenReturn(Optional.of(entry));

        try (MockedStatic<EncryptionUtil> encryptionUtil = mockStatic(EncryptionUtil.class)) {
            encryptionUtil.when(() -> EncryptionUtil.decrypt("encrypted", user.getPasswordHash()))
                    .thenReturn("plain");

            // When
            Optional<JournalEntry> result = journalPersistenceService.getJournalEntryById(entryId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getRawText()).isEqualTo("plain");
        }
    }

    // ========== getJournalEntriesPageResponse Tests ==========

    @Test
    void getJournalEntriesPageResponse_shouldDecryptAndMapEntries() {
        // Given
        LocalDate start = LocalDate.of(2026, 9, 1);
        LocalDate end = LocalDate.of(2026, 9, 8);
        Pageable pageable = PageRequest.of(0, 10);

        JournalEntry entry = new JournalEntry();
        entry.setId(UUID.randomUUID());
        entry.setRawText("encrypted");
        entry.setUser(user);

        Page<JournalEntry> page = new PageImpl<>(List.of(entry), pageable, 1);
        when(journalEntryRepository.findByUserAndEntryDateBetweenOrderByCreationTimestampDesc(
                user, start, end, pageable))
                .thenReturn(page);

        try (MockedStatic<EncryptionUtil> encryptionUtil = mockStatic(EncryptionUtil.class)) {
            encryptionUtil.when(() -> EncryptionUtil.decrypt("encrypted", user.getPasswordHash()))
                    .thenReturn("plain text");

            // When
            var result = journalPersistenceService.getJournalEntriesPageResponse(user, start, end, pageable);

            // Then
            assertThat(result.content()).hasSize(1);
            assertThat(result.pageNumber()).isEqualTo(0);
            assertThat(result.totalPages()).isEqualTo(1);
        }
    }
    @Test
    void getMoodDataForChart_shouldReturnMoodData() {
        // Given
        LocalDate start = LocalDate.now().minusDays(5);
        LocalDate end = LocalDate.now();
        JournalEntry entry1 = new JournalEntry();
        entry1.setEntryDate(LocalDate.now());
        entry1.setMoodScore(0.8);
        JournalEntry entry2 = new JournalEntry();
        entry2.setEntryDate(LocalDate.now().minusDays(1));
        entry2.setMoodScore(0.5);
        List<JournalEntry> entries = List.of(entry1, entry2);

        when(journalEntryRepository.findByUserAndEntryDateBetween(user, start, end))
                .thenReturn(entries);

        // When
        var result = journalPersistenceService.getMoodDataForChart(user, start, end);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).date()).isEqualTo(LocalDate.now().minusDays(1)); // sorted
        assertThat(result.get(0).moodScore()).isEqualTo(0.5);
    }

    @Test
    void getDailyAggregatedDataForUser_shouldReturnAggregatedData() {
        // Given
        LocalDate start = LocalDate.of(2026, 9, 1);
        LocalDate end = LocalDate.of(2026, 9, 8);
        //  Provide data in ascending order (earliest first)
        List<Object[]> rawResults = List.of(
                new Object[]{LocalDate.of(2026, 9, 6), 0.6, 90L},  // earlier
                new Object[]{LocalDate.of(2026, 9, 7), 0.7, 100L}   // later
        );
        when(dailyJournalSummaryRepository.findByUserAndDateRange(user.getId(), start, end))
                .thenReturn(rawResults);

        // When
        var result = journalPersistenceService.getDailyAggregatedDataForUser(user, start, end);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).date()).isEqualTo(LocalDate.of(2026, 9, 6));
        assertThat(result.get(0).averageMood()).isEqualTo(0.6);
        assertThat(result.get(0).totalWords()).isEqualTo(90L);
    }
    // ========== findByUser Tests ==========

    @Test
    void findByUser_shouldReturnAllEntriesAndDecrypt() {
        // Given
        JournalEntry entry1 = new JournalEntry();
        entry1.setRawText("enc1");
        entry1.setUser(user);
        JournalEntry entry2 = new JournalEntry();
        entry2.setRawText("enc2");
        entry2.setUser(user);
        List<JournalEntry> entries = List.of(entry1, entry2);

        when(journalEntryRepository.findByUser(user)).thenReturn(entries);

        try (MockedStatic<EncryptionUtil> encryptionUtil = mockStatic(EncryptionUtil.class)) {
            encryptionUtil.when(() -> EncryptionUtil.decrypt("enc1", user.getPasswordHash()))
                    .thenReturn("plain1");
            encryptionUtil.when(() -> EncryptionUtil.decrypt("enc2", user.getPasswordHash()))
                    .thenReturn("plain2");

            // When
            List<JournalEntry> result = journalPersistenceService.findByUser(user);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getRawText()).isEqualTo("plain1");
        }
    }


    @Test
    void findLatestEntryByUser_shouldReturnLatestEntry() {
        // Given
        JournalEntry entry = new JournalEntry();
        entry.setId(UUID.randomUUID());
        entry.setUser(user);
        PageRequest limitOne = PageRequest.of(0, 1);
        when(journalEntryRepository.findLatestEntryByUser(user, limitOne)).thenReturn(List.of(entry));

        // When
        Optional<JournalEntry> result = journalPersistenceService.findLatestEntryByUser(user);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(entry);
    }

    @Test
    void findLatestEntryByUser_shouldReturnEmpty_whenNoEntries() {
        // Given
        PageRequest limitOne = PageRequest.of(0, 1);
        when(journalEntryRepository.findLatestEntryByUser(user, limitOne)).thenReturn(List.of());

        // When
        Optional<JournalEntry> result = journalPersistenceService.findLatestEntryByUser(user);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getLatestJournalTimelineWithoutDecryption_shouldReturnEntries() {
        // Given
        int limit = 5;
        JournalEntry entry = new JournalEntry();
        entry.setId(UUID.randomUUID());
        PageRequest pageRequest = PageRequest.of(0, limit);
        when(journalEntryRepository.findLatestEntryByUser(user, pageRequest)).thenReturn(List.of(entry));

        // When
        List<JournalEntry> result = journalPersistenceService.getLatestJournalTimelineWithoutDecryption(user, limit);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(entry);
    }

    @Test
    void getEntriesForTrendsWithoutDecryption_shouldReturnEntries() {
        // Given
        LocalDate start = LocalDate.now().minusDays(90);
        LocalDate end = LocalDate.now();
        JournalEntry entry = new JournalEntry();
        entry.setId(UUID.randomUUID());
        when(journalEntryRepository.findByUserAndEntryDateBetweenOrderByCreationTimestampDesc(user, start, end))
                .thenReturn(List.of(entry));

        // When
        List<JournalEntry> result = journalPersistenceService.getEntriesForTrendsWithoutDecryption(user, start, end);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(entry);
    }

    @Test
    void getJournalEntriesForUser_shouldHandleNullPasswordHash() {
        // Given
        user.setPasswordHash(null);
        LocalDate start = LocalDate.now().minusDays(5);
        LocalDate end = LocalDate.now();
        JournalEntry entry = new JournalEntry();
        entry.setRawText("encrypted");
        when(journalEntryRepository.findByUserAndEntryDateBetweenOrderByCreationTimestampDesc(user, start, end))
                .thenReturn(List.of(entry));

        // When
        List<JournalEntry> result = journalPersistenceService.getJournalEntriesForUser(user, start, end);

        // Then – should not decrypt, rawText remains "encrypted"
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRawText()).isEqualTo("encrypted");
    }

    // Similarly for getJournalEntryById, getMoodDataForChart, findByUser
    @Test
    void getJournalEntryById_shouldHandleNullPasswordHash() {
        user.setPasswordHash(null);
        UUID entryId = UUID.randomUUID();
        JournalEntry entry = new JournalEntry();
        entry.setId(entryId);
        entry.setRawText("encrypted");
        entry.setUser(user); // ✅ Required – must set the user!
        when(journalEntryRepository.findByIdWithDetails(entryId)).thenReturn(Optional.of(entry));

        Optional<JournalEntry> result = journalPersistenceService.getJournalEntryById(entryId);
        assertThat(result).isPresent();
        assertThat(result.get().getRawText()).isEqualTo("encrypted");
    }

    @Test
    void getMoodDataForChart_shouldHandleNullPasswordHash() {
        user.setPasswordHash(null);
        LocalDate start = LocalDate.now().minusDays(5);
        LocalDate end = LocalDate.now();
        JournalEntry entry = new JournalEntry();
        entry.setMoodScore(0.5);
        when(journalEntryRepository.findByUserAndEntryDateBetween(user, start, end))
                .thenReturn(List.of(entry));

        var result = journalPersistenceService.getMoodDataForChart(user, start, end);
        assertThat(result).hasSize(1);
        // Mood data doesn't use rawText, but it still runs the loop; branch covered.
    }

    @Test
    void findByUser_shouldHandleNullPasswordHash() {
        user.setPasswordHash(null);
        JournalEntry entry = new JournalEntry();
        entry.setRawText("encrypted");
        when(journalEntryRepository.findByUser(user)).thenReturn(List.of(entry));

        List<JournalEntry> result = journalPersistenceService.findByUser(user);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRawText()).isEqualTo("encrypted");
    }

    @Test
    void resetAiFields_shouldCreateNewKeyPhrasesList_whenNull() {
        JournalEntry entry = new JournalEntry();
        entry.setKeyPhrases(null); // explicitly null

        journalPersistenceService.resetAiFields(entry);

        assertThat(entry.getKeyPhrases()).isNotNull();
        assertThat(entry.getKeyPhrases()).isEmpty();
        assertThat(entry.getMoodScore()).isNull();
        assertThat(entry.getEmotions()).isNull();
        // ... other fields reset
    }


    @Test
    void saveJournalEntry_shouldThrowException_whenUserSecretIsNull() {
        // Given
        user.setPasswordHash(null); // no secret
        String rawText = "Hello";

        // When/Then
        assertThatThrownBy(() -> journalPersistenceService.saveJournalEntry(user, rawText))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("User secret (password hash) not available for encryption.");
    }

    @Test
    void saveJournalEntry_shouldRegisterSynchronization_whenTransactionActive() {
        // Given
        String rawText = "Hello";
        String encrypted = "encrypted";

        try (MockedStatic<EncryptionUtil> encryptionUtil = mockStatic(EncryptionUtil.class);
             MockedStatic<TransactionSynchronizationManager> txManager = mockStatic(TransactionSynchronizationManager.class)) {

            encryptionUtil.when(() -> EncryptionUtil.encrypt(rawText, user.getPasswordHash()))
                    .thenReturn(encrypted);

            txManager.when(TransactionSynchronizationManager::isActualTransactionActive).thenReturn(true);

            // Capture the synchronization object
            ArgumentCaptor<TransactionSynchronization> syncCaptor = ArgumentCaptor.forClass(TransactionSynchronization.class);
            txManager.when(() -> TransactionSynchronizationManager.registerSynchronization(syncCaptor.capture()))
                    .thenAnswer(invocation -> null); // void method

            JournalEntry savedEntry = new JournalEntry();
            savedEntry.setId(UUID.randomUUID());
            savedEntry.setRawText(encrypted);
            when(journalEntryRepository.save(any(JournalEntry.class))).thenReturn(savedEntry);

            // When
            JournalEntry result = journalPersistenceService.saveJournalEntry(user, rawText);

            // Then
            verify(journalEntryRepository).save(any(JournalEntry.class));
            // Verify that registerSynchronization was called
            txManager.verify(() -> TransactionSynchronizationManager.registerSynchronization(any(TransactionSynchronization.class)));

            // Verify that async analysis was NOT called directly (it should be scheduled)
            verify(asyncJournalAnalysisService, never()).analyzeJournalEntryAsync(any(), any(), any());

            // Now manually trigger the synchronization's afterCommit
            TransactionSynchronization sync = syncCaptor.getValue();
            sync.afterCommit();

            // Verify that async analysis was called after commit
            verify(asyncJournalAnalysisService).analyzeJournalEntryAsync(eq(savedEntry.getId()), eq(rawText), eq(user.getId()));
        }
    }

    @Test
    void updateJournalEntry_shouldRegisterSynchronization_whenTransactionActive() throws Exception {
        // Given
        UUID entryId = UUID.randomUUID();
        String updatedText = "New content";
        String newEncrypted = "new encrypted";
        JournalEntry existingEntry = new JournalEntry();
        existingEntry.setId(entryId);
        existingEntry.setUser(user);
        existingEntry.setRawText("old encrypted");

        try (MockedStatic<EncryptionUtil> encryptionUtil = mockStatic(EncryptionUtil.class);
             MockedStatic<TransactionSynchronizationManager> txManager = mockStatic(TransactionSynchronizationManager.class)) {

            encryptionUtil.when(() -> EncryptionUtil.decrypt("old encrypted", user.getPasswordHash()))
                    .thenReturn("old text");
            encryptionUtil.when(() -> EncryptionUtil.encrypt(updatedText, user.getPasswordHash()))
                    .thenReturn(newEncrypted);

            txManager.when(TransactionSynchronizationManager::isActualTransactionActive).thenReturn(true);

            ArgumentCaptor<TransactionSynchronization> syncCaptor = ArgumentCaptor.forClass(TransactionSynchronization.class);
            txManager.when(() -> TransactionSynchronizationManager.registerSynchronization(syncCaptor.capture()))
                    .thenAnswer(invocation -> null);

            when(journalEntryRepository.findByIdWithDetails(entryId)).thenReturn(Optional.of(existingEntry));
            when(journalEntryRepository.save(any(JournalEntry.class))).thenReturn(existingEntry);

            // When
            journalPersistenceService.updateJournalEntry(entryId, user, updatedText);

            // Then
            verify(journalEntryRepository).save(existingEntry);
            verify(asyncJournalAnalysisService, never()).analyzeJournalEntryAsync(any(), any(), any());

            TransactionSynchronization sync = syncCaptor.getValue();
            sync.afterCommit();

            verify(asyncJournalAnalysisService).analyzeJournalEntryAsync(eq(entryId), eq(updatedText), eq(user.getId()));
        }
    }

    // ========================================================================
    // ULTIMATE JACOCO SWEEP: JournalPersistenceService Edge Cases
    // ========================================================================

    @Test
    void saveJournalEntry_shouldHandleNullText_and_InactiveTransaction() {
        // Covers: rawText == null
        // Covers: actualRawText.trim().isEmpty()
        // Covers: isActualTransactionActive() == false

        try (MockedStatic<EncryptionUtil> encryptionUtil = mockStatic(EncryptionUtil.class);
             MockedStatic<TransactionSynchronizationManager> txManager = mockStatic(TransactionSynchronizationManager.class)) {

            // When rawText is null, actualRawText becomes "". Encrypt "" -> "encrypted"
            encryptionUtil.when(() -> EncryptionUtil.encrypt("", user.getPasswordHash()))
                    .thenReturn("encrypted");

            txManager.when(TransactionSynchronizationManager::isActualTransactionActive).thenReturn(false);

            JournalEntry savedEntry = new JournalEntry();
            savedEntry.setId(UUID.randomUUID());
            savedEntry.setRawText("encrypted");
            when(journalEntryRepository.save(any(JournalEntry.class))).thenReturn(savedEntry);

            // Act with NULL text
            JournalEntry result = journalPersistenceService.saveJournalEntry(user, null);

            // Assert
            assertThat(result.getWordCount()).isZero();

            // Verify async analysis was called directly (because tx is inactive)
            verify(asyncJournalAnalysisService).analyzeJournalEntryAsync(eq(savedEntry.getId()), eq(""), eq(user.getId()));
        }
    }

    @Test
    void updateJournalEntry_shouldHandleNullText_unchangedText_encryptionFails_and_inactiveTx() {
        // Covers: updatedText == null
        // Covers: textContentChanged == false
        // Covers: encryptedText == null fallback
        // Covers: isActualTransactionActive() == false

        UUID entryId = UUID.randomUUID();
        JournalEntry existingEntry = new JournalEntry();
        existingEntry.setId(entryId);
        existingEntry.setUser(user);
        existingEntry.setRawText("old encrypted");

        try (MockedStatic<EncryptionUtil> encryptionUtil = mockStatic(EncryptionUtil.class);
             MockedStatic<TransactionSynchronizationManager> txManager = mockStatic(TransactionSynchronizationManager.class)) {

            // 1. First scenario: No changes to text (textContentChanged == false)
            encryptionUtil.when(() -> EncryptionUtil.decrypt("old encrypted", user.getPasswordHash())).thenReturn("Same Text");
            encryptionUtil.when(() -> EncryptionUtil.encrypt("Same Text", user.getPasswordHash())).thenReturn("old encrypted");

            when(journalEntryRepository.findByIdWithDetails(entryId)).thenReturn(Optional.of(existingEntry));
            when(journalEntryRepository.save(any(JournalEntry.class))).thenReturn(existingEntry);

            journalPersistenceService.updateJournalEntry(entryId, user, "Same Text");
            // Verify async analysis NOT called since text didn't change
            verify(asyncJournalAnalysisService, never()).analyzeJournalEntryAsync(any(), any(), any());

            // 2. Second scenario: Null updatedText, Encryption fails, Inactive Tx
            encryptionUtil.when(() -> EncryptionUtil.decrypt("old encrypted", user.getPasswordHash())).thenReturn("Old Text");
            encryptionUtil.when(() -> EncryptionUtil.encrypt("", user.getPasswordHash())).thenReturn(null); // Encryption fails
            txManager.when(TransactionSynchronizationManager::isActualTransactionActive).thenReturn(false);

            // Act with NULL updatedText
            JournalEntry result = journalPersistenceService.updateJournalEntry(entryId, user, null);

            // Verify fallback to plain text ""
            assertThat(result.getRawText()).isEmpty();
            assertThat(result.getWordCount()).isZero();

            // Verify async analysis called directly (inactive tx)
            verify(asyncJournalAnalysisService).analyzeJournalEntryAsync(eq(entryId), eq(""), eq(user.getId()));
        }
    }

    @Test
    void readQueries_shouldHandleEmptyPasswordHash_and_nullCollections() {
        // Covers: userSecret.isEmpty() == true across all query methods
        // Covers: getKeyPhrases() == null and getMoodScore() == null in loops

        user.setPasswordHash(""); // EMPTY STRING (not null)

        JournalEntry entry = new JournalEntry();
        entry.setId(UUID.randomUUID());
        entry.setRawText("encrypted");
        entry.setUser(user);
        entry.setKeyPhrases(null); // Ensures Hibernate.initialize() is skipped
        entry.setMoodScore(null);  // Ensures filter skips it in mood chart

        LocalDate start = LocalDate.now();
        LocalDate end = LocalDate.now();
        Pageable pageable = PageRequest.of(0, 10);
        Page<JournalEntry> page = new PageImpl<>(List.of(entry));

        // Mock repositories
        when(journalEntryRepository.findByUserAndEntryDateBetweenOrderByCreationTimestampDesc(user, start, end))
                .thenReturn(List.of(entry));
        when(journalEntryRepository.findByIdWithDetails(entry.getId())).thenReturn(Optional.of(entry));
        when(journalEntryRepository.findByUserAndEntryDateBetween(user, start, end)).thenReturn(List.of(entry));
        when(journalEntryRepository.findByUser(user)).thenReturn(List.of(entry));
        when(journalEntryRepository.findByUserAndEntryDateBetweenOrderByCreationTimestampDesc(user, start, end, pageable))
                .thenReturn(page);

        // Mock journalMapper to avoid NullPointerExceptions during page response building
        when(journalMapper.toResponse(any())).thenReturn(null);

        // Fire all read queries to trigger the `isEmpty()` checks!
        journalPersistenceService.getJournalEntriesForUser(user, start, end);
        journalPersistenceService.getJournalEntryById(entry.getId());
        journalPersistenceService.getMoodDataForChart(user, start, end);
        journalPersistenceService.findByUser(user);
        journalPersistenceService.getJournalEntriesPageResponse(user, start, end, pageable);

        // Verification: Since password hash was empty, none of the text should be decrypted.
        // It successfully logged an error and returned the encrypted text safely.
        assertThat(entry.getRawText()).isEqualTo("encrypted");
    }

    @Test
    void resetAiFields_shouldClearExistingKeyPhrases_whenNotNull() {
        // Covers: entry.getKeyPhrases() != null branch in resetAiFields
        JournalEntry entry = new JournalEntry();
        List<KeyPhrase> existingPhrases = new java.util.ArrayList<>();
        existingPhrases.add(new KeyPhrase());
        entry.setKeyPhrases(existingPhrases);

        journalPersistenceService.resetAiFields(entry);

        assertThat(entry.getKeyPhrases()).isEmpty();
    }

    // ========================================================================
    // FINAL JACOCO SWEEP: Missing Lambdas and Null/Empty Boolean Branches
    // ========================================================================

    @Test
    void deleteJournalEntry_shouldThrowException_whenEntryNotFound() {
        // Covers the hidden lambda inside .orElseThrow(...)
        UUID entryId = UUID.randomUUID();
        when(journalEntryRepository.findById(entryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> journalPersistenceService.deleteJournalEntry(entryId, user))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Journal entry not found with ID: " + entryId);
    }

    @Test
    void saveJournalEntry_shouldThrowException_whenUserSecretIsEmptyString() {
        // Covers the FALSE || TRUE branch of: if (userSecret == null || userSecret.isEmpty())
        user.setPasswordHash(""); // Explicitly an empty string, not null

        assertThatThrownBy(() -> journalPersistenceService.saveJournalEntry(user, "Test text"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("User secret (password hash) not available for encryption.");
    }

    @Test
    void getJournalEntriesPageResponse_shouldHandleNullPasswordHash() {
        // Covers the FALSE && ? branch of: if (userSecret != null && !userSecret.isEmpty())
        user.setPasswordHash(null); // Explicitly null

        LocalDate start = LocalDate.now();
        LocalDate end = LocalDate.now();
        Pageable pageable = PageRequest.of(0, 10);

        JournalEntry entry = new JournalEntry();
        entry.setId(UUID.randomUUID());
        entry.setRawText("encryptedText");
        entry.setUser(user);

        Page<JournalEntry> page = new PageImpl<>(List.of(entry));

        when(journalEntryRepository.findByUserAndEntryDateBetweenOrderByCreationTimestampDesc(user, start, end, pageable))
                .thenReturn(page);

        // Mock journalMapper to avoid NullPointerExceptions
        when(journalMapper.toResponse(any())).thenReturn(null);

        // Execute
        journalPersistenceService.getJournalEntriesPageResponse(user, start, end, pageable);

        // Verify it safely bypassed decryption
        assertThat(entry.getRawText()).isEqualTo("encryptedText");
    }
}