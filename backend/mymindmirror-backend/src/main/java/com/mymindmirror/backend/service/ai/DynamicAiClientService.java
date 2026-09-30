package com.mymindmirror.backend.service.ai;

import com.google.genai.Client;
import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.enums.AITask;
import com.mymindmirror.backend.service.ApiKeyService;
import com.mymindmirror.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class DynamicAiClientService {

    private final ApiKeyService apiKeyService;
    private final UserService userService;

    // Cache key format: "userId_modelName" -> Custom Initialized ChatModel
    private final Map<String, ChatModel> userModelCache = new ConcurrentHashMap<>();

    // Dynamic state tracker for rate-limited models: "modelName" -> Expiration Timestamp
    private final Map<String, Long> modelCooldownMap = new ConcurrentHashMap<>();

    private static final long COOLDOWN_DURATION_MS = TimeUnit.SECONDS.toMillis(60);
    private static final long DEPRECATED_COOLDOWN_MS = TimeUnit.HOURS.toMillis(24);

    @Value("${spring.ai.google.genai.api-key:}")
    private String globalApiKey;

    @Value("${spring.ai.google.genai.chat.options.temperature:0.4}")
    private Double defaultTemperature;

    public String generate(String prompt, UUID userId, AITask task) {
        Exception lastException = null;

        for (String modelName : task.getModelCascade()) {
            if (isCooldownLocked(modelName)) {
                log.info("FAST PATH: Skipping model [{}] due to active cooldown lock.", modelName);
                continue;
            }

            try {
                log.info("Calling AI model [{}] for Plain Text generation...", modelName);
                ChatModel model = getChatModel(userId, modelName);
                Prompt p = new Prompt(prompt);
                org.springframework.ai.chat.model.ChatResponse response = model.call(p);

                if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
                    throw new RuntimeException("Empty response returned from model.");
                }

                //  Handles Gemma/Thinking models by ALWAYS grabbing the final index (ignoring thoughts)
                int lastIndex = response.getResults().size() - 1;
                String finalAnswer = response.getResults().get(lastIndex).getOutput().getText();
                return finalAnswer != null ? finalAnswer.trim() : "";

            } catch (Exception e) {
                log.warn("Model [{}] failed for task {}. Error: {}. Cascading to next model...", modelName, task, e.getMessage());
                applyDynamicCooldown(modelName, e);
                lastException = e;
            }
        }

        log.error("CRITICAL: All models in cascade exhausted for task {}", task, lastException);
        return "I'm currently handling exceptionally high volume. Please give us a brief moment and try again.";
    }

    public <T> T generateStructured(String prompt, Class<T> responseType, UUID userId, AITask task) {
        Exception lastException = null;

        for (String modelName : task.getModelCascade()) {
            if (isCooldownLocked(modelName)) {
                log.info("FAST PATH: Skipping structured model [{}] due to active cooldown lock.", modelName);
                continue;
            }

            try {
                log.info("Calling AI model [{}] for Structured JSON generation...", modelName);
                ChatModel model = getChatModel(userId, modelName);
                return executeStructuredCall(model, prompt, responseType);
            } catch (Exception e) {
                log.warn("Model [{}] failed structured generation for task {}. Error: {}. Cascading to next model...", modelName, task, e.getMessage());
                applyDynamicCooldown(modelName, e);
                lastException = e;
            }
        }

        log.error("CRITICAL: All structured models in cascade exhausted for task {}", task, lastException);
        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                buildRateLimitMessage(userId),
                lastException);
    }

    private <T> T executeStructuredCall(ChatModel model, String prompt, Class<T> responseType) {
        BeanOutputConverter<T> converter = new BeanOutputConverter<>(responseType);

        //  Aggressive anti-hallucination prompt
        String structuredPromptText = prompt + "\n\n" +
                "CRITICAL INSTRUCTIONS FOR AI:\n" +
                "1. You MUST respond with a valid, perfectly formatted JSON object matching this schema:\n" +
                converter.getJsonSchema() + "\n" +
                "2. Return ONLY the raw JSON block. Do NOT use markdown formatting (like ```json).\n" +
                "3. Stop generating immediately after closing the final JSON bracket. Do NOT append extra trailing characters, brackets, or commas.";

        GoogleGenAiChatOptions jsonOptions = GoogleGenAiChatOptions.builder()
                .responseMimeType("application/json")
                .build();

        Prompt structuredPrompt = new Prompt(structuredPromptText, jsonOptions);
        org.springframework.ai.chat.model.ChatResponse chatResponse = model.call(structuredPrompt);

        if (chatResponse == null || chatResponse.getResults() == null || chatResponse.getResults().isEmpty()) {
            throw new RuntimeException("AI Model failed to return a valid response object.");
        }

        // 🔥 CRITICAL: Extract the actual answer, bypassing "Thoughts" from models like Gemma
        int lastIndex = chatResponse.getResults().size() - 1;
        String rawResponse = chatResponse.getResults().get(lastIndex).getOutput().getText();

        String cleanJson = extractJson(rawResponse);
        if (cleanJson == null) {
            throw new RuntimeException("Invalid response layout received from model. Structural parse failed.");
        }
        return converter.convert(cleanJson);
    }

    private boolean isCooldownLocked(String modelName) {
        Long blockExpiration = modelCooldownMap.get(modelName);
        if (blockExpiration != null) {
            if (System.currentTimeMillis() < blockExpiration) {
                return true;
            } else {
                modelCooldownMap.remove(modelName);
                log.info("Cooldown window expired for [{}]. Restoring to active routing pool.", modelName);
            }
        }
        return false;
    }

    private void applyDynamicCooldown(String modelName, Exception e) {
        long cooldownMs = COOLDOWN_DURATION_MS;
        if (e.getMessage() != null && e.getMessage().contains("404")) {
            log.warn("Model [{}] returned 404 (Deprecated or Not Found). Locking out for 24 hours.", modelName);
            cooldownMs = DEPRECATED_COOLDOWN_MS;
        } else {
            log.warn("Circuit Breaker: Locking out model [{}] for 60 seconds due to rate limits or parsing failure.", modelName);
        }
        modelCooldownMap.put(modelName, System.currentTimeMillis() + cooldownMs);
    }

    private ChatModel getChatModel(UUID userId, String targetModelName) {
        String cacheKey = (userId != null ? userId.toString() : "global") + "_" + targetModelName;

        ChatModel cachedModel = userModelCache.get(cacheKey);
        if (cachedModel != null) {
            return cachedModel;
        }

        String apiKeyToUse = globalApiKey;
        if (userId != null) {
            User user = userService.findById(userId).orElse(null);
            if (user != null) {
                String customKey = apiKeyService.getDecryptedApiKey(user);
                if (customKey != null && !customKey.isBlank()) {
                    apiKeyToUse = customKey;
                }
            }
        }

        //  Create an "effectively final" copy of the string to satisfy the Java lambda compiler rule!
        final String resolvedApiKey = apiKeyToUse;
        return userModelCache.computeIfAbsent(cacheKey, key -> buildSpecificModel(resolvedApiKey, targetModelName));
    }

    private GoogleGenAiChatModel buildSpecificModel(String apiKey, String modelName) {
        Client googleClient = Client.builder().apiKey(apiKey).build();

        GoogleGenAiChatOptions options =
                GoogleGenAiChatOptions.builder()
                        .model(modelName)
                        .temperature(defaultTemperature)
                        .build();

        return GoogleGenAiChatModel.builder()
                .genAiClient(googleClient)
                .defaultOptions(options)
                .build();
    }

    private String extractJson(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) return null;
        String text = rawResponse.trim();

        //  Strip out illegal JSON escape sequences like \$ or \' that models sometimes hallucinate
        text = text.replace("\\$", "$").replace("\\'", "'");

        // Clean up markdown backticks if AI ignores instructions
        if (text.startsWith("```json")) {
            text = text.substring(7);
        } else if (text.startsWith("```")) {
            text = text.substring(3);
        }
        if (text.endsWith("```")) {
            text = text.substring(0, text.length() - 3);
        }
        text = text.trim();

        int firstBrace = text.indexOf('{');
        int firstBracket = text.indexOf('[');
        int start = (firstBrace != -1 && firstBracket != -1) ? Math.min(firstBrace, firstBracket) : Math.max(firstBrace, firstBracket);
        if (start == -1) return null;

        int lastBrace = text.lastIndexOf('}');
        int lastBracket = text.lastIndexOf(']');
        int end = (lastBrace != -1 && lastBracket != -1) ? Math.max(lastBrace, lastBracket) : Math.max(lastBrace, lastBracket);
        if (end == -1 || end < start) return null;

        return text.substring(start, end + 1);
    }

    public void evictUserModel(UUID userId) {
        userModelCache.keySet().removeIf(key -> key.startsWith(userId.toString()));
    }

    /**
     * Builds a rate-limit message that tells the user which key was exhausted
     * and what they can do about it.
     */
    private String buildRateLimitMessage(UUID userId) {
        if (userHasOwnApiKey(userId)) {
            return "Your Gemini API key has hit its rate limit. Please try again shortly, or check your quota in Google AI Studio.";
        }
        return "The shared AI service is currently at capacity. Please try again shortly, or add your own API key in Profile → Gemini API Key for priority access.";
    }

    /**
     * Checks whether the user has configured their own API key.
     * Fails safe — any lookup error is treated as "no custom key".
     */
    private boolean userHasOwnApiKey(UUID userId) {
        if (userId == null) return false;
        try {
            User user = userService.findById(userId).orElse(null);
            if (user == null) return false;
            String key = apiKeyService.getDecryptedApiKey(user);
            return key != null && !key.isBlank();
        } catch (Exception e) {
            log.warn("Could not determine API key source for user {}: {}", userId, e.getMessage());
            return false;
        }
    }
}