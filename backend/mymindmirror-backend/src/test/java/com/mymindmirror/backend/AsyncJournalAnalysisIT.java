package com.mymindmirror.backend;

import com.mymindmirror.backend.enums.AITask;
import com.mymindmirror.backend.model.JournalEntry;
import com.mymindmirror.backend.payload.response.JournalAnalysisResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Async journal analysis — fires after commit, populates DB")
class AsyncJournalAnalysisIT extends AbstractIntegrationTest {

    private String aliceJwt;

    @BeforeEach
    void loginAlice() throws Exception {
        aliceJwt = registerAndLogin("alice", "password123");
    }

    /** Mock AI response with joy=1.0, giving moodScore = 1.0 after calculation. */
    private JournalAnalysisResponse joyAnalysis() {
        return new JournalAnalysisResponse(
                Map.of("joy", 1.0),
                List.of("work"),
                "Test summary",
                List.of("Tip 1"),
                List.of("phrase1", "phrase2")
        );
    }

    private JournalEntry findOnlyEntry() {
        List<JournalEntry> all = journalEntryRepository.findAll();
        if (all.isEmpty()) {
            return null;
        }
        UUID id = all.get(0).getId();
        // Second fetch joins keyPhrases and user — safe outside the transaction
        return journalEntryRepository.findByIdWithDetails(id).orElse(null);
    }

    private void waitForMoodScore() {
        await().atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(200))
                .until(() -> {
                    JournalEntry entry = findOnlyEntry();
                    return entry != null && entry.getMoodScore() != null;
                });
    }

    // =================================================================
    // Happy path — async analysis populates the entry
    // =================================================================

    @Test
    @DisplayName("Async analysis populates moodScore, summary, and keyPhrases")
    void asyncAnalysis_populatesEntry() throws Exception {
        when(aiClientService.generateStructured(
                anyString(), eq(JournalAnalysisResponse.class), any(), eq(AITask.JOURNAL_ANALYSIS)))
                .thenReturn(joyAnalysis());

        mockMvc.perform(post("/api/journal")
                        .header("Authorization", "Bearer " + aliceJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rawText\":\"Today I felt joy at work.\"}"))
                .andExpect(status().isCreated());

        waitForMoodScore();

        JournalEntry entry = findOnlyEntry();
        assertThat(entry).isNotNull();
        assertThat(entry.getMoodScore()).isEqualTo(1.0);          // joy=1.0 → mood=1.0
        assertThat(entry.getSummary()).isEqualTo("Test summary");
        assertThat(entry.getKeyPhrases()).hasSize(2);
        assertThat(entry.getWordCount()).isEqualTo(6);            // "Today I felt joy at work."
    }

    // =================================================================
    // Update path — changing text triggers re-analysis
    // =================================================================

    @Test
    @DisplayName("Updating journal text triggers re-analysis (AI called twice)")
    void updateEntry_withNewText_triggersReanalysis() throws Exception {
        when(aiClientService.generateStructured(
                anyString(), eq(JournalAnalysisResponse.class), any(), eq(AITask.JOURNAL_ANALYSIS)))
                .thenReturn(joyAnalysis());

        // 1. Create + wait for first analysis
        mockMvc.perform(post("/api/journal")
                        .header("Authorization", "Bearer " + aliceJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rawText\":\"First version\"}"))
                .andExpect(status().isCreated());

        waitForMoodScore();
        UUID entryId = findOnlyEntry().getId();

        // 2. Update with NEW text — textContentChanged=true triggers re-analysis
        mockMvc.perform(put("/api/journal/{id}", entryId)
                        .header("Authorization", "Bearer " + aliceJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rawText\":\"Completely different text with new meaning\"}"))
                .andExpect(status().isOk());

        waitForMoodScore();

        // 3. Verify AI was called exactly twice
        verify(aiClientService, times(2)).generateStructured(
                anyString(), eq(JournalAnalysisResponse.class), any(), eq(AITask.JOURNAL_ANALYSIS));
    }

    // =================================================================
    // Failure path — AI fails, async uses fallback, entry still saved
    // =================================================================

    @Test
    @DisplayName("AI failure uses fallback (no crash, entry still analyzed)")
    void asyncAnalysis_aiFailure_usesFallback() throws Exception {
        when(aiClientService.generateStructured(
                anyString(), eq(JournalAnalysisResponse.class), any(), eq(AITask.JOURNAL_ANALYSIS)))
                .thenThrow(new RuntimeException("AI unavailable"));

        mockMvc.perform(post("/api/journal")
                        .header("Authorization", "Bearer " + aliceJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rawText\":\"Analysis will fall back\"}"))
                .andExpect(status().isCreated());

        waitForMoodScore();

        JournalEntry entry = findOnlyEntry();
        assertThat(entry).isNotNull();
        // Fallback summary from AsyncJournalAnalysisService
        assertThat(entry.getSummary()).isEqualTo("AI analysis temporarily unavailable.");
        // Empty emotions → moodScore = 0.0
        assertThat(entry.getMoodScore()).isEqualTo(0.0);
    }
}