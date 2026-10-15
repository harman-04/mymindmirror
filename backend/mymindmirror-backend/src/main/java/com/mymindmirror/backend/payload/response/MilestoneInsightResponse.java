package com.mymindmirror.backend.payload.response;
import com.mymindmirror.backend.enums.InsightStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record MilestoneInsightResponse(
        @Schema(description = "Summary of work remaining on this milestone.")
        String remainingWork,

        @Schema(description = "Assessment of the user's progress.")
        String performanceAssessment,

        @Schema(description = "Actionable tips. Each tip may contain markdown.")
        List<String> tips,

        @Schema(description = "Short encouragement message.")
        String encouragement,

        @Schema(description = "Suggested new tasks to help achieve the goal.")
        List<String> suggestedNewTasks,

        @Schema(
                description = "Insight generation status. ERROR when the AI is unavailable and the " +
                        "deterministic fallback payload is returned.",
                allowableValues = {"SUCCESS", "ERROR", "PARTIAL"})
        InsightStatus status
) {
    public static MilestoneInsightResponse createFallback() {
        return new MilestoneInsightResponse(
                "Unable to determine remaining work.",
                "AI analysis unavailable.",
                List.of("Check your Gemini API key and quota.", "Ensure your network is stable."),
                "Manual review is recommended. You can still track your progress manually.",
                List.of("Review incomplete tasks", "Set a new deadline if needed"),
                InsightStatus.ERROR
        );
    }
}