package com.mymindmirror.backend.controller;

import com.mymindmirror.backend.annotation.CurrentUser;
import com.mymindmirror.backend.mapper.JournalMapper;
import com.mymindmirror.backend.model.JournalEntry;
import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.payload.request.JournalEntryRequest;
import com.mymindmirror.backend.payload.response.JournalEntryResponse;
import com.mymindmirror.backend.payload.response.MoodDataResponse;
import com.mymindmirror.backend.payload.response.PageResponse;
import com.mymindmirror.backend.service.JournalService;
import com.mymindmirror.backend.util.DateRange;
import com.mymindmirror.backend.util.DateUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;


import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/journal")
@RequiredArgsConstructor
@Slf4j
public class JournalController {

    private final JournalService journalService;
    private final JournalMapper journalMapper;

    @PostMapping
    public ResponseEntity<JournalEntryResponse> createJournalEntry(
            @CurrentUser User currentUser,
            @Valid @RequestBody JournalEntryRequest request) {

        log.info("Received request to create journal entry.");
        JournalEntry savedEntry = journalService.saveJournalEntry(currentUser, request.rawText());
        log.info("New journal entry saved successfully for user {}.", currentUser.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(journalMapper.toResponse(savedEntry));
    }

    @PutMapping("/{id}")
    public ResponseEntity<JournalEntryResponse> updateJournalEntry(
            @CurrentUser User currentUser,
            @PathVariable UUID id,
            @Valid @RequestBody JournalEntryRequest request) {

        log.info("Received request to update journal entry with ID: {}", id);
        JournalEntry updatedEntry = journalService.updateJournalEntry(id, currentUser, request.rawText());
        log.info("Journal entry with ID {} updated successfully for user {}.", id, currentUser.getUsername());
        return ResponseEntity.ok(journalMapper.toResponse(updatedEntry));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJournalEntry(
            @CurrentUser User currentUser,
            @PathVariable UUID id) {

        log.info("Received request to delete journal entry with ID: {}", id);
        journalService.deleteJournalEntry(id, currentUser);
        log.info("Journal entry with ID {} deleted successfully for user {}.", id, currentUser.getUsername());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/history")
    public ResponseEntity<List<JournalEntryResponse>> getJournalHistory(
            @CurrentUser User currentUser,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        log.info("Received request for journal history for user: {}", currentUser.getUsername());

        DateRange range = DateUtil.parseHistoryDateRange(startDate, endDate);
        List<JournalEntry> entries = journalService.getJournalEntriesForUser(
                currentUser, range.start(), range.end()
        );
        List<JournalEntryResponse> responses = entries.stream()
                .map(journalMapper::toResponse)
                .collect(Collectors.toList());

        log.info("Found {} journal entries for user {}.", responses.size(), currentUser.getUsername());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/mood-data")
    public ResponseEntity<List<MoodDataResponse>> getMoodData(
            @CurrentUser User currentUser,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        log.info("Received request for mood data for chart for user: {}", currentUser.getUsername());

        DateRange range = DateUtil.parseDateRange(startDate, endDate, 30);
        List<MoodDataResponse> moodData = journalService.getMoodDataForChart(
                currentUser, range.start(), range.end()
        );

        log.info("Found {} mood data points for user {}.", moodData.size(), currentUser.getUsername());
        return ResponseEntity.ok(moodData);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JournalEntryResponse> getJournalEntry(
            @CurrentUser User currentUser,
            @PathVariable UUID id) {

        log.info("Received request for journal entry with ID: {}.", id);
        JournalEntry entry = journalService.getJournalEntryById(id)
                .filter(e -> e.getUser().getId().equals(currentUser.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Journal entry not found or not owned."));

        return ResponseEntity.ok(journalMapper.toResponse(entry));
    }

    @GetMapping("/trends")
    public ResponseEntity<Map<String, Long>> getJournalTrends(
            @CurrentUser User currentUser,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "10") int limit) {

        log.info("Received request for journal trends for user: {}", currentUser.getUsername());

        //  Parse dates in the controller using DateUtil
        DateRange range = DateUtil.parseDateRange(startDate, endDate, 90);
        Map<String, Long> trends = journalService.getTopKeyPhrases(
                currentUser, range.start(), range.end(), limit
        );

        log.info("Returning {} top trends for user {}.", trends.size(), currentUser.getUsername());
        return ResponseEntity.ok(trends);
    }

    @GetMapping("/search/keyword")
    public ResponseEntity<List<JournalEntryResponse>> searchJournalEntriesByKeyword(
            @CurrentUser User currentUser,
            @RequestParam String keyword) {

        log.info("Received request to search journal entries by keyword: '{}'", keyword);
        List<JournalEntry> entries = journalService.searchJournalEntriesByKeyword(currentUser, keyword);
        List<JournalEntryResponse> responses = entries.stream()
                .map(journalMapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/search/mood")
    public ResponseEntity<List<JournalEntryResponse>> searchJournalEntriesByMood(
            @CurrentUser User currentUser,
            @RequestParam(required = false) Double minMood,
            @RequestParam(required = false) Double maxMood) {

        log.info("Received request to search journal entries by mood range.");
        Double actualMinMood = (minMood != null) ? minMood : -1.0;
        Double actualMaxMood = (maxMood != null) ? maxMood : 1.0;

        if (actualMinMood > actualMaxMood) {
            throw new IllegalArgumentException("Minimum mood cannot be greater than maximum mood.");
        }

        List<JournalEntry> entries = journalService.searchJournalEntriesByMoodScore(currentUser, actualMinMood, actualMaxMood);
        List<JournalEntryResponse> responses = entries.stream()
                .map(journalMapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/search/semantic")
    public ResponseEntity<List<JournalEntryResponse>> searchJournalEntriesSemantically(
            @CurrentUser User currentUser,
            @RequestParam String concept) {

        log.info("Received request for semantic search: '{}'", concept);
        List<JournalEntry> entries = journalService.searchJournalEntriesSemantically(currentUser, concept);
        List<JournalEntryResponse> responses = entries.stream()
                .map(journalMapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/key-phrases")
    public ResponseEntity<Map<String, Long>> getKeyPhraseFrequencies(@CurrentUser User currentUser) {
        Map<String, Long> freq = journalService.getKeyPhraseFrequencies(currentUser);
        return ResponseEntity.ok(freq);
    }

    @GetMapping("/history/paginated")
    public ResponseEntity<PageResponse<JournalEntryResponse>> getJournalHistoryPaginated(
            @CurrentUser User currentUser,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("Received request for paginated journal history for user: {}", currentUser.getUsername());

        DateRange range = DateUtil.parseHistoryDateRange(startDate, endDate);
        PageResponse<JournalEntryResponse> page = journalService.getJournalEntriesPageResponse(
                currentUser, range.start(), range.end(), pageable
        );

        log.info("Returning page {}/{} for user {}.", page.pageNumber(), page.totalPages(), currentUser.getUsername());
        return ResponseEntity.ok(page);
    }

}