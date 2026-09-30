package com.mymindmirror.backend.payload.response;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record JournalEntryResponse(
        UUID id,
        UserResponse user,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate entryDate,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime creationTimestamp,
        String rawText,
        Double moodScore,
        Map<String, Double> emotions,
        List<String> coreConcerns,
        String summary,
        List<String> growthTips,
        List<String> keyPhrases
) {}