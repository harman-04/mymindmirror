package com.mymindmirror.backend.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record UserFullProfileResponse(
        UUID id,
        String username,
        String email,

        @Schema(description = "True when the user has stored their own Gemini API key.")
        boolean usingOwnKey,

        @Schema(
                description = "Masked form of the user's API key: last 4 characters visible, prefixed " +
                        "with '••••••••'. Null when the user has no personal key stored.",
                nullable = true)
        String maskedKey,

        UserRoadmapPreferencesDto roadmapPreferences,

        @Schema(
                description = "Weekly availability as JSON. Format: " +
                        "{\"monday\":[[\"09:00\",\"12:00\"],[\"13:00\",\"17:00\"]],...}.",
                nullable = true)
        String availableHoursJson,

        @Schema(description = "IANA timezone identifier.", nullable = true)
        String timezone,

        @Schema(description = "User's energy peak.",
                allowableValues = {"MORNING", "AFTERNOON", "EVENING"},
                nullable = true)
        String energyPeak,

        @Schema(description = "Wake time as HH:mm:ss string.", nullable = true)
        String wakeTime,

        @Schema(description = "Sleep time as HH:mm:ss string.", nullable = true)
        String sleepTime,

        @Schema(description = "Lunch time as HH:mm:ss string.", nullable = true)
        String lunchTime,

        @Schema(description = "Daily habits as a JSON array of strings.", nullable = true)
        String dailyHabitsJson
) {}