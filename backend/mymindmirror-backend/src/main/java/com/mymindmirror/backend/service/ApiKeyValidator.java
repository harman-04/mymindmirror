package com.mymindmirror.backend.service;

import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import com.mymindmirror.backend.enums.AITask;
import com.mymindmirror.backend.enums.ValidationStatus;
import com.mymindmirror.backend.payload.response.ValidationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Validates a user's Gemini API key by making a single minimal chat call
 * against the first two models in the {@link AITask#JOURNAL_ANALYSIS} cascade.
 * Reusing the cascade means validation survives Google's model deprecation
 * cycle — if the first model is retired, the next is tried.
 *
 * <p>Three outcomes:
 * <ul>
 *   <li>{@code VERIFIED} — the call succeeded</li>
 *   <li>{@code REJECTED} — Google returned 401/403, the key is bad</li>
 *   <li>{@code UNVERIFIED} — could not verify (timeout, network, all
 *       models returned non-auth errors). The key is still saved.</li>
 * </ul>
 */
@Service
@Slf4j
public class ApiKeyValidator {

    private static final String TEST_PROMPT = "Reply with the single word: OK";
    private static final int MAX_MODELS_TO_TRY = 2;

    private static final int CONNECT_TIMEOUT_SECONDS = 3;
    private static final int READ_TIMEOUT_SECONDS = 4;

    public ValidationResult validate(String plainApiKey) {
        if (plainApiKey == null || plainApiKey.isBlank()) {
            throw new IllegalArgumentException("API key must not be blank");
        }

        List<String> cascade = AITask.JOURNAL_ANALYSIS.getModelCascade();
        int attempts = Math.min(MAX_MODELS_TO_TRY, cascade.size());

        for (int i = 0; i < attempts; i++) {
            String modelName = cascade.get(i);
            try {
                callModel(plainApiKey, modelName);
                log.info("API key validation succeeded with model {}", modelName);
                return new ValidationResult(ValidationStatus.VERIFIED);
            } catch (Exception e) {
                if (isAuthFailure(e)) {
                    log.warn("API key rejected by Google during validation: {}", e.getMessage());
                    return new ValidationResult(ValidationStatus.REJECTED);
                }
                log.warn("Validation with model {} failed (non-auth), trying next: {}",
                        modelName, e.getMessage());
            }
        }

        log.warn("API key validation could not be completed within timeout budget");
        return new ValidationResult(ValidationStatus.UNVERIFIED);
    }

    /**
     * Package-private to allow unit testing of the cascade behavior via subclassing.
     * In production this method is only invoked from {@link #validate(String)}.
     */
    protected void callModel(String apiKey, String modelName) {
        int timeoutMs = (CONNECT_TIMEOUT_SECONDS + READ_TIMEOUT_SECONDS) * 1000;

        HttpOptions httpOptions = HttpOptions.builder()
                .timeout(timeoutMs)
                .build();

        Client googleClient = Client.builder()
                .apiKey(apiKey)
                .httpOptions(httpOptions)
                .build();

        GoogleGenAiChatOptions options = GoogleGenAiChatOptions.builder()
                .model(modelName)
                .build();

        GoogleGenAiChatModel model = GoogleGenAiChatModel.builder()
                .genAiClient(googleClient)
                .defaultOptions(options)
                .build();

        ChatResponse response = model.call(new Prompt(TEST_PROMPT));
        if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
            throw new RuntimeException("Empty response from model " + modelName);
        }
    }

    /**
     * Package-private for direct unit testing. Walks the cause chain looking
     * for known auth-failure markers.
     */
    boolean isAuthFailure(Throwable t) {
        while (t != null) {
            String msg = t.getMessage();
            if (msg != null) {
                String lower = msg.toLowerCase();
                if (lower.contains("401") || lower.contains("403")
                        || lower.contains("api key not valid")
                        || lower.contains("api_key_invalid")
                        || lower.contains("invalid authentication")
                        || lower.contains("unauthenticated")
                        || lower.contains("permission denied")
                        || lower.contains("permission_denied")
                        || lower.contains("api key expired")) {
                    return true;
                }
            }
            t = t.getCause();
        }
        return false;
    }
}