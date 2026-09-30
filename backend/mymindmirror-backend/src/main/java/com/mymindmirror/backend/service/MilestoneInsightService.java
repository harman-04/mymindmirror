package com.mymindmirror.backend.service;

import com.mymindmirror.backend.enums.AITask;
import com.mymindmirror.backend.enums.GamificationAction;
import com.mymindmirror.backend.model.Milestone;
import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.payload.request.TaskForInsightRequest;
import com.mymindmirror.backend.payload.response.MilestoneInsightResponse;
import com.mymindmirror.backend.service.ai.DynamicAiClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MilestoneInsightService {

    private final DynamicAiClientService aiClientService;
    private final GamificationService gamificationService;

    public MilestoneInsightResponse getMilestoneInsights(Milestone milestone) {
        User user = milestone.getUser();
        log.info("Requesting native Spring AI insights for milestone: {}", milestone.getTitle());

        // Tasks are already eagerly loaded by the controller, perfectly safe to map!
        List<TaskForInsightRequest> taskRequests = milestone.getTasks().stream()
                .map(task -> new TaskForInsightRequest(
                        task.getDescription(),
                        task.getDueDate(),
                        task.getStatus()
                ))
                .collect(Collectors.toList());

        String prompt = buildInsightsPrompt(
                milestone.getTitle(),
                milestone.getDescription(),
                milestone.getDueDate() != null ? milestone.getDueDate().toString() : null,
                milestone.getStatus().name(),
                milestone.getCompletionPercentage(),
                taskRequests
        );

        try {
            //  AI Call is completely synchronous and outside any database transactions
            MilestoneInsightResponse response = aiClientService.generateStructured(prompt, MilestoneInsightResponse.class, user.getId(), AITask.MILESTONE_INSIGHTS);

            // Record activity handles its own isolated micro-transaction
            gamificationService.recordActivity(user, GamificationAction.AI_INSIGHT);

            return response;
        } catch (Exception e) {
            log.error("Failed to get milestone insights from AI natively", e);
            return MilestoneInsightResponse.createFallback();
        }
    }

    private String buildInsightsPrompt(String title, String description, String dueDate,
                                       String status, double completionPercentage,
                                       List<TaskForInsightRequest> tasks) {
        StringBuilder tasksStr = new StringBuilder();
        if (!tasks.isEmpty()) {
            tasksStr.append("\nTasks:\n");
            for (TaskForInsightRequest task : tasks) {
                tasksStr.append(String.format("- %s (Status: %s, Due: %s)\n",
                        task.description(),
                        task.status() != null ? task.status() : "PENDING",
                        task.dueDate() != null ? task.dueDate() : "No due date"
                ));
            }
        } else {
            tasksStr.append("No specific tasks defined for this milestone.");
        }

        return String.format("""
    Analyze the following milestone and its associated tasks to provide comprehensive insights.
    **Language & Style Instruction:**
    - Detect the language(s) and style (casual, formal, motivational) of the milestone title and description.
    - Generate ALL text output (remainingWork, performanceAssessment, tips, encouragement, suggestedNewTasks) in the **same language(s) and style**.
    - Use markdown formatting where appropriate (e.g., **bold**, bullet points, `> quotes`) to make insights clearer and more actionable.

    Focus on:
    1.  **Remaining Work:** What specific tasks are left, and how much time is remaining if a due date is present. Write as a short paragraph, possibly with bullet points.
    2.  **Performance Assessment:** How well is the user progressing? Are they on track, falling behind, or excelling? Provide a concise evaluation.
    3.  **Actionable Tips:** Provide 2-3 practical, step‑by‑step tips to help the user progress or improve. Each tip should be a short paragraph with bullet points or numbered steps.
    4.  **Encouragement:** Offer a brief, encouraging statement (1-2 sentences) in the same tone.
    5.  **New Task Suggestions:** Suggest 1-2 concrete, next‑step tasks to help achieve the goal, especially if it's stalled. Each suggested task should be a short phrase (as a string).

    Milestone Details:
    Title: %s
    Description: %s
    Due Date: %s
    Current Status: %s
    Completion Percentage: %.1f%%
    %s

    Provide the response as a JSON object with the following structure. Use markdown (e.g., **bold**, bullet points) inside the string fields where helpful.
    {
        "remainingWork": "string (summary of what's left, may contain markdown)",
        "performanceAssessment": "string (how they're doing, may contain markdown)",
        "tips": ["string (markdown allowed)", "string", ...],
        "encouragement": "string (plain text or markdown)",
        "suggestedNewTasks": ["string", "string", ...],
        "status": "string (e.g., 'SUCCESS', 'ERROR', 'PARTIAL')"
    }
    Ensure the "status" field is always included, indicating the success of insight generation. It must be strictly one of: 'SUCCESS', 'ERROR', 'PARTIAL'.
    """, title, description, dueDate != null ? dueDate : "Not set",
                status, completionPercentage, tasksStr.toString());
    }
}