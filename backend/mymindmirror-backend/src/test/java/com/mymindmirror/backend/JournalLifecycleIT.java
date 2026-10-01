package com.mymindmirror.backend;

import com.mymindmirror.backend.model.JournalEntry;
import com.mymindmirror.backend.payload.request.JournalEntryRequest;
import com.mymindmirror.backend.util.EncryptionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import com.mymindmirror.backend.enums.AITask;
import com.mymindmirror.backend.payload.response.JournalAnalysisResponse;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Journal lifecycle — encryption at rest, CRUD, filters, pagination, search")
class JournalLifecycleIT extends AbstractIntegrationTest {



    private String aliceJwt;

    @BeforeEach
    void loginAlice() throws Exception {
        aliceJwt = registerAndLogin("alice", "password123");
    }

    /**
     * All tests in this class create journal entries, which fire async analysis.
     * Provide a deterministic AI response so the async thread succeeds and
     * populates moodScore — otherwise the mock returns null and the async
     * service NPEs.
     */
    @BeforeEach
    void stubAiClient() {
        when(aiClientService.generateStructured(
                anyString(),
                eq(JournalAnalysisResponse.class),
                any(),
                eq(AITask.JOURNAL_ANALYSIS)))
                .thenReturn(new JournalAnalysisResponse(
                        Map.of("joy", 1.0),        // → moodScore = 1.0
                        List.of("test"),           // coreConcerns
                        "Test summary",            // summary
                        List.of("Tip 1"),          // growthTips
                        List.of("phrase1", "phrase2")  // keyPhrases
                ));
    }
    // =================================================================
    // Helpers
    // =================================================================

    /**
     * Reads the first stored entry WITH user + keyPhrases fetch-joined.
     * Required because rawText + user are needed outside a session.
     */
    private JournalEntry reloadFirstEntry() {
        List<JournalEntry> all = journalEntryRepository.findAll();
        if (all.isEmpty()) {
            return null;
        }
        return journalEntryRepository.findByIdWithDetails(all.get(0).getId()).orElse(null);
    }

