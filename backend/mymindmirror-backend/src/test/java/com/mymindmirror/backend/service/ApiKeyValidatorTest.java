package com.mymindmirror.backend.service;

import com.mymindmirror.backend.enums.ValidationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ApiKeyValidatorTest {

    // ============================================================
    // validate() — input guards
    // ============================================================

    @Test
    @DisplayName("validate() throws IllegalArgumentException for null key")
    void validate_shouldThrow_whenKeyIsNull() {
        ApiKeyValidator validator = new ApiKeyValidator();
        assertThatThrownBy(() -> validator.validate(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be blank");
    }

    @Test
    @DisplayName("validate() throws IllegalArgumentException for blank key")
    void validate_shouldThrow_whenKeyIsBlank() {
        ApiKeyValidator validator = new ApiKeyValidator();
        assertThatThrownBy(() -> validator.validate("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be blank");
    }

    // ============================================================
    // validate() — cascade logic
    // ============================================================

    @Test
    @DisplayName("validate() returns VERIFIED when the first model succeeds")
    void validate_shouldReturnVerified_whenFirstModelSucceeds() {
        ApiKeyValidator validator = new ApiKeyValidator() {
            @Override
            protected void callModel(String apiKey, String modelName) {
                // no-op: pretend the call succeeded
            }
        };

        assertThat(validator.validate("valid-key").status())
                .isEqualTo(ValidationStatus.VERIFIED);
    }

    @Test
    @DisplayName("validate() returns REJECTED immediately when first model throws auth failure")
    void validate_shouldReturnRejected_whenFirstModelThrowsAuthError() {
        AtomicInteger calls = new AtomicInteger();
        ApiKeyValidator validator = new ApiKeyValidator() {
            @Override
            protected void callModel(String apiKey, String modelName) {
                calls.incrementAndGet();
                throw new RuntimeException("401 Unauthorized");
            }
        };

        assertThat(validator.validate("bad-key").status())
                .isEqualTo(ValidationStatus.REJECTED);
        assertThat(calls.get()).as("should stop after first auth failure").isEqualTo(1);
    }

    @Test
    @DisplayName("validate() falls through to second model on non-auth failure, then VERIFIED")
    void validate_shouldTryNextModel_whenFirstFailsNonAuth_andReturnVerified_whenSecondSucceeds() {
        AtomicInteger calls = new AtomicInteger();
        ApiKeyValidator validator = new ApiKeyValidator() {
            @Override
            protected void callModel(String apiKey, String modelName) {
                if (calls.incrementAndGet() == 1) {
                    throw new RuntimeException("Failed to generate content");
                }
                // second call succeeds
            }
        };

        assertThat(validator.validate("valid-key").status())
                .isEqualTo(ValidationStatus.VERIFIED);
        assertThat(calls.get()).as("should try two models").isEqualTo(2);
    }

    @Test
    @DisplayName("validate() returns REJECTED when second model throws auth failure")
    void validate_shouldReturnRejected_whenSecondModelThrowsAuthError() {
        AtomicInteger calls = new AtomicInteger();
        ApiKeyValidator validator = new ApiKeyValidator() {
            @Override
            protected void callModel(String apiKey, String modelName) {
                if (calls.incrementAndGet() == 1) {
                    throw new RuntimeException("Failed to generate content");
                }
                throw new RuntimeException("403 Forbidden");
            }
        };

        assertThat(validator.validate("bad-key").status())
                .isEqualTo(ValidationStatus.REJECTED);
        assertThat(calls.get()).as("should try two models").isEqualTo(2);
    }

    @Test
    @DisplayName("validate() returns UNVERIFIED when all models throw non-auth failures")
    void validate_shouldReturnUnverified_whenAllModelsFailNonAuth() {
        AtomicInteger calls = new AtomicInteger();
        ApiKeyValidator validator = new ApiKeyValidator() {
            @Override
            protected void callModel(String apiKey, String modelName) {
                calls.incrementAndGet();
                throw new RuntimeException("Connection timed out");
            }
        };

        assertThat(validator.validate("some-key").status())
                .isEqualTo(ValidationStatus.UNVERIFIED);
        assertThat(calls.get()).as("should try exactly MAX_MODELS_TO_TRY models").isEqualTo(2);
    }

    // ============================================================
    // isAuthFailure() — string classification
    // ============================================================

    @ParameterizedTest
    @ValueSource(strings = {
            "401 Unauthorized",
            "403 Forbidden",
            "API key not valid. Please pass a valid API key.",
            "API_KEY_INVALID",
            "invalid authentication credentials",
            "Unauthenticated",
            "Permission denied",
            "PERMISSION_DENIED",
            "API key expired"
    })
    @DisplayName("isAuthFailure() returns true for known auth-error markers")
    void isAuthFailure_shouldReturnTrue_forAuthMarkers(String message) {
        ApiKeyValidator validator = new ApiKeyValidator();
        assertThat(validator.isAuthFailure(new RuntimeException(message))).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Failed to generate content",
            "Connection timed out",
            "Unknown error",
            "NullPointerException"
    })
    @DisplayName("isAuthFailure() returns false for non-auth errors")
    void isAuthFailure_shouldReturnFalse_forNonAuthErrors(String message) {
        ApiKeyValidator validator = new ApiKeyValidator();
        assertThat(validator.isAuthFailure(new RuntimeException(message))).isFalse();
    }

    @Test
    @DisplayName("isAuthFailure() walks the cause chain")
    void isAuthFailure_shouldWalkCauseChain() {
        ApiKeyValidator validator = new ApiKeyValidator();
        Throwable root = new RuntimeException("401 Unauthorized");
        Throwable wrapped = new RuntimeException("Failed to generate content", root);

        assertThat(validator.isAuthFailure(wrapped)).isTrue();
    }

    @Test
    @DisplayName("isAuthFailure() handles exceptions with null messages")
    void isAuthFailure_shouldHandleNullMessage() {
        ApiKeyValidator validator = new ApiKeyValidator();
        assertThat(validator.isAuthFailure(new RuntimeException())).isFalse();
    }

    // ============================================================
    // callModel() — Integration Builder Layer Checks
    // ============================================================

    @Test
    @DisplayName("callModel() successfully finishes execution when underlying chat engine returns a safe response")
    void callModel_shouldCompleteSuccessfully_whenChatResponseIsValid() {
        ApiKeyValidator validator = new ApiKeyValidator();

        // Mock the creation pipeline of GoogleGenAiChatModel using Mockito-Inline
        try (org.mockito.MockedConstruction<org.springframework.ai.google.genai.GoogleGenAiChatModel> mockedModel =
                     org.mockito.Mockito.mockConstruction(org.springframework.ai.google.genai.GoogleGenAiChatModel.class,
                             (mock, context) -> {
                                 org.springframework.ai.chat.model.ChatResponse mockResponse = org.mockito.Mockito.mock(org.springframework.ai.chat.model.ChatResponse.class);
                                 org.springframework.ai.chat.model.Generation mockGeneration = org.mockito.Mockito.mock(org.springframework.ai.chat.model.Generation.class);

                                 org.mockito.Mockito.when(mockResponse.getResults()).thenReturn(java.util.List.of(mockGeneration));
                                 org.mockito.Mockito.when(mock.call(org.mockito.Mockito.any(org.springframework.ai.chat.prompt.Prompt.class))).thenReturn(mockResponse);
                             })) {

            assertDoesNotThrow(() ->
                    validator.callModel("fake-valid-key", "gemini-3.5-flash-lite")
            );
        }
    }

    @Test
    @DisplayName("callModel() throws RuntimeException when underlying chat engine returns an empty response payload")
    void callModel_shouldThrowException_whenChatResponseIsEmpty() {
        ApiKeyValidator validator = new ApiKeyValidator();

        try (org.mockito.MockedConstruction<org.springframework.ai.google.genai.GoogleGenAiChatModel> mockedModel =
                     org.mockito.Mockito.mockConstruction(org.springframework.ai.google.genai.GoogleGenAiChatModel.class,
                             (mock, context) -> {
                                 org.springframework.ai.chat.model.ChatResponse mockResponse = org.mockito.Mockito.mock(org.springframework.ai.chat.model.ChatResponse.class);

                                 // Force an empty results list back
                                 org.mockito.Mockito.when(mockResponse.getResults()).thenReturn(java.util.Collections.emptyList());
                                 org.mockito.Mockito.when(mock.call(org.mockito.Mockito.any(org.springframework.ai.chat.prompt.Prompt.class))).thenReturn(mockResponse);
                             })) {

            assertThatThrownBy(() -> validator.callModel("fake-valid-key", "gemini-3.5-flash-lite"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Empty response from model");
        }
    }

    @Test
    @DisplayName("callModel() throws RuntimeException when underlying chat engine returns null response")
    void callModel_shouldThrowException_whenChatResponseIsNull() {
        ApiKeyValidator validator = new ApiKeyValidator();

        try (org.mockito.MockedConstruction<org.springframework.ai.google.genai.GoogleGenAiChatModel> mockedModel =
                     org.mockito.Mockito.mockConstruction(org.springframework.ai.google.genai.GoogleGenAiChatModel.class,
                             (mock, context) -> {
                                 // Force the entire response object to be null
                                 org.mockito.Mockito.when(mock.call(org.mockito.Mockito.any(org.springframework.ai.chat.prompt.Prompt.class)))
                                         .thenReturn(null);
                             })) {

            assertThatThrownBy(() -> validator.callModel("fake-valid-key", "gemini-3.5-flash-lite"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Empty response from model");
        }
    }

    @Test
    @DisplayName("callModel() throws RuntimeException when underlying chat engine returns null results list")
    void callModel_shouldThrowException_whenChatResultsAreNull() {
        ApiKeyValidator validator = new ApiKeyValidator();

        try (org.mockito.MockedConstruction<org.springframework.ai.google.genai.GoogleGenAiChatModel> mockedModel =
                     org.mockito.Mockito.mockConstruction(org.springframework.ai.google.genai.GoogleGenAiChatModel.class,
                             (mock, context) -> {
                                 org.springframework.ai.chat.model.ChatResponse mockResponse = org.mockito.Mockito.mock(org.springframework.ai.chat.model.ChatResponse.class);

                                 // Force the results list to be null
                                 org.mockito.Mockito.when(mockResponse.getResults()).thenReturn(null);
                                 org.mockito.Mockito.when(mock.call(org.mockito.Mockito.any(org.springframework.ai.chat.prompt.Prompt.class))).thenReturn(mockResponse);
                             })) {

            assertThatThrownBy(() -> validator.callModel("fake-valid-key", "gemini-3.5-flash-lite"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Empty response from model");
        }
    }
}