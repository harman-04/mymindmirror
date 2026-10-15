package com.mymindmirror.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mymindmirror.backend.enums.AITask;
import com.mymindmirror.backend.enums.GamificationAction;
import com.mymindmirror.backend.model.JournalEntry;
import com.mymindmirror.backend.model.KeyPhrase;
import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.service.ai.DynamicAiClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JournalReflectionService {

    private final JournalPersistenceService journalPersistenceService;
    private final JournalSearchService journalSearchService;
    private final DynamicAiClientService aiClientService;
    private final ObjectMapper objectMapper;
    private final ChatMemoryService chatMemoryService;
    private final GamificationService gamificationService;

    @Value("${app.rag.top-k-reflective:5}")
    private int reflectiveTopK;

    @Value("${app.rag.top-k-fallback:4}")
    private int fallbackTopK;

    // Cache for reflective questions
    private static class CachedQuestion {
        final String question;
        final long timestamp;
        CachedQuestion(String question) {
            this.question = question;
            this.timestamp = System.currentTimeMillis();
        }
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > TimeUnit.MINUTES.toMillis(5);
        }
    }
    private final Map<String, CachedQuestion> questionCache = new ConcurrentHashMap<>();

    public String generateDailyReflection(User user) {
        log.info("Generating daily reflection for user {}", user.getUsername());

        LocalDate today = LocalDate.now();
        List<JournalEntry> todayEntries = journalPersistenceService.getJournalEntriesForUser(user, today, today);

        if (todayEntries == null || todayEntries.isEmpty()) {
            return "Journal an entry today to get your daily reflection!";
        }

        StringBuilder combinedText = new StringBuilder();
        Map<String, Double> aggregatedEmotions = new HashMap<>();
        Set<String> aggregatedConcerns = new HashSet<>();
        int count = todayEntries.size();

        for (JournalEntry entry : todayEntries) {
            combinedText.append(entry.getRawText()).append("\n\n---\n\n");
            try {
                if (entry.getEmotions() != null) {
                    Map<String, Double> emotions = objectMapper.readValue(entry.getEmotions(), new TypeReference<>() {});
                    for (Map.Entry<String, Double> e : emotions.entrySet()) {
                        aggregatedEmotions.put(e.getKey(), aggregatedEmotions.getOrDefault(e.getKey(), 0.0) + e.getValue());
                    }
                }
            } catch (Exception ignored) {}
            try {
                if (entry.getCoreConcerns() != null) {
                    List<String> concerns = objectMapper.readValue(entry.getCoreConcerns(), new TypeReference<>() {});
                    aggregatedConcerns.addAll(concerns);
                }
            } catch (Exception ignored) {}
        }

        List<String> emotionStrings = new ArrayList<>();
        for (Map.Entry<String, Double> entry : aggregatedEmotions.entrySet()) {
            double avg = entry.getValue() / count;
            if (avg > 0.01) {
                emotionStrings.add(String.format("%s (%.1f%%)", entry.getKey(), avg * 100));
            }
        }

        String emotionsStr = emotionStrings.isEmpty() ? "No specific emotions detected." : String.join(", ", emotionStrings);
        String concernsStr = aggregatedConcerns.isEmpty() ? "No specific concerns identified." : String.join(", ", aggregatedConcerns);

        String prompt = String.format("""
            Based on the following journal entries from today, their detected emotions, and core concerns,
            generate a concise (2-5 sentences), empathetic, and insightful "Today's Reflection" or a short, encouraging thought.

            **Language & Style Instruction:**
            - Detect the language(s) and style (casual, formal, emotional) of the journal entries.
            - Generate the reflection in the **same language(s) and style** as the entries. If the entries mix languages (e.g., Hinglish), respond in that same mix.
            - Focus on summarizing the overall emotional state and offering a gentle, positive perspective.

            **CRITICAL SYSTEM INSTRUCTION:**
            - DO NOT output your internal thought process, drafts, or constraints.
            - DO NOT use "Input:", "Goal:", or "Draft" labels.
            - Return ONLY the final 2-5 sentence reflection, with no introductory or concluding remarks.

            Journal Entries (combined): "%s"
            Detected Emotions (averaged): %s
            Core Concerns: %s

            Today's Reflection:""", combinedText.toString(), emotionsStr, concernsStr);

        try {
            String reflection = aiClientService.generate(prompt, user.getId(), AITask.TODAY_REFLECTION);
            gamificationService.recordActivity(user, GamificationAction.AI_REFLECTION);
            return (reflection != null && !reflection.isBlank()) ? reflection.trim() : "Couldn't generate a reflection today. Please try again later.";
        } catch (Exception e) {
            log.error("Failed to generate reflection natively: {}", e.getMessage(), e);
            return "Failed to generate reflection due to an internal error.";
        }
    }
    public String generateReflectionChat(User user, String query, String sessionId, boolean rememberChat) {
        // 1. Retrieve entries (This opens and closes a quick read-only transaction)
        List<JournalEntry> relevantEntries = journalSearchService.retrieveRelevantEntries(user, query, reflectiveTopK);

        StringBuilder journalContext = new StringBuilder();
        if (relevantEntries.isEmpty()) {
            log.info("RAG search yielded no results, falling back to recent entries.");
            journalContext.append("Here are the user's most recent journal entries (no specific match found for the query):\n\n");

            // ✅ Highly Optimized: Fetches EXACTLY 5 entries. No decryption overhead!
            List<JournalEntry> recentEntries = journalPersistenceService.getLatestJournalTimelineWithoutDecryption(user, reflectiveTopK);

            for (JournalEntry entry : recentEntries) {
                journalContext.append("Date: ").append(entry.getEntryDate()).append("\n");
                journalContext.append("Summary: ").append(entry.getSummary()).append("\n");
                String emotionsStr = entry.getEmotions() != null ? entry.getEmotions() : "{}";
                journalContext.append("Emotions: ").append(emotionsStr).append("\n---\n");
            }

        } else {
            log.info("RAG search successful. Feeding {} relevant entries to LLM.", relevantEntries.size());
            journalContext.append("Here are the user's most relevant past journal entries based on their question:\n\n");
            for (JournalEntry entry : relevantEntries) {
                journalContext.append("Date: ").append(entry.getEntryDate()).append("\n");
                journalContext.append("Summary: ").append(entry.getSummary()).append("\n");
                String emotionsStr = entry.getEmotions() != null ? entry.getEmotions() : "{}";
                journalContext.append("Emotions: ").append(emotionsStr).append("\n");

                // NO LazyInitializationException here because the EntityGraph already loaded them!
                if (entry.getKeyPhrases() != null && !entry.getKeyPhrases().isEmpty()) {
                    String phrases = entry.getKeyPhrases().stream()
                            .map(KeyPhrase::getPhrase)
                            .collect(Collectors.joining(", "));
                    journalContext.append("Key phrases: ").append(phrases).append("\n");
                }
                journalContext.append("---\n");
            }
        }

        // 2. Chat history (Redis - no DB connection used)
        StringBuilder conversationHistoryContext = new StringBuilder();
        if (rememberChat && sessionId != null && !sessionId.isBlank()) {
            List<ChatMemoryService.ChatMessage> history = chatMemoryService.getHistory(user.getId(), sessionId);
            if (!history.isEmpty()) {
                conversationHistoryContext.append("\n==============================================\n");
                conversationHistoryContext.append("RECENT CONVERSATION HISTORY (For ongoing context):\n");
                for (ChatMemoryService.ChatMessage msg : history) {
                    String label = "user".equalsIgnoreCase(msg.role()) ? "User" : "AI Coach";
                    conversationHistoryContext.append(label).append(": ").append(msg.content()).append("\n");
                }
                conversationHistoryContext.append("==============================================\n");
            }
        }

        String prompt = String.format("""
        You are 'MyMindMirror', a compassionate and highly insightful AI reflection coach. 
        The user is asking a question about their life, emotional patterns, or past experiences.
        
        I have searched the user's secure journal database and retrieved the most relevant past entries related to their question.
        
        Relevant Journal Context:
        %s
        %s
        
        User's current question: "%s"
        
        INSTRUCTIONS:
        1. Answer the user's question directly using the journal context and ongoing conversation history provided above.
        2. Speak directly to the user in a warm, empathetic, and conversational tone.
        3. If the context contains the answer, synthesize it beautifully. Refer to previous things they just said in the history if it helps the conversation flow naturally.
        4. If the context does NOT contain the answer, politely and gently inform the user that they haven't explicitly journaled about this topic recently, but offer general, supportive advice.
        5. Keep the answer concise (3-5 sentences). Do not use markdown formatting like bolding or bullet points unless absolutely necessary.
        """, journalContext.toString(), conversationHistoryContext.toString(), query);

        try {
            // 3. AI call runs totally independently. Database is completely safe!
            String aiResponse = aiClientService.generate(prompt, user.getId(), AITask.REFLECTION_CHAT);

            gamificationService.recordActivity(user, GamificationAction.CHAT);


            if (rememberChat && sessionId != null && !sessionId.isBlank() && aiResponse != null) {
                chatMemoryService.appendMessage(user.getId(), sessionId, "user", query);
                chatMemoryService.appendMessage(user.getId(), sessionId, "model", aiResponse);
            }
            return aiResponse;
        } catch (Exception e) {
            log.error("Error generating reflection chat", e);
            return "I'm unable to answer right now. Please try again later.";
        }
    }

    public String generateReflectiveQuestion(User user) {
        String cacheKey = user.getId().toString();
        CachedQuestion cached = questionCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            log.debug("Returning cached reflective question for user {}", user.getUsername());
            return cached.question;
        }

        StringBuilder context = new StringBuilder();
        String currentAnchorQuery = "my recent emotional journey";
        UUID mostRecentId = null;

        LocalDate today = LocalDate.now();
        List<JournalEntry> todayEntries = journalPersistenceService.getJournalEntriesForUser(user, today, today);

        if (todayEntries != null && !todayEntries.isEmpty()) {
            mostRecentId = todayEntries.get(0).getId();
            context.append("--- CURRENTLY ON USER'S MIND TODAY ---\n");
            StringBuilder todayCombinedSummaries = new StringBuilder();
            for (JournalEntry entry : todayEntries) {
                context.append("Entry (").append(entry.getCreationTimestamp().toLocalTime()).append("): ")
                        .append(entry.getSummary()).append("\n");
                if (entry.getSummary() != null) {
                    todayCombinedSummaries.append(entry.getSummary()).append(" ");
                }
            }
            context.append("\n");
            if (todayCombinedSummaries.length() > 0) {
                currentAnchorQuery = todayCombinedSummaries.toString().trim();
            }
        } else {
            // ✅ FIX 1: Used Persistence Service instead of Repository
            Optional<JournalEntry> latestEntryOpt = journalPersistenceService.findLatestEntryByUser(user);
            if (latestEntryOpt.isPresent()) {
                JournalEntry latestEntry = latestEntryOpt.get();
                mostRecentId = latestEntry.getId();
                currentAnchorQuery = latestEntry.getSummary() != null ? latestEntry.getSummary() : currentAnchorQuery;
                context.append("--- LATEST USER REFLECTION ---\n");
                context.append("Summary: ").append(currentAnchorQuery).append("\n\n");
            }
        }

        log.info("Executing optimized RAG vector search for reflective question.");
        List<JournalEntry> historicalParallels = journalSearchService.retrieveRelevantEntries(user, currentAnchorQuery, fallbackTopK);
        if (mostRecentId != null) {
            final UUID finalMostRecentId = mostRecentId;
            historicalParallels.removeIf(entry -> entry.getId().equals(finalMostRecentId));
        }

        if (!historicalParallels.isEmpty()) {
            context.append("--- RELEVANT PAST HISTORY FOUND via RAG ---\n");
            for (JournalEntry entry : historicalParallels) {
                context.append("Past Date: ").append(entry.getEntryDate()).append("\n");
                context.append("Past Summary: ").append(entry.getSummary()).append("\n");
                context.append("Past Emotions: ").append(entry.getEmotions()).append("\n");
                context.append("---\n");
            }
        } else {
            log.info("RAG search yielded zero historical matches. Falling back to latest timeline.");

            // ✅ Highly Optimized: Fetches EXACTLY 4 entries. No decryption overhead!
            List<JournalEntry> standardTimeline = journalPersistenceService.getLatestJournalTimelineWithoutDecryption(user, fallbackTopK);

            context.append("--- RECENT PAST TIMELINE ---\n");
            for (JournalEntry entry : standardTimeline) {
                context.append("Date: ").append(entry.getEntryDate()).append("\n");
                context.append("Summary: ").append(entry.getSummary()).append("\n");
                context.append("Emotions: ").append(entry.getEmotions()).append("\n");
                context.append("---\n");
            }
        }

        String prompt = String.format("""
            You are a compassionate, world-class AI reflection coach named 'MyMindMirror'.
            Your goal is to look at the user's current situation and bridge it with their past history to generate a single, highly insightful, open-ended reflective question.
            
            **CRITICAL INSTRUCTIONS:**
            - DO NOT output your internal thought process, reasoning steps, bullet points, or drafts.
            - DO NOT use any markdown labels, headers, or metadata indicators.
            - Return ONLY the final question string. Absolutely no extra text.
            - Focus heavily on drawing a parallel, a contrast, or a pattern between what they are experiencing today versus what they went through in the past context.
            - Keep the tone deeply warm, empathetic, and casual.

            Journal Context Matrix:
            %s

            Reflective Question:""", context.toString());

        try {
            String question = aiClientService.generate(prompt, user.getId(), AITask.REFLECTIVE_QUESTION);
            if (question == null || question.isBlank()) {
                question = "What's one thing you've learned about yourself recently?";
            }
            questionCache.put(cacheKey, new CachedQuestion(question));
            return question;
        } catch (Exception e) {
            log.error("Error generating RAG reflective question", e);
            return "What's one thing you've learned about yourself recently?";
        }
    }
}