    private UUID createEntry(String rawText) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/journal")
                        .header("Authorization", "Bearer " + aliceJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JournalEntryRequest(rawText))))
                .andExpect(status().isCreated())
                .andReturn();

        return UUID.fromString(objectMapper.readTree(
                result.getResponse().getContentAsString()).get("id").asText());
    }

    /** Waits for the async analysis to populate moodScore. */
    private void waitForAnalysis() {
        await().atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(200))
                .until(() -> {
                    List<JournalEntry> all = journalEntryRepository.findAll();
                    return !all.isEmpty() && all.get(0).getMoodScore() != null;
                });
    }

    // =================================================================
    // Create + encryption at rest
    // =================================================================

    @Test
    @DisplayName("Create: rawText stored encrypted, decryptable with user's password hash")
    void createEntry_storesEncryptedAtRest() throws Exception {
        String plaintext = "Today I tested encryption at rest.";

        createEntry(plaintext);

        JournalEntry stored = reloadFirstEntry();
        assertThat(stored).isNotNull();

        String encryptedRaw = stored.getRawText();

        // 1. Ciphertext is not the plaintext
        assertThat(encryptedRaw).isNotEqualTo(plaintext);
        assertThat(encryptedRaw).doesNotContain("encryption at rest");

        // 2. Ciphertext is decryptable with the user's secret
        String userSecret = stored.getUser().getPasswordHash();
        String decrypted = EncryptionUtil.decrypt(encryptedRaw, userSecret);
        assertThat(decrypted).isEqualTo(plaintext);
    }

    // =================================================================
    // Get by ID — decryption on read
    // =================================================================

    @Test
    @DisplayName("Get by ID: returns decrypted rawText")
    void getById_returnsDecryptedText() throws Exception {
        String plaintext = "Fetch me decrypted please.";
        UUID id = createEntry(plaintext);

        mockMvc.perform(get("/api/journal/{id}", id)
                        .header("Authorization", "Bearer " + aliceJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.rawText").value(plaintext));
    }

    @Test
    @DisplayName("Get by ID: random UUID → 404")
    void getById_randomUuid_returns404() throws Exception {
        mockMvc.perform(get("/api/journal/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + aliceJwt))
                .andExpect(status().isNotFound());
    }

    // =================================================================
    // Update — re-encryption
    // =================================================================

    @Test
    @DisplayName("Update: re-encrypts with new text (DB decrypts back to the update)")
    void updateEntry_reEncrypts() throws Exception {
        UUID id = createEntry("Original text");
        String newText = "Updated text with new content";

        mockMvc.perform(put("/api/journal/{id}", id)
                        .header("Authorization", "Bearer " + aliceJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JournalEntryRequest(newText))))
                .andExpect(status().isOk());

        // Verify the stored ciphertext decrypts to the new text
        JournalEntry stored = reloadFirstEntry();
        assertThat(stored).isNotNull();
        String decrypted = EncryptionUtil.decrypt(
                stored.getRawText(), stored.getUser().getPasswordHash());
        assertThat(decrypted).isEqualTo(newText);
    }

    // =================================================================
    // Delete
    // =================================================================

    @Test
    @DisplayName("Delete: removes entry from DB")
    void deleteEntry_removesFromDatabase() throws Exception {
        UUID id = createEntry("Delete me");

        // Let async analysis finish so it doesn't race with the delete
        waitForAnalysis();

        mockMvc.perform(delete("/api/journal/{id}", id)
                        .header("Authorization", "Bearer " + aliceJwt))
                .andExpect(status().isNoContent());

        assertThat(journalEntryRepository.findById(id)).isEmpty();
    }

    // =================================================================
    // History with date filter
    // =================================================================

    @Test
    @DisplayName("History: entries within requested range are returned")
    void historyWithDateFilter_returnsMatchingEntries() throws Exception {
        createEntry("Entry 1");
        createEntry("Entry 2");

        String today = LocalDate.now().toString();
        String tomorrow = LocalDate.now().plusDays(1).toString();

        mockMvc.perform(get("/api/journal/history")
                        .header("Authorization", "Bearer " + aliceJwt)
                        .param("startDate", today)
                        .param("endDate", tomorrow))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("History: future-only range returns empty")
    void historyWithFutureRange_returnsEmpty() throws Exception {
        createEntry("Present entry");

        String futureStart = LocalDate.now().plusDays(10).toString();
        String futureEnd = LocalDate.now().plusDays(20).toString();

        mockMvc.perform(get("/api/journal/history")
                        .header("Authorization", "Bearer " + aliceJwt)
                        .param("startDate", futureStart)
                        .param("endDate", futureEnd))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // =================================================================
    // Pagination
    // =================================================================

    @Test
    @DisplayName("Paginated history: correct page metadata and content slice")
    void paginatedHistory_returnsCorrectMetadata() throws Exception {
        for (int i = 0; i < 5; i++) {
            createEntry("Entry " + i);
        }

        mockMvc.perform(get("/api/journal/history/paginated")
                        .header("Authorization", "Bearer " + aliceJwt)
                        .param("page", "0")
                        .param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(3))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false));
    }

    // =================================================================
    // Search
    // =================================================================

    @Test
    @DisplayName("Search by keyword: matches decrypted content only")
    void searchByKeyword_matchesDecryptedContent() throws Exception {
        createEntry("I ate a delicious watermelon today.");
        createEntry("Routine day with no surprises.");

        mockMvc.perform(get("/api/journal/search/keyword")
                        .header("Authorization", "Bearer " + aliceJwt)
                        .param("keyword", "watermelon"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].rawText").value(containsString("watermelon")));
    }

    // =================================================================
    // Trends
    // =================================================================

    @Test
    @DisplayName("Trends endpoint: returns an object (empty or populated), never errors")
    void trendsEndpoint_returnsObject() throws Exception {
        createEntry("Consistent daily practice helps.");

        mockMvc.perform(get("/api/journal/trends")
                        .header("Authorization", "Bearer " + aliceJwt)
                        .param("limit", "5"))
                .andExpect(status().isOk());
    }
}