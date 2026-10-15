package com.mymindmirror.backend.service;

import com.mymindmirror.backend.constants.CacheConstants;
import com.mymindmirror.backend.model.JournalEntry;
import com.mymindmirror.backend.model.KeyPhrase;
import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.payload.response.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JournalService {

    private final JournalPersistenceService journalPersistenceService;
    private final JournalSearchService journalSearchService;
    private final JournalReflectionService journalReflectionService;
    private final AnomalyDetectionService anomalyDetectionService;


    private static final int DEFAULT_TREND_DAYS = 90;
    private static final int MAX_TREND_LIMIT = 100;

    public Map<String, Long> getTopKeyPhrases(User user, LocalDate start, LocalDate end, int limit) {
        int safeLimit = Math.min(limit, MAX_TREND_LIMIT);

        //  Using the highly optimized, non‑decrypting fetch method
        List<JournalEntry> entries = journalPersistenceService.getEntriesForTrendsWithoutDecryption(user, start, end);

        return entries.stream()
                .filter(entry -> entry.getKeyPhrases() != null)
                .flatMap(entry -> entry.getKeyPhrases().stream())
                .map(KeyPhrase::getPhrase)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(safeLimit)
                .collect(
                        LinkedHashMap::new,
                        (map, entry) -> map.put(entry.getKey(), entry.getValue()),
                        LinkedHashMap::putAll
                );
    }


    // ---------- Persistence ----------
    @Transactional
    @CacheEvict(value = CacheConstants.KEY_PHRASE_FREQUENCIES, key = "#user.id")
    public JournalEntry saveJournalEntry(User user, String rawText) {
        return journalPersistenceService.saveJournalEntry(user, rawText);
    }

    @Transactional
    @CacheEvict(value = CacheConstants.KEY_PHRASE_FREQUENCIES, key = "#user.id")
    public JournalEntry updateJournalEntry(UUID entryId, User user, String updatedText) {
        return journalPersistenceService.updateJournalEntry(entryId, user, updatedText);
    }

    @Transactional
    @CacheEvict(value = CacheConstants.KEY_PHRASE_FREQUENCIES, key = "#user.id")
    public void deleteJournalEntry(UUID entryId, User user) {
        journalPersistenceService.deleteJournalEntry(entryId, user);
    }

    @Transactional(readOnly = true)
    public List<JournalEntry> getJournalEntriesForUser(User user, LocalDate startDate, LocalDate endDate) {
        return journalPersistenceService.getJournalEntriesForUser(user, startDate, endDate);
    }

    @Transactional(readOnly = true)
    public Optional<JournalEntry> getJournalEntryById(UUID entryId) {
        return journalPersistenceService.getJournalEntryById(entryId);
    }

    public List<MoodDataResponse> getMoodDataForChart(User user, LocalDate startDate, LocalDate endDate) {
        return journalPersistenceService.getMoodDataForChart(user, startDate, endDate);
    }

    @Transactional(readOnly = true)
    public List<DailyAggregatedDataResponse> getDailyAggregatedDataForUser(User user, LocalDate startDate, LocalDate endDate) {
        return journalPersistenceService.getDailyAggregatedDataForUser(user, startDate, endDate);
    }

    @Transactional(readOnly = true)
    public List<JournalEntry> findByUser(User user) {
        return journalPersistenceService.findByUser(user);
    }

    @Transactional(readOnly = true)
    public PageResponse<JournalEntryResponse> getJournalEntriesPageResponse(User user, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        return journalPersistenceService.getJournalEntriesPageResponse(user, startDate, endDate, pageable);
    }

    // ---------- Search ----------
    @Transactional(readOnly = true)
    public List<JournalEntry> searchJournalEntriesByKeyword(User user, String keyword) {
        return journalSearchService.searchJournalEntriesByKeyword(user, keyword);
    }

    @Transactional(readOnly = true)
    public List<JournalEntry> searchJournalEntriesByMoodScore(User user, Double minMood, Double maxMood) {
        return journalSearchService.searchJournalEntriesByMoodScore(user, minMood, maxMood);
    }

    @Transactional(readOnly = true)
    public List<JournalEntry> searchJournalEntriesSemantically(User user, String concept) {
        return journalSearchService.searchJournalEntriesSemantically(user, concept);
    }

    // ---------- Reflection ----------
    public String generateDailyReflection(User user) {
        return journalReflectionService.generateDailyReflection(user);
    }

    // Called from ReflectionChatController — ChatRequest.query is @NotBlank validated.
    public String generateReflectionChat(User user, String query, String sessionId, boolean rememberChat) {
        return journalReflectionService.generateReflectionChat(user, query, sessionId, rememberChat);
    }

    public String generateReflectiveQuestion(User user) {
        return journalReflectionService.generateReflectiveQuestion(user);
    }

    // ---------- Anomaly ----------
    public AnomalyDetectionResponse runAnomalyDetection(List<DailyAggregatedDataResponse> aggregatedData) {
        return anomalyDetectionService.detectAnomalies(aggregatedData);
    }

    // ---------- Key phrase frequencies (cached) ----------
    @Cacheable(value = CacheConstants.KEY_PHRASE_FREQUENCIES, key = "#user.id")
    @Transactional(readOnly = true)
    public Map<String, Long> getKeyPhraseFrequencies(User user) {
        List<JournalEntry> entries = journalPersistenceService.findByUser(user);
        Map<String, Long> freq = new HashMap<>();
        for (JournalEntry entry : entries) {
            if (entry.getKeyPhrases() != null) {
                for (var kp : entry.getKeyPhrases()) {
                    String phrase = kp.getPhrase().toLowerCase();
                    freq.put(phrase, freq.getOrDefault(phrase, 0L) + 1);
                }
            }
        }
        return freq;
    }
}