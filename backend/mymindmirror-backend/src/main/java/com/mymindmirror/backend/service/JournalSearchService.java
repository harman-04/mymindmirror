package com.mymindmirror.backend.service;

import com.mymindmirror.backend.model.JournalEntry;
import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.repository.JournalEntryRepository;
import com.mymindmirror.backend.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JournalSearchService {

    private final JournalEntryRepository journalEntryRepository;
    private final EmbeddingGenerationService embeddingGenerationService;

    @Value("${app.rag.similarity-threshold:0.55}")
    private double similarityThreshold;

    @Value("${app.rag.similarity-threshold-fallback:0.50}")
    private double similarityThresholdFallback;

    @Value("${app.rag.top-k:10}")
    private int semanticSearchTopK;   // for public semantic search


    @Transactional(readOnly = true)
    public List<JournalEntry> searchJournalEntriesByKeyword(User user, String keyword) {
        log.info("Searching journal entries for user: {} with keyword: '{}'", user.getUsername(), keyword);

        String userSecret = user.getPasswordHash();
        if (userSecret == null || userSecret.isEmpty()) {
            log.error("User {} has no password hash. Cannot decrypt journal entries.", user.getUsername());
            return List.of();
        }

        List<JournalEntry> allEncryptedEntries = journalEntryRepository.findByUserOrderByCreationTimestampDesc(user);
        String lowerCaseKeyword = keyword.toLowerCase();
        return allEncryptedEntries.stream()
                .peek(entry -> {
                    String decryptedText = EncryptionUtil.decrypt(entry.getRawText(), userSecret);
                    entry.setRawText(decryptedText != null ? decryptedText : "");
                })
                .filter(entry -> entry.getRawText().toLowerCase().contains(lowerCaseKeyword))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<JournalEntry> searchJournalEntriesByMoodScore(User user, Double minMood, Double maxMood) {
        log.info("Searching journal entries for user: {} with mood score between {} and {}", user.getUsername(), minMood, maxMood);
        List<JournalEntry> entries = journalEntryRepository
                .findByUserAndMoodScoreBetweenOrderByCreationTimestampDesc(user, minMood, maxMood);

        String userSecret = user.getPasswordHash();
        if (userSecret != null && !userSecret.isEmpty()) {
            for (JournalEntry entry : entries) {
                entry.setRawText(EncryptionUtil.decrypt(entry.getRawText(), userSecret));
            }
        } else {
            log.error("User {} has no password hash. Cannot decrypt entries.", user.getUsername());
        }
        return entries;
    }

//    @Transactional(readOnly = true)
//    public List<JournalEntry> searchJournalEntriesSemantically(User user, String concept) {
//        log.info("Executing semantic concept search for user: {} -> '{}'", user.getUsername(), concept);
//        String userSecret = user.getPasswordHash();
//        if (userSecret == null || userSecret.isEmpty()) return List.of();
//
//        try {
//            SearchRequest searchRequest = SearchRequest.builder()
//                    .query(concept)
//                    .topK(semanticSearchTopK)               // ✅ injected
//                    .similarityThreshold(similarityThreshold) // ✅ injected
//                    .filterExpression("userId == '" + user.getId().toString() + "'")
//                    .build();
//
//            List<Document> documents = embeddingGenerationService.getVectorStore().similaritySearch(searchRequest);
//            if (documents == null || documents.isEmpty()) return List.of();
//
//            List<UUID> orderedEntryIds = documents.stream()
//                    .map(doc -> UUID.fromString(doc.getMetadata().get("entryId").toString()))
//                    .collect(Collectors.toList());
//
//            List<JournalEntry> unorderedEntries = journalEntryRepository.findByIdIn(orderedEntryIds);
//            Map<UUID, JournalEntry> entryMap = unorderedEntries.stream()
//                    .collect(Collectors.toMap(JournalEntry::getId, java.util.function.Function.identity()));
//
//            List<JournalEntry> rankedAndDecrypted = new ArrayList<>();
//            for (UUID id : orderedEntryIds) {
//                JournalEntry entry = entryMap.get(id);
//                if (entry != null) {
//                    String decryptedText = EncryptionUtil.decrypt(entry.getRawText(), userSecret);
//                    entry.setRawText(decryptedText != null ? decryptedText : "");
//                    rankedAndDecrypted.add(entry);
//                }
//            }
//            return rankedAndDecrypted;
//        } catch (Exception e) {
//            log.error("Fatal exception during semantic search execution", e);
//            return List.of();
//        }
//    }

    @Transactional(readOnly = true)
    public List<JournalEntry> searchJournalEntriesSemantically(User user, String concept) {
        log.info("Executing semantic concept search for user: {} -> '{}'", user.getUsername(), concept);
        String userSecret = user.getPasswordHash();
        if (userSecret == null || userSecret.isEmpty()) return List.of();

        // 1. Vector store call — Postgres-specific; fail loudly on outage
        List<Document> documents;
        try {
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(concept)
                    .topK(semanticSearchTopK)
                    .similarityThreshold(similarityThreshold)
                    .filterExpression("userId == '" + user.getId().toString() + "'")
                    .build();

            documents = embeddingGenerationService.getVectorStore().similaritySearch(searchRequest);
        } catch (Exception e) {
            log.error("Vector store (Postgres) unreachable during semantic search", e);
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Semantic search is temporarily unavailable. Please try again in a moment.",
                    e);
        }

        if (documents == null || documents.isEmpty()) return List.of();

        // 2. Rest of the method — MySQL failures propagate to handleResourceFailure → 503
        List<UUID> orderedEntryIds = documents.stream()
                .map(doc -> UUID.fromString(doc.getMetadata().get("entryId").toString()))
                .collect(Collectors.toList());

        List<JournalEntry> unorderedEntries = journalEntryRepository.findByIdIn(orderedEntryIds);
        Map<UUID, JournalEntry> entryMap = unorderedEntries.stream()
                .collect(Collectors.toMap(JournalEntry::getId, java.util.function.Function.identity()));

        List<JournalEntry> rankedAndDecrypted = new ArrayList<>();
        for (UUID id : orderedEntryIds) {
            JournalEntry entry = entryMap.get(id);
            if (entry != null) {
                String decryptedText = EncryptionUtil.decrypt(entry.getRawText(), userSecret);
                entry.setRawText(decryptedText != null ? decryptedText : "");
                rankedAndDecrypted.add(entry);
            }
        }
        return rankedAndDecrypted;
    }

    // Changed to public and added @Transactional so it loads keyPhrases eagerly before returning
    @Transactional(readOnly = true)
    public List<JournalEntry> retrieveRelevantEntries(User user, String query, int topK) {
        log.info("Performing internal vector similarity search for user: {}", user.getUsername());
        try {
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(topK)                                 // ✅ parameter passed from caller
                    .similarityThreshold(similarityThresholdFallback) // ✅ injected
                    .filterExpression("userId == '" + user.getId().toString() + "'")
                    .build();


            List<Document> documents = embeddingGenerationService.getVectorStore().similaritySearch(searchRequest);
            if (documents == null || documents.isEmpty()) {
                return new ArrayList<>();
            }

            List<UUID> orderedEntryIds = documents.stream()
                    .map(doc -> UUID.fromString(doc.getMetadata().get("entryId").toString()))
                    .collect(Collectors.toList());

            // Because this is inside @Transactional and findByIdIn has @EntityGraph,
            // keyPhrases are safely loaded into memory here!
            List<JournalEntry> unorderedEntries = journalEntryRepository.findByIdIn(orderedEntryIds);
            Map<UUID, JournalEntry> entryMap = unorderedEntries.stream()
                    .collect(Collectors.toMap(JournalEntry::getId, java.util.function.Function.identity()));

            List<JournalEntry> rankedEntries = new ArrayList<>();
            for (UUID id : orderedEntryIds) {
                JournalEntry entry = entryMap.get(id);
                if (entry != null) {
                    rankedEntries.add(entry);
                }
            }
            return rankedEntries;
        } catch (Exception e) {
            log.error("Failed to execute internal vector search: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
}