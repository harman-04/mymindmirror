package com.mymindmirror.backend.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

public record DailyAggregatedDataResponse(
        LocalDate date,

        @Schema(
                description = "Average mood score for the day. Range: -1.0 (very negative) to 1.0 (very positive).",
                nullable = true)
        Double averageMood,

        @Schema(description = "Total words written across all entries for the day.", nullable = true)
        Long totalWords
) {}