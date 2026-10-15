package com.mymindmirror.backend.service;


import com.mymindmirror.backend.constants.CacheConstants;
import com.mymindmirror.backend.enums.GamificationAction;
import com.mymindmirror.backend.mapper.JournalMapper;
import com.mymindmirror.backend.model.JournalEntry;
import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.payload.response.*;
import com.mymindmirror.backend.repository.DailyJournalSummaryRepository;
import com.mymindmirror.backend.repository.JournalEntryRepository;
import com.mymindmirror.backend.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class JournalPersistenceService {

    private final JournalEntryRepository journalEntryRepository;
    private final DailyJournalSummaryRepository dailyJournalSummaryRepository;
    private final GamificationService gamificationService;
    private final AsyncJournalAnalysisService asyncJournalAnalysisService;
    private final EmbeddingGenerationService embeddingGenerationService;
    private final JournalMapper journalMapper;



    // ------------------------- CRUD -------------------------

    @Transactional
    @CacheEvict(value = CacheConstants.KEY_PHRASE_FREQUENCIES, key = "#user.id")
    public JournalEntry saveJournalEntry(User user, String rawText) {
        log.info("Saving new journal entry for user: {}", user.getUsername());

        String userSecret = user.getPasswordHash();
        if (userSecret == null || userSecret.isEmpty()) {
            throw new IllegalStateException("User secret (password hash) not available for encryption.");
        }

        JournalEntry newEntry = new JournalEntry();
        newEntry.setUser(user);
        newEntry.setEntryDate(LocalDate.now());
        newEntry.setCreationTimestamp(LocalDateTime.now());

        String actualRawText = (rawText != null) ? rawText : "";
        int wordCount = actualRawText.trim().isEmpty() ? 0 : actualRawText.split("\\s+").length;
        newEntry.setWordCount(wordCount);

        String encryptedText = EncryptionUtil.encrypt(actualRawText, userSecret);
        newEntry.setRawText(encryptedText != null ? encryptedText : actualRawText);

        resetAiFields(newEntry);

        JournalEntry savedEntry = journalEntryRepository.save(newEntry);

        gamificationService.recordActivity(user, GamificationAction.JOURNAL);

        // Register async analysis after commit
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    asyncJournalAnalysisService.analyzeJournalEntryAsync(savedEntry.getId(), actualRawText, user.getId());
                }
            });
        } else {
            asyncJournalAnalysisService.analyzeJournalEntryAsync(savedEntry.getId(), actualRawText, user.getId());
        }

        log.info("Journal entry {} saved, async analysis scheduled.", savedEntry.getId());
        return savedEntry;
    }

    @Transactional
    @CacheEvict(value = CacheConstants.KEY_PHRASE_FREQUENCIES, key = "#user.id")
    public JournalEntry updateJournalEntry(UUID entryId, User user, String updatedText) {
        log.info("Updating journal entry {} for user {}", entryId, user.getUsername());


        JournalEntry existingEntry = journalEntryRepository.findByIdWithDetails(entryId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Journal entry not found with ID: " + entryId));

        if (!existingEntry.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Journal entry not found with ID: " + entryId);
        }

        String userSecret = user.getPasswordHash();
        String textToEncrypt = (updatedText != null) ? updatedText : "";
        String decryptedOldText = EncryptionUtil.decrypt(existingEntry.getRawText(), userSecret);
        boolean textContentChanged = !textToEncrypt.equals(decryptedOldText);

        int wordCount = textToEncrypt.trim().isEmpty() ? 0 : textToEncrypt.trim().split("\\s+").length;
        existingEntry.setWordCount(wordCount);

        String encryptedText = EncryptionUtil.encrypt(textToEncrypt, userSecret);
        existingEntry.setRawText(encryptedText != null ? encryptedText : textToEncrypt);

        if (textContentChanged) {
            resetAiFields(existingEntry);
            final String textToAnalyze = textToEncrypt;
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        asyncJournalAnalysisService.analyzeJournalEntryAsync(entryId, textToAnalyze, user.getId());
                    }
                });
            } else {
                asyncJournalAnalysisService.analyzeJournalEntryAsync(entryId, textToAnalyze, user.getId());
            }
        }

        JournalEntry savedEntry = journalEntryRepository.save(existingEntry);
        log.info("Journal entry {} updated.", entryId);
        return savedEntry;
    }

    @Transactional
    @CacheEvict(value = CacheConstants.KEY_PHRASE_FREQUENCIES, key = "#user.id")
    public void deleteJournalEntry(UUID entryId, User user) {
        log.info("Deleting journal entry {} for user {}", entryId, user.getUsername());

        JournalEntry existingEntry = journalEntryRepository.findById(entryId)

                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Journal entry not found with ID: " + entryId));

        if (!existingEntry.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Journal entry not found with ID: " + entryId);
        }

        journalEntryRepository.delete(existingEntry);
        embeddingGenerationService.deleteEmbedding(entryId);
        log.info("Journal entry {} deleted.", entryId);
    }

    // ------------------------- Queries -------------------------

    @Transactional(readOnly = true)
    public List<JournalEntry> getJournalEntriesForUser(User user, LocalDate startDate, LocalDate endDate) {
        log.info("Fetching journal entries for user {} from {} to {}", user.getUsername(), startDate, endDate);
        List<JournalEntry> entries = journalEntryRepository
                .findByUserAndEntryDateBetweenOrderByCreationTimestampDesc(user, startDate, endDate);

        String userSecret = user.getPasswordHash();
        if (userSecret != null && !userSecret.isEmpty()) {
            for (JournalEntry entry : entries) {
                entry.setRawText(EncryptionUtil.decrypt(entry.getRawText(), userSecret));
            }
        } else {
            log.error("User {} has no password hash – cannot decrypt entries.", user.getUsername());
        }
        return entries;
    }

    @Transactional(readOnly = true)
    public Optional<JournalEntry> getJournalEntryById(UUID entryId) {
        log.info("Fetching journal entry by ID: {}", entryId);
        Optional<JournalEntry> entryOptional = journalEntryRepository.findByIdWithDetails(entryId);
        entryOptional.ifPresent(entry -> {
            User user = entry.getUser();
            String userSecret = user.getPasswordHash();
            if (userSecret != null && !userSecret.isEmpty()) {
                entry.setRawText(EncryptionUtil.decrypt(entry.getRawText(), userSecret));
            } else {
                log.error("User {} has no password hash – cannot decrypt entry {}.", user.getUsername(), entryId);
            }
        });
        return entryOptional;
    }

    @Transactional(readOnly = true)
    public List<MoodDataResponse> getMoodDataForChart(User user, LocalDate startDate, LocalDate endDate) {
        log.info("Fetching mood data for chart for user {} from {} to {}", user.getUsername(), startDate, endDate);
        List<JournalEntry> entries = journalEntryRepository.findByUserAndEntryDateBetween(user, startDate, endDate);

        String userSecret = user.getPasswordHash();
        if (userSecret != null && !userSecret.isEmpty()) {
            for (JournalEntry entry : entries) {
                entry.setRawText(EncryptionUtil.decrypt(entry.getRawText(), userSecret));
            }
        } else {
            log.error("User {} has no password hash – cannot decrypt entries.", user.getUsername());
        }

        // Force initialization of keyPhrases if needed (Hibernate.initialize)
        for (JournalEntry entry : entries) {
            if (entry.getKeyPhrases() != null) {
                Hibernate.initialize(entry.getKeyPhrases());
            }
        }

        return entries.stream()
                .filter(entry -> entry.getMoodScore() != null)
                .map(entry -> new MoodDataResponse(entry.getEntryDate(), entry.getMoodScore()))
                .sorted(Comparator.comparing(MoodDataResponse::date))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DailyAggregatedDataResponse> getDailyAggregatedDataForUser(User user, LocalDate startDate, LocalDate endDate) {
        log.info("Fetching daily aggregated data for user {} from {} to {}", user.getUsername(), startDate, endDate);
        List<Object[]> results = dailyJournalSummaryRepository.findByUserAndDateRange(user.getId(), startDate, endDate);
        return results.stream()
                .map(row -> new DailyAggregatedDataResponse((LocalDate) row[0], (Double) row[1], (Long) row[2]))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<JournalEntry> findByUser(User user) {
        log.info("Fetching all entries for user {}", user.getUsername());
        List<JournalEntry> entries = journalEntryRepository.findByUser(user);

        String userSecret = user.getPasswordHash();
        if (userSecret != null && !userSecret.isEmpty()) {
            for (JournalEntry entry : entries) {
                entry.setRawText(EncryptionUtil.decrypt(entry.getRawText(), userSecret));
            }
        } else {
            log.error("User {} has no password hash – cannot decrypt entries.", user.getUsername());
        }

        // Initialize lazy collections
        for (JournalEntry entry : entries) {
            if (entry.getKeyPhrases() != null) {
                Hibernate.initialize(entry.getKeyPhrases());
            }
            Hibernate.initialize(entry.getUser());
        }
        return entries;
    }

    @Transactional(readOnly = true)
    public PageResponse<JournalEntryResponse> getJournalEntriesPageResponse(User user, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        log.info("Fetching paginated journal entries for user {} from {} to {}", user.getUsername(), startDate, endDate);
        Page<JournalEntry> page = journalEntryRepository
                .findByUserAndEntryDateBetweenOrderByCreationTimestampDesc(user, startDate, endDate, pageable);

        String userSecret = user.getPasswordHash();
        List<JournalEntryResponse> responseList = new ArrayList<>();

        for (JournalEntry entry : page.getContent()) {
            String decryptedText = entry.getRawText();
            if (userSecret != null && !userSecret.isEmpty()) {
                decryptedText = EncryptionUtil.decrypt(entry.getRawText(), userSecret);
                entry.setRawText(decryptedText);
            }
            Hibernate.initialize(entry.getKeyPhrases());
            Hibernate.initialize(entry.getUser());

            //  Use the mapper – it handles all JSON parsing via JsonMapperHelper
            responseList.add(journalMapper.toResponse(entry));
        }

        return PageResponse.<JournalEntryResponse>builder()
                .content(responseList)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    // ------------------------- Helper methods -------------------------

    public void resetAiFields(JournalEntry entry) {
        entry.setMoodScore(null);
        entry.setEmotions(null);
        entry.setCoreConcerns(null);
        entry.setSummary(null);
        entry.setGrowthTips(null);
        if (entry.getKeyPhrases() != null) {
            entry.getKeyPhrases().clear();
        } else {
            entry.setKeyPhrases(new ArrayList<>());
        }
    }



    @Transactional(readOnly = true)
    public Optional<JournalEntry> findLatestEntryByUser(User user) {
        log.debug("Fetching latest journal entry for user: {}", user.getUsername());
        org.springframework.data.domain.PageRequest limitOne = org.springframework.data.domain.PageRequest.of(0, 1);
        List<JournalEntry> results = journalEntryRepository.findLatestEntryByUser(user, limitOne);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }


    /**
     * Highly optimized fetch for AI Prompts.
     * Uses DB-level LIMIT, avoids N+1 EntityGraphs, and skips heavy AES decryption.
     */
    @Transactional(readOnly = true)
    public List<JournalEntry> getLatestJournalTimelineWithoutDecryption(User user, int limit) {
        log.debug("Fetching latest {} timeline entries (no decryption) for user {}", limit, user.getUsername());
        return journalEntryRepository.findLatestEntryByUser(user, org.springframework.data.domain.PageRequest.of(0, limit));
    }



    /**
     * Highly optimized fetch for Trend analysis.
     * Skips AES decryption because trends only rely on KeyPhrases.
     */
    @Transactional(readOnly = true)
    public List<JournalEntry> getEntriesForTrendsWithoutDecryption(User user, LocalDate startDate, LocalDate endDate) {
        log.debug("Fetching entries for trends (no decryption) for user {}", user.getUsername());
        return journalEntryRepository.findByUserAndEntryDateBetweenOrderByCreationTimestampDesc(user, startDate, endDate);
    }
}