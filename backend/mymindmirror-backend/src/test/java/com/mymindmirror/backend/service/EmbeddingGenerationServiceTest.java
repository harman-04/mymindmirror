package com.mymindmirror.backend.service;

import com.mymindmirror.backend.model.JournalEntry;
import com.mymindmirror.backend.model.KeyPhrase;
import com.mymindmirror.backend.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmbeddingGenerationServiceTest {

    @Mock
    private VectorStore vectorStore;

    @InjectMocks
    private EmbeddingGenerationService embeddingGenerationService;

    private UUID userId;
    private UUID entryId;
    private JournalEntry entry;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        entryId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        user.setUsername("testuser");

        entry = new JournalEntry();
        entry.setId(entryId);
        entry.setUser(user);
        entry.setSummary("This is a test summary.");
        entry.setMoodScore(0.8);
        entry.setKeyPhrases(List.of(
                new KeyPhrase("test phrase", entry),
                new KeyPhrase("another phrase", entry)
        ));
    }

    // ========== generateAndStoreEmbedding Tests ==========

    @Test
    void generateAndStoreEmbedding_shouldStoreDocument_whenEntryValid() {
        // Given
        // When
        embeddingGenerationService.generateAndStoreEmbedding(entry, userId);

        // Then
        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(captor.capture());
        List<Document> documents = captor.getValue();
        assertThat(documents).hasSize(1);
        Document doc = documents.get(0);
        assertThat(doc.getId()).isEqualTo(entryId.toString());
        assertThat(doc.getText()).contains("test summary");
        assertThat(doc.getText()).contains("Key phrases: test phrase, another phrase");
        assertThat(doc.getMetadata())
                .containsEntry("userId", userId.toString())
                .containsEntry("entryId", entryId.toString());
    }

    @Test
    void generateAndStoreEmbedding_shouldDoNothing_whenEntryIsNull() {
        // When
        embeddingGenerationService.generateAndStoreEmbedding(null, userId);

        // Then
        verify(vectorStore, never()).add(anyList());
    }

    @Test
    void generateAndStoreEmbedding_shouldDoNothing_whenEntryNotAnalyzed() {
        // Given – moodScore is null
        entry.setMoodScore(null);

        // When
        embeddingGenerationService.generateAndStoreEmbedding(entry, userId);

        // Then
        verify(vectorStore, never()).add(anyList());
    }

    @Test
    void generateAndStoreEmbedding_shouldSkipEmptyText() {
        // Given – empty summary and no key phrases
        entry.setSummary("");
        entry.setKeyPhrases(List.of());

        // When
        embeddingGenerationService.generateAndStoreEmbedding(entry, userId);

        // Then
        verify(vectorStore, never()).add(anyList());
    }

    @Test
    void generateAndStoreEmbedding_shouldLogError_whenVectorStoreThrowsException() {
        // Given
        doThrow(new RuntimeException("DB error")).when(vectorStore).add(anyList());

        // When – should not throw, just log
        embeddingGenerationService.generateAndStoreEmbedding(entry, userId);

        // Then – verify that the method didn't propagate the exception
        verify(vectorStore).add(anyList()); // still called
        // No assertion needed – method completes without exception.
    }

    // ========== deleteEmbedding Tests ==========

    @Test
    void deleteEmbedding_shouldDeleteDocument() {
        // Given
        // When
        embeddingGenerationService.deleteEmbedding(entryId);

        // Then
        verify(vectorStore).delete(List.of(entryId.toString()));
    }

    @Test
    void deleteEmbedding_shouldLogError_whenVectorStoreThrowsException() {
        // Given
        doThrow(new RuntimeException("Delete error")).when(vectorStore).delete(anyList());

        // When – should not throw
        embeddingGenerationService.deleteEmbedding(entryId);

        // Then
        verify(vectorStore).delete(anyList());
    }

    // ========== updateEmbedding Tests ==========

    @Test
    void updateEmbedding_shouldDeleteAndGenerate() {
        // Given
        // When
        embeddingGenerationService.updateEmbedding(entry, userId);

        // Then
        // Verify delete was called first
        verify(vectorStore).delete(List.of(entryId.toString()));
        // Verify generate was called
        verify(vectorStore).add(anyList());
    }

    @Test
    void updateEmbedding_shouldHandleDeleteFailureAndStillGenerate() {
        // Given – delete fails, but should still try to generate
        doThrow(new RuntimeException("Delete error")).when(vectorStore).delete(anyList());

        // When
        embeddingGenerationService.updateEmbedding(entry, userId);

        // Then – delete called, and add also called
        verify(vectorStore).delete(anyList());
        verify(vectorStore).add(anyList());
    }

    @Test
    void updateEmbedding_shouldNotGenerate_whenEntryInvalid() {
        // Given – entry not analyzed
        entry.setMoodScore(null);

        // When
        embeddingGenerationService.updateEmbedding(entry, userId);

        // Then – delete is called, but add is not
        verify(vectorStore).delete(List.of(entryId.toString()));
        verify(vectorStore, never()).add(anyList());
    }

    // ========== getVectorStore Tests ==========

    @Test
    void getVectorStore_shouldReturnTheMock() {
        // When
        VectorStore result = embeddingGenerationService.getVectorStore();

        // Then
        assertThat(result).isSameAs(vectorStore);
    }

    @Test
    @DisplayName("buildTextForEmbedding: null summary + key phrases → only key phrases, no leading space")
    void buildTextForEmbedding_whenSummaryNullButKeyPhrasesPresent() {
        // Given
        entry.setSummary(null);
        entry.setKeyPhrases(List.of(
                new KeyPhrase("phrase one", entry),
                new KeyPhrase("phrase two", entry)
        ));

        // When
        embeddingGenerationService.generateAndStoreEmbedding(entry, userId);

        // Then
        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(captor.capture());
        Document doc = captor.getValue().get(0);

        // No leading space before "Key phrases"
        assertThat(doc.getText()).isEqualTo("Key phrases: phrase one, phrase two");
        assertThat(doc.getText()).doesNotStartWith(" ");
    }

    @Test
    @DisplayName("buildTextForEmbedding: null key phrases → only summary")
    void buildTextForEmbedding_whenKeyPhrasesNull() {
        // Given
        entry.setSummary("Just a summary, no key phrases.");
        entry.setKeyPhrases(null);

        // When
        embeddingGenerationService.generateAndStoreEmbedding(entry, userId);

        // Then
        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(captor.capture());
        Document doc = captor.getValue().get(0);

        assertThat(doc.getText()).isEqualTo("Just a summary, no key phrases.");
        assertThat(doc.getText()).doesNotContain("Key phrases:");
    }
}