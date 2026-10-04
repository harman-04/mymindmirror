package com.mymindmirror.backend.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import java.time.LocalTime;

public record UpdateUserPreferencesRequest(
        @Schema(
                description = "Weekly availability as JSON. Format: " +
                        "{\"monday\":[[\"09:00\",\"12:00\"],[\"13:00\",\"17:00\"]],\"tuesday\":[],...}. " +
                        "Keys are lowercase day names; values are arrays of [start,end] time pairs.",
                nullable = true)
        String availableHoursJson,

        @Schema(
                description = "IANA timezone identifier, e.g. 'Asia/Kolkata', 'America/New_York'.",
                nullable = true)
        String timezone,

        @Pattern(regexp = "MORNING|AFTERNOON|EVENING", message = "Energy peak must be MORNING, AFTERNOON, or EVENING")
        @Schema(
                description = "User's energy peak. Optional.",
                allowableValues = {"MORNING", "AFTERNOON", "EVENING"},
                nullable = true)
        String energyPeak,

        LocalTime wakeTime,

        LocalTime sleepTime,

        LocalTime lunchTime,

        @Schema(
                description = "Daily habits as a JSON array of strings, e.g. [\"30 min workout\", \"Read 10 pages\"].",
                nullable = true)
        String dailyHabitsJson
) {}