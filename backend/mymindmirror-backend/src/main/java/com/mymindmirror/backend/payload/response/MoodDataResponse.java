package com.mymindmirror.backend.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

public record MoodDataResponse(
        LocalDate date,

        @Schema(description = "Mood score. Range: -1.0 to 1.0.")
        Double moodScore
) {}