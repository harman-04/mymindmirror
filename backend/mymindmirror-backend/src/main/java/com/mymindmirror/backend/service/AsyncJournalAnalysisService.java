package com.mymindmirror.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mymindmirror.backend.enums.AITask;
import com.mymindmirror.backend.model.JournalEntry;
import com.mymindmirror.backend.model.KeyPhrase;
import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.payload.response.JournalAnalysisResponse;
import com.mymindmirror.backend.repository.JournalEntryRepository;
import com.mymindmirror.backend.service.ai.DynamicAiClientService;
import com.mymindmirror.backend.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncJournalAnalysisService {

    private final JournalEntryRepository journalEntryRepository;
    private final UserService userService;
    private final DynamicAiClientService aiClientService;
    private final ObjectMapper objectMapper;
    private final EmbeddingGenerationService embeddingGenerationService;
    private final TransactionTemplate transactionTemplate;

    @Async
    public void analyzeJournalEntryAsync(UUID entryId, String decryptedRawText, UUID userId) {
        log.info("Starting async analysis for entry: {}", entryId);

        try {
            // 1. Verify user exists (optional, but needed for embedding)
            User user = userService.findById(userId).orElse(null);
            if (user == null) {
                log.error("User not found for async analysis, entry: {}", entryId);
                return;
            }

            // 2. Build prompt and call AI (outside any transaction)
            String prompt = buildJournalAnalysisPrompt(decryptedRawText);
            JournalAnalysisResponse analysis;
            try {
                analysis = aiClientService.generateStructured(prompt, JournalAnalysisResponse.class, userId, AITask.JOURNAL_ANALYSIS);
                log.info("AI analysis completed for entry: {}", entryId);
            } catch (Exception e) {
                log.error("AI analysis failed for entry {}: {}", entryId, e.getMessage());
                // Fallback
                analysis = new JournalAnalysisResponse(
                        Map.of(),
                        List.of(),
                        "AI analysis temporarily unavailable.",
                        List.of("Please try again later."),
                        List.of()
                );
            }

            // 3. Update and save in a short transaction, and get the saved entity
            JournalAnalysisResponse finalAnalysis = analysis;
            JournalEntry savedEntry = transactionTemplate.execute(status -> {
                // Use findByIdWithDetails to eagerly load keyPhrases – avoids lazy loading inside update
                JournalEntry freshEntry = journalEntryRepository.findByIdWithDetails(entryId)
                        .orElseThrow(() -> new RuntimeException("Entry disappeared before saving"));
                updateEntryWithAnalysis(freshEntry, finalAnalysis);
                return journalEntryRepository.save(freshEntry);
            });

            // 4. Generate embedding asynchronously using the updated entry (not the stale one!)
            embeddingGenerationService.updateEmbedding(savedEntry, user.getId());

        } catch (Exception e) {
            log.error("Async analysis failed for entry {}: {}", entryId, e.getMessage(), e);
        }
    }


//    2.  **Core Concerns**: Categorize the entry into 1 to 3 BROAD, standardized life themes. You MUST try to use one-to-two word categories like "Work", "Relationships", "Health", "Finances", "Family", "Mental Health", "Education", or "Hobbies". Do NOT invent highly specific phrases here; keep them generic so they can be grouped mathematically over time. Use the same mixed language/style if necessary, but keep it broad.

    private String buildJournalAnalysisPrompt(String journalText) {
        // This is the exact prompt from Flask's `get_gemini_journal_analysis`
        // We will embed it as a multi-line string.
        return """
                Analyze the following journal entry and provide the following information as a single JSON object.
                **LANGUAGE & STYLE INSTRUCTION:**
                - The journal entry may contain multiple languages (e.g., Hinglish: "Aaj wali movie bas timepass thi.") or mixed scripts.
                - Detect the primary language(s) and the style (casual, formal, humorous, etc.).
                - Generate ALL text output (summary, growth tips, core concerns, key phrases) in the **same mixed style and script** as the entry. For example, if the entry is in Hinglish (Hindi words written in Latin script), respond in Hinglish. If it mixes English and Spanish, respond with that mix.
                - Do not force a single language; preserve the natural code‑switching.

                1.  **Emotions**: Identify primary emotions with intensity scores from 0.0 to 1.0. Focus on common emotions like joy, sadness, anger, fear, surprise, disgust, love, anxiety, relief, neutral, excitement, contentment, frustration, gratitude, hope. Ensure scores sum up to 1.0 if possible, or represent relative intensity.

                2.  **Core Concerns**: Identify 3-5 main themes or core concerns discussed, as a list of concise strings (e.g., "work", "relationships", "health", "personal growth"). Use the same mixed language/style.

                3.  **Summary**: Write a dense, 2-3 sentence summary capturing the core emotional arc, key events, people, and any resolutions. **This summary will be used for semantic search indexing.** Avoid wasted filler like "The user writes about...". Instead, be direct and highly specific (e.g., "Felt extreme anxiety over a Spring Boot bug, but experienced immense relief after fixing the database connection."). Use the same mixed language/style.

                4.  **Growth Tips**: Generate 3-5 comprehensive, actionable, and deeply useful growth tips based on the entry's detected emotions and core concerns.
                    Each tip MUST be a **detailed markdown string** (not just one sentence). Use:
                    - **`##` subheadings** for each tip
                    - **Bullet points** for actionable steps
                    - **Bold text** for key concepts
                    - **`>` blockquotes** for reflective questions or affirmations
                    - **`---` horizontal rules** to separate tips (optional)
                    - **Links to relevant resources** (if applicable) in markdown `[text](url)`
                    Adapt the tone and style to match the entry (e.g., empathetic, motivational, practical). Use the same mixed language/style.

                5.  **Key Phrases**: Extract 5-10 descriptive, semantically rich key phrases. Use the formula: [Emotion/State] + [Context]. Avoid single generic words like "work" or "sad". Instead, use multi-word conceptual phrases (e.g., "anxiety before graduation viva", "joy from family time", "burnout from project deadlines"). Preserve the original language mix.

                The output MUST be a valid JSON object with the exact following structure. If a field cannot be determined, provide an empty list for arrays, an empty string for strings, or an empty object for emotion scores.
                {
                    "emotions": {
                        "joy": 0.X,
                        "sadness": 0.Y,
                        "anger": 0.Z,
                        "fear": 0.A,
                        "surprise": 0.B,
                        "disgust": 0.C,
                        "love": 0.D,
                        "anxiety": 0.E,
                        "relief": 0.F,
                        "neutral": 0.G,
                        "excitement": 0.H,
                        "contentment": 0.I,
                        "frustration": 0.J,
                        "gratitude": 0.K,
                        "hope": 0.L
                    },
                    "coreConcerns": ["concern1", "concern2", "concern3"],
                    "summary": "This is a concise summary of the journal entry.",
                    "growthTips": [
                        "## First Tip Heading\\n\\n> Reflective quote\\n\\n- **Action:** ...\\n- **Resource:** [link]...",
                        "## Second Tip Heading\\n\\n..."
                    ],
                    "keyPhrases": ["phrase1", "phrase2", "phrase3"]
                }

                Journal Entry: """ + journalText + """

                JSON Analysis:""";
    }

    private void updateEntryWithAnalysis(JournalEntry entry, JournalAnalysisResponse analysis) {
        // Set fields from the DTO
        entry.setMoodScore(calculateMoodScore(analysis.emotions()));
        entry.setEmotions(writeAsJson(analysis.emotions()));
        entry.setCoreConcerns(writeAsJson(analysis.coreConcerns()));
        entry.setSummary(analysis.summary());
        entry.setGrowthTips(writeAsJson(analysis.growthTips()));

        // Clear and set key phrases
        entry.getKeyPhrases().clear();
        List<KeyPhrase> newKeyPhrases = analysis.keyPhrases().stream()
                .map(phrase -> new KeyPhrase(phrase, entry))
                .collect(Collectors.toList());
        entry.getKeyPhrases().addAll(newKeyPhrases);
    }

    private Double calculateMoodScore(Map<String, Double> emotions) {
        // Emotion weights (mirror Flask logic)
        Map<String, Double> weights = Map.ofEntries(
                Map.entry("joy", 1.0), Map.entry("love", 1.0), Map.entry("surprise", 0.5),
                Map.entry("amusement", 0.5), Map.entry("excitement", 0.8), Map.entry("sadness", -1.0),
                Map.entry("anger", -0.8), Map.entry("fear", -0.7), Map.entry("disappointment", -0.6),
                Map.entry("grief", -1.0), Map.entry("neutral", 0.0), Map.entry("optimism", 0.7),
                Map.entry("relief", 0.4), Map.entry("caring", 0.6), Map.entry("curiosity", 0.3),
                Map.entry("embarrassment", -0.4), Map.entry("pride", 0.5), Map.entry("remorse", -0.5),
                Map.entry("annoyance", -0.3), Map.entry("disgust", -0.6), Map.entry("stress", -0.7),
                Map.entry("frustration", -0.5), Map.entry("gratitude", 0.9), Map.entry("hope", 0.8)
        );

        double totalWeighted = 0.0;
        double totalScore = 0.0;
        for (var entry : emotions.entrySet()) {
            String emotion = entry.getKey().toLowerCase();
            Double score = entry.getValue();
            Double weight = weights.getOrDefault(emotion, 0.0);
            totalWeighted += score * weight;
            totalScore += score;
        }
        if (totalScore > 0) {
            return totalWeighted / totalScore;
        }
        return 0.0;
    }

    private String writeAsJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize to JSON", e);
            return "{}";
        }
    }
}