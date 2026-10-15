package com.mymindmirror.backend.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyList;

class JsonMapperHelperTest {

    private JsonMapperHelper helper;

    @BeforeEach
    void setUp() {
        // Real ObjectMapper — no mocking, we want to verify actual JSON behaviour
        helper = new JsonMapperHelper(new ObjectMapper());
    }

    // ============================================================
    // jsonToStringList()
    // ============================================================

    @Test
    @DisplayName("jsonToStringList() should parse valid JSON array")
    void jsonToStringList_shouldParseArray() {
        List<String> result = helper.jsonToStringList("[\"a\", \"b\", \"c\"]");
        assertThat(result).containsExactly("a", "b", "c");
    }

    @Test
    @DisplayName("jsonToStringList() should return empty list for empty array")
    void jsonToStringList_shouldHandleEmptyArray() {
        List<String> result = helper.jsonToStringList("[]");
        assertThat(result).isEmpty();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("jsonToStringList() should return empty list for null or blank input")
    void jsonToStringList_shouldHandleNullAndBlank(String input) {
        assertThat(helper.jsonToStringList(input)).isEmpty();
    }

    @Test
    @DisplayName("jsonToStringList() should return empty list on malformed JSON (no crash)")
    void jsonToStringList_shouldHandleMalformedJson() {
        assertThat(helper.jsonToStringList("not valid json")).isEmpty();
        assertThat(helper.jsonToStringList("{unclosed")).isEmpty();
        assertThat(helper.jsonToStringList("[1, 2, 3")).isEmpty();
    }

    @Test
    @DisplayName("jsonToStringList() should return empty list when JSON is an object (not array)")
    void jsonToStringList_shouldHandleNonArrayJson() {
        assertThat(helper.jsonToStringList("{\"key\":\"value\"}")).isEmpty();
    }

    @Test
    @DisplayName("jsonToStringList() should handle unicode strings")
    void jsonToStringList_shouldHandleUnicode() {
        List<String> result = helper.jsonToStringList("[\"नमस्ते\", \"🌍\", \"¡Hola!\"]");
        assertThat(result).containsExactly("नमस्ते", "🌍", "¡Hola!");
    }

    // ============================================================
    // stringListToJson()
    // ============================================================

    @Test
    @DisplayName("stringListToJson() should serialize a list to JSON")
    void stringListToJson_shouldSerializeList() {
        String json = helper.stringListToJson(List.of("a", "b", "c"));
        assertThat(json).isEqualTo("[\"a\",\"b\",\"c\"]");
    }

    @Test
    @DisplayName("stringListToJson() should return null for null list")
    void stringListToJson_shouldReturnNullForNull() {
        assertThat(helper.stringListToJson(null)).isNull();
    }

    @Test
    @DisplayName("stringListToJson() should return null for empty list")
    void stringListToJson_shouldReturnNullForEmptyList() {
        assertThat(helper.stringListToJson(List.of())).isNull();
    }

    @Test
    @DisplayName("stringListToJson() should handle unicode strings")
    void stringListToJson_shouldHandleUnicode() {
        String json = helper.stringListToJson(List.of("नमस्ते", "🌍"));
        assertThat(json).contains("नमस्ते").contains("🌍");
    }

    // ============================================================
    // jsonToMap()
    // ============================================================

    @Test
    @DisplayName("jsonToMap() should parse valid JSON object of doubles")
    void jsonToMap_shouldParseObject() {
        Map<String, Double> result = helper.jsonToMap("{\"joy\":0.5,\"sadness\":0.3}");
        assertThat(result).containsEntry("joy", 0.5).containsEntry("sadness", 0.3);
    }

    @Test
    @DisplayName("jsonToMap() should return empty map for empty object")
    void jsonToMap_shouldHandleEmptyObject() {
        assertThat(helper.jsonToMap("{}")).isEmpty();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("jsonToMap() should return empty map for null or blank input")
    void jsonToMap_shouldHandleNullAndBlank(String input) {
        assertThat(helper.jsonToMap(input)).isEmpty();
    }

    @Test
    @DisplayName("jsonToMap() should return empty map on malformed JSON (no crash)")
    void jsonToMap_shouldHandleMalformedJson() {
        assertThat(helper.jsonToMap("not json")).isEmpty();
        assertThat(helper.jsonToMap("{unclosed")).isEmpty();
    }

    @Test
    @DisplayName("jsonToMap() should return empty map when JSON is an array (not object)")
    void jsonToMap_shouldHandleArrayInput() {
        assertThat(helper.jsonToMap("[\"a\", \"b\"]")).isEmpty();
    }

    // ============================================================
    // Round-trip consistency
    // ============================================================

    @Test
    @DisplayName("round-trip List → JSON → List preserves order and content")
    void roundTrip_listPreservesContent() {
        List<String> original = List.of("alpha", "beta", "gamma");
        String json = helper.stringListToJson(original);
        List<String> restored = helper.jsonToStringList(json);

        assertThat(restored).isEqualTo(original);
    }

    @Test
    @DisplayName("round-trip list with special chars survives JSON escaping")
    void roundTrip_listHandlesEscaping() {
        List<String> original = List.of("line1\nline2", "quote \"inside\"", "tab\there");
        String json = helper.stringListToJson(original);
        List<String> restored = helper.jsonToStringList(json);

        assertThat(restored).isEqualTo(original);
    }

    @Test
    @DisplayName("stringListToJson() should throw RuntimeException when serialization fails unexpectedly")
    void stringListToJson_shouldThrow_whenSerializationFails() throws Exception {
        ObjectMapper failingMapper = mock(ObjectMapper.class);
        when(failingMapper.writeValueAsString(anyList()))
                .thenThrow(new JsonProcessingException("simulated failure") {});

        JsonMapperHelper failingHelper = new JsonMapperHelper(failingMapper);

        assertThatThrownBy(() -> failingHelper.stringListToJson(List.of("a", "b")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to serialize list to JSON");
    }
}