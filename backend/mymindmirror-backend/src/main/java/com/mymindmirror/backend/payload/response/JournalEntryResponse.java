package com.mymindmirror.backend.payload.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record JournalEntryResponse(
        UUID id,
        UserResponse user,

        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate entryDate,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime creationTimestamp,

        @Schema(description = "Journal text. Stored AES-encrypted at rest; decrypted for this response.")
        String rawText,

        @Schema(
                description = "Mood score. Range: -1.0 to 1.0. Null until asynchronous AI analysis completes.",
                nullable = true)
        Double moodScore,

        @Schema(
                description = "Emotion intensities. Map of emotion name to a score in [0.0, 1.0]. " +
                        "Null until AI analysis completes.",
                nullable = true)
        Map<String, Double> emotions,

        @Schema(description = "List of core concerns detected by the AI. Null until analysis completes.",
                nullable = true)
        List<String> coreConcerns,

        @Schema(description = "AI-generated summary of the entry. Null until analysis completes.",
                nullable = true)
        String summary,

        @Schema(description = "AI-generated growth tips (markdown). Null until analysis completes.",
                nullable = true)
        List<String> growthTips,

        @Schema(description = "Key phrases extracted by the AI. Null until analysis completes.",
                nullable = true)
        List<String> keyPhrases
) {}