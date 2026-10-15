package com.mymindmirror.backend.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mymindmirror.backend.model.JournalEntry;
import com.mymindmirror.backend.model.KeyPhrase;
import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.payload.response.JournalEntryResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(JournalMapperTest.TestConfig.class)
class JournalMapperTest {

    @Configuration
    @ComponentScan(basePackages = "com.mymindmirror.backend.mapper")
    static class TestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @Autowired
    private JournalMapper journalMapper;

    // ============================================================
    // Helper builder
    // ============================================================

    private JournalEntry buildFullEntry() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPasswordHash("hashed");

        JournalEntry entry = new JournalEntry();
        entry.setId(UUID.randomUUID());
        entry.setUser(user);
        entry.setEntryDate(LocalDate.of(2026, 9, 10));
        entry.setCreationTimestamp(LocalDateTime.of(2026, 9, 10, 14, 30, 0));
        entry.setRawText("Today was a good day.");
        entry.setMoodScore(0.75);
        entry.setEmotions("{\"joy\":0.7,\"sadness\":0.3}");
        entry.setCoreConcerns("[\"work\",\"health\"]");
        entry.setSummary("A positive day with mild worries.");
        entry.setGrowthTips("[\"tip one\",\"tip two\"]");
        entry.setKeyPhrases(new ArrayList<>(List.of(
                new KeyPhrase("positive mood", entry),
                new KeyPhrase("work progress", entry)
        )));
        return entry;
    }

    // ============================================================
    // Happy path
    // ============================================================

    @Test
    @DisplayName("toResponse() should map a fully-populated entity correctly")
    void toResponse_shouldMapAllFields() {
        JournalEntry entry = buildFullEntry();

        JournalEntryResponse response = journalMapper.toResponse(entry);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(entry.getId());
        assertThat(response.entryDate()).isEqualTo(LocalDate.of(2026, 9, 10));
        assertThat(response.creationTimestamp()).isEqualTo(LocalDateTime.of(2026, 9, 10, 14, 30));
        assertThat(response.rawText()).isEqualTo("Today was a good day.");
        assertThat(response.moodScore()).isEqualTo(0.75);
        assertThat(response.summary()).isEqualTo("A positive day with mild worries.");
    }

    @Test
    @DisplayName("toResponse() should deserialize emotions JSON into a Map")
    void toResponse_shouldDeserializeEmotionsMap() {
        JournalEntry entry = buildFullEntry();

        JournalEntryResponse response = journalMapper.toResponse(entry);

        assertThat(response.emotions())
                .containsEntry("joy", 0.7)
                .containsEntry("sadness", 0.3);
    }

    @Test
    @DisplayName("toResponse() should deserialize coreConcerns JSON into a List")
    void toResponse_shouldDeserializeCoreConcerns() {
        JournalEntry entry = buildFullEntry();

        JournalEntryResponse response = journalMapper.toResponse(entry);

        assertThat(response.coreConcerns()).containsExactly("work", "health");
    }

    @Test
    @DisplayName("toResponse() should deserialize growthTips JSON into a List")
    void toResponse_shouldDeserializeGrowthTips() {
        JournalEntry entry = buildFullEntry();

        JournalEntryResponse response = journalMapper.toResponse(entry);

        assertThat(response.growthTips()).containsExactly("tip one", "tip two");
    }

    @Test
    @DisplayName("toResponse() should extract phrases from keyPhrases entities")
    void toResponse_shouldExtractKeyPhraseStrings() {
        JournalEntry entry = buildFullEntry();

        JournalEntryResponse response = journalMapper.toResponse(entry);

        assertThat(response.keyPhrases()).containsExactly("positive mood", "work progress");
    }

    @Test
    @DisplayName("toResponse() should map the nested user to UserResponse")
    void toResponse_shouldMapNestedUser() {
        JournalEntry entry = buildFullEntry();

        JournalEntryResponse response = journalMapper.toResponse(entry);

        assertThat(response.user()).isNotNull();
        assertThat(response.user().id()).isEqualTo(entry.getUser().getId());
        assertThat(response.user().username()).isEqualTo("testuser");
        assertThat(response.user().email()).isEqualTo("test@example.com");
    }

    // ============================================================
    // Null handling — JSON fields
    // ============================================================

    @Test
    @DisplayName("toResponse() should return empty map when emotions is null")
    void toResponse_shouldReturnEmptyMap_whenEmotionsNull() {
        JournalEntry entry = buildFullEntry();
        entry.setEmotions(null);

        JournalEntryResponse response = journalMapper.toResponse(entry);

        assertThat(response.emotions()).isEmpty();
    }

    @Test
    @DisplayName("toResponse() should return empty list when coreConcerns is null")
    void toResponse_shouldReturnEmptyList_whenCoreConcernsNull() {
        JournalEntry entry = buildFullEntry();
        entry.setCoreConcerns(null);

        JournalEntryResponse response = journalMapper.toResponse(entry);

        assertThat(response.coreConcerns()).isEmpty();
    }

    @Test
    @DisplayName("toResponse() should return empty list when growthTips is null")
    void toResponse_shouldReturnEmptyList_whenGrowthTipsNull() {
        JournalEntry entry = buildFullEntry();
        entry.setGrowthTips(null);

        JournalEntryResponse response = journalMapper.toResponse(entry);

        assertThat(response.growthTips()).isEmpty();
    }

    @Test
    @DisplayName("toResponse() should return empty list when keyPhrases is null")
    void toResponse_shouldReturnEmptyList_whenKeyPhrasesNull() {
        JournalEntry entry = buildFullEntry();
        entry.setKeyPhrases(null);

        JournalEntryResponse response = journalMapper.toResponse(entry);

        assertThat(response.keyPhrases()).isEmpty();
    }

    @Test
    @DisplayName("toResponse() should return empty list when keyPhrases is empty")
    void toResponse_shouldReturnEmptyList_whenKeyPhrasesEmpty() {
        JournalEntry entry = buildFullEntry();
        entry.setKeyPhrases(new ArrayList<>());

        JournalEntryResponse response = journalMapper.toResponse(entry);

        assertThat(response.keyPhrases()).isEmpty();
    }

    // ============================================================
    // Malformed JSON — graceful fallback (via JsonMapperHelper)
    // ============================================================

    @Test
    @DisplayName("toResponse() should return empty map on malformed emotions JSON")
    void toResponse_shouldHandleMalformedEmotions() {
        JournalEntry entry = buildFullEntry();
        entry.setEmotions("not json at all");

        JournalEntryResponse response = journalMapper.toResponse(entry);

        assertThat(response.emotions()).isEmpty();
    }

    @Test
    @DisplayName("toResponse() should return empty list on malformed coreConcerns JSON")
    void toResponse_shouldHandleMalformedCoreConcerns() {
        JournalEntry entry = buildFullEntry();
        entry.setCoreConcerns("[unclosed");

        JournalEntryResponse response = journalMapper.toResponse(entry);

        assertThat(response.coreConcerns()).isEmpty();
    }

    @Test
    @DisplayName("toResponse() should return empty list on malformed growthTips JSON")
    void toResponse_shouldHandleMalformedGrowthTips() {
        JournalEntry entry = buildFullEntry();
        entry.setGrowthTips("invalid-json");

        JournalEntryResponse response = journalMapper.toResponse(entry);

        assertThat(response.growthTips()).isEmpty();
    }

    // ============================================================
    // Edge cases — null simple fields
    // ============================================================

    @Test
    @DisplayName("toResponse() should pass through null rawText, summary, moodScore")
    void toResponse_shouldPassThroughNullFields() {
        JournalEntry entry = buildFullEntry();
        entry.setRawText(null);
        entry.setSummary(null);
        entry.setMoodScore(null);

        JournalEntryResponse response = journalMapper.toResponse(entry);

        assertThat(response.rawText()).isNull();
        assertThat(response.summary()).isNull();
        assertThat(response.moodScore()).isNull();
    }

    @Test
    @DisplayName("toResponse() should handle null user without crashing")
    void toResponse_shouldHandleNullUser() {
        JournalEntry entry = buildFullEntry();
        entry.setUser(null);

        JournalEntryResponse response = journalMapper.toResponse(entry);

        assertThat(response.user()).isNull();
    }

    // ============================================================
    // Unicode content
    // ============================================================

    @Test
    @DisplayName("toResponse() should preserve unicode in JSON fields")
    void toResponse_shouldPreserveUnicode() {
        JournalEntry entry = buildFullEntry();
        entry.setEmotions("{\"joy\":0.8,\"शांति\":0.2}");
        entry.setCoreConcerns("[\"नमस्ते\",\"🌍\"]");
        entry.setGrowthTips("[\"¡Hola!\",\"مرحبا\"]");

        JournalEntryResponse response = journalMapper.toResponse(entry);

        assertThat(response.emotions()).containsKey("शांति");
        assertThat(response.coreConcerns()).containsExactly("नमस्ते", "🌍");
        assertThat(response.growthTips()).containsExactly("¡Hola!", "مرحبا");
    }

    @Test
    @DisplayName("toResponse() should return null when entity is null")
    void toResponse_shouldReturnNull_whenEntryNull() {
        assertThat(journalMapper.toResponse(null)).isNull();
    }
}