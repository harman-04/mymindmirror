package com.mymindmirror.backend.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.Set;

public record UserStatsResponse(
        @Schema(description = "Current consecutive-day activity streak.")
        int currentStreak,

        @Schema(description = "Longest consecutive-day activity streak ever achieved.")
        int longestStreak,

        @Schema(description = "Date of the most recent recorded activity.", nullable = true)
        LocalDate lastActiveDate,

        @Schema(
                description = "Set of earned badge codes. Known values: FIRST_STEP, TASK_MASTER, " +
                        "PRODUCTIVITY_NINJA, FIRST_THOUGHT, REFLECTIVE_SOUL, TIME_LORD, FIRST_CHAT, " +
                        "AI_WHISPERER, VISIONARY, ARCHITECT, INTROSPECTIVE, DEEP_DIVER, " +
                        "THREE_DAY_STREAK, SEVEN_DAY_STREAK, THIRTY_DAY_LEGEND, ROADMAP_FINISHER.")
        Set<String> badges,

        @Schema(description = "Total tasks completed by the user.")
        int totalTasksCompleted,

        @Schema(description = "Total experience points earned.")
        int experiencePoints,

        @Schema(description = "Current user level. Derived as 1 + (experiencePoints / 500).")
        int level,

        @Schema(description = "Total journal entries written.")
        int totalJournalEntries,

        @Schema(description = "Total reflection-chat messages sent.")
        int totalChats,

        @Schema(description = "Total schedules generated.")
        int schedulesGenerated
) {}