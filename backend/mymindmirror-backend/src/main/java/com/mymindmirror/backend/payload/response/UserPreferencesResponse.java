package com.mymindmirror.backend.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record UserPreferencesResponse(
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

        @Schema(
                description = "User's energy peak.",
                allowableValues = {"MORNING", "AFTERNOON", "EVENING"},
                nullable = true)
        String energyPeak,

        @Schema(description = "Wake time as HH:mm:ss string.", nullable = true)
        String wakeTime,

        @Schema(description = "Sleep time as HH:mm:ss string.", nullable = true)
        String sleepTime,

        @Schema(description = "Lunch time as HH:mm:ss string.", nullable = true)
        String lunchTime,

        @Schema(
                description = "Daily habits as a JSON array of strings, e.g. [\"30 min workout\", \"Read 10 pages\"].",
                nullable = true)
        String dailyHabitsJson
) {}