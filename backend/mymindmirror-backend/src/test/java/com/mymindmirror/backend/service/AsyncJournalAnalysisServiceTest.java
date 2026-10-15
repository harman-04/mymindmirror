package com.mymindmirror.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mymindmirror.backend.enums.AITask;
import com.mymindmirror.backend.model.JournalEntry;
import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.payload.response.JournalAnalysisResponse;
import com.mymindmirror.backend.repository.JournalEntryRepository;
import com.mymindmirror.backend.service.ai.DynamicAiClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AsyncJournalAnalysisServiceTest {

    @Mock
    private JournalEntryRepository journalEntryRepository;

    @Mock
    private UserService userService;

    @Mock
    private DynamicAiClientService aiClientService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private EmbeddingGenerationService embeddingGenerationService;

    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private AsyncJournalAnalysisService asyncJournalAnalysisService;

    private UUID entryId;
    private UUID userId;
    private String decryptedRawText;
    private User user;
    private JournalEntry entry;

    @BeforeEach
    void setUp() {
        entryId = UUID.randomUUID();
        userId = UUID.randomUUID();
        decryptedRawText = "Today was a great day";
        user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        entry = new JournalEntry();
        entry.setId(entryId);
        entry.setUser(user);
        entry.setRawText("encrypted");
        entry.setWordCount(5);
    }

    // ========== Success Path ==========

    @Test
    void analyzeJournalEntryAsync_shouldUpdateEntryAndGenerateEmbedding_whenAIAnalysisSucceeds() {
        // Given
        JournalAnalysisResponse analysisResponse = new JournalAnalysisResponse(
                Map.of("joy", 0.8, "sadness", 0.2),
                List.of("work", "health"),
                "The user had a productive day",
                List.of("Keep up the good work"),
                List.of("productive day", "good work")
        );

        when(userService.findById(userId)).thenReturn(Optional.of(user));
        when(journalEntryRepository.findByIdWithDetails(entryId)).thenReturn(Optional.of(entry));

        when(aiClientService.generateStructured(anyString(), eq(JournalAnalysisResponse.class), eq(userId), eq(AITask.JOURNAL_ANALYSIS)))
                .thenReturn(analysisResponse);

        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<JournalEntry> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
        when(journalEntryRepository.save(any(JournalEntry.class))).thenReturn(entry);

        // When
        asyncJournalAnalysisService.analyzeJournalEntryAsync(entryId, decryptedRawText, userId);

        // Then
        ArgumentCaptor<JournalEntry> entryCaptor = ArgumentCaptor.forClass(JournalEntry.class);
        verify(journalEntryRepository).save(entryCaptor.capture());
        JournalEntry savedEntry = entryCaptor.getValue();

        //  Use tolerance for floating-point
        assertThat(savedEntry.getMoodScore()).isCloseTo(0.6, within(1e-9));
        assertThat(savedEntry.getSummary()).isEqualTo("The user had a productive day");
        verify(embeddingGenerationService).updateEmbedding(entry, userId);
    }

    // ========== AI Failure Fallback ==========

    @Test
    void analyzeJournalEntryAsync_shouldUseFallbackAnalysis_whenAIFails() {
        // Given
        when(userService.findById(userId)).thenReturn(Optional.of(user));
        when(journalEntryRepository.findByIdWithDetails(entryId)).thenReturn(Optional.of(entry));

        when(aiClientService.generateStructured(anyString(), eq(JournalAnalysisResponse.class), eq(userId), eq(AITask.JOURNAL_ANALYSIS)))
                .thenThrow(new RuntimeException("AI error"));

        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<JournalEntry> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
        when(journalEntryRepository.save(any(JournalEntry.class))).thenReturn(entry);

        // When
        asyncJournalAnalysisService.analyzeJournalEntryAsync(entryId, decryptedRawText, userId);

        // Then
        ArgumentCaptor<JournalEntry> entryCaptor = ArgumentCaptor.forClass(JournalEntry.class);
        verify(journalEntryRepository).save(entryCaptor.capture());
        JournalEntry savedEntry = entryCaptor.getValue();
        assertThat(savedEntry.getSummary()).isEqualTo("AI analysis temporarily unavailable.");
        assertThat(savedEntry.getMoodScore()).isCloseTo(0.0, within(1e-9));
        verify(embeddingGenerationService).updateEmbedding(entry, userId);
    }

    // ========== User Not Found ==========

    @Test
    void analyzeJournalEntryAsync_shouldAbort_whenUserNotFound() {
        // Given
        when(userService.findById(userId)).thenReturn(Optional.empty());

        // When
        asyncJournalAnalysisService.analyzeJournalEntryAsync(entryId, decryptedRawText, userId);

        // Then
        verify(journalEntryRepository, never()).findByIdWithDetails(any());
        verify(aiClientService, never()).generateStructured(any(), any(), any(), any());
        verify(embeddingGenerationService, never()).updateEmbedding(any(), any());
    }

    // ========== Entry Not Found ==========

    @Test
    void analyzeJournalEntryAsync_shouldHandleEntryNotFound() {
        // Given – AI succeeds, but entry disappears before saving
        when(userService.findById(userId)).thenReturn(Optional.of(user));
        when(aiClientService.generateStructured(anyString(), eq(JournalAnalysisResponse.class), eq(userId), eq(AITask.JOURNAL_ANALYSIS)))
                .thenReturn(new JournalAnalysisResponse(Map.of(), List.of(), "Summary", List.of(), List.of()));

        // Simulate transaction failing because entry not found inside transaction
        when(transactionTemplate.execute(any())).thenThrow(new RuntimeException("Entry disappeared before saving"));

        // When
        asyncJournalAnalysisService.analyzeJournalEntryAsync(entryId, decryptedRawText, userId);

        // Then – embedding is NOT called
        verify(embeddingGenerationService, never()).updateEmbedding(any(), any());
        // AI was called (we don't need to verify, but we can)
        verify(aiClientService).generateStructured(anyString(), eq(JournalAnalysisResponse.class), eq(userId), eq(AITask.JOURNAL_ANALYSIS));
    }

    // ========== Transaction Save Failure ==========

    @Test
    void analyzeJournalEntryAsync_shouldHandleException_whenSaveFails() {
        // Given
        when(userService.findById(userId)).thenReturn(Optional.of(user));
        when(aiClientService.generateStructured(anyString(), eq(JournalAnalysisResponse.class), eq(userId), eq(AITask.JOURNAL_ANALYSIS)))
                .thenReturn(new JournalAnalysisResponse(Map.of(), List.of(), "Summary", List.of(), List.of()));

        // Simulate transaction failure
        when(transactionTemplate.execute(any())).thenThrow(new RuntimeException("Save failed"));

        // When
        asyncJournalAnalysisService.analyzeJournalEntryAsync(entryId, decryptedRawText, userId);

        // Then – embedding generation is NOT called
        verify(embeddingGenerationService, never()).updateEmbedding(any(), any());
    }
    // Helper for floating-point tolerance
    private static org.assertj.core.data.Offset<Double> within(double tolerance) {
        return org.assertj.core.data.Offset.offset(tolerance);
    }

    @Test
    void analyzeJournalEntryAsync_shouldHandleJsonProcessingException_whenSerializing() throws Exception {
        // Given
        when(userService.findById(userId)).thenReturn(Optional.of(user));
        when(journalEntryRepository.findByIdWithDetails(entryId)).thenReturn(Optional.of(entry));

        JournalAnalysisResponse analysisResponse = new JournalAnalysisResponse(
                Map.of("joy", 0.8),
                List.of("work"),
                "Summary",
                List.of("Tip"),
                List.of("phrase")
        );
        when(aiClientService.generateStructured(anyString(), eq(JournalAnalysisResponse.class), eq(userId), eq(AITask.JOURNAL_ANALYSIS)))
                .thenReturn(analysisResponse);

        // Simulate JSON processing failure when writing emotions
        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("Mock serialization error") {});

        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<JournalEntry> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
        when(journalEntryRepository.save(any(JournalEntry.class))).thenReturn(entry);

        // When
        asyncJournalAnalysisService.analyzeJournalEntryAsync(entryId, decryptedRawText, userId);

        // Then – verify that serialization failure is handled (returned "{}") and embedding still called
        ArgumentCaptor<JournalEntry> entryCaptor = ArgumentCaptor.forClass(JournalEntry.class);
        verify(journalEntryRepository).save(entryCaptor.capture());
        JournalEntry savedEntry = entryCaptor.getValue();
        // The emotions field should be set to "{}" (fallback)
        assertThat(savedEntry.getEmotions()).isEqualTo("{}");
        verify(embeddingGenerationService).updateEmbedding(entry, userId);
    }

    @Test
    void analyzeJournalEntryAsync_shouldHandleEntryDisappearedDuringTransaction() {
        when(userService.findById(userId)).thenReturn(Optional.of(user));
        when(aiClientService.generateStructured(anyString(), eq(JournalAnalysisResponse.class), eq(userId), eq(AITask.JOURNAL_ANALYSIS)))
                .thenReturn(new JournalAnalysisResponse(Map.of(), List.of(), "S", List.of(), List.of()));

        // Trigger the orElseThrow inside transaction
        when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            TransactionCallback<JournalEntry> cb = inv.getArgument(0);
            when(journalEntryRepository.findByIdWithDetails(entryId)).thenReturn(Optional.empty());
            return cb.doInTransaction(null);
        });

        // Should not throw
        asyncJournalAnalysisService.analyzeJournalEntryAsync(entryId, decryptedRawText, userId);
    }
}