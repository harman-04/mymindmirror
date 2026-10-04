package com.mymindmirror.backend.api;

import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.payload.request.MoveScheduledTaskRequest;
import com.mymindmirror.backend.payload.request.ScheduleCustomTaskRequest;
import com.mymindmirror.backend.payload.response.ErrorResponse;
import com.mymindmirror.backend.payload.response.ScheduledTaskResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

/**
 * OpenAPI contract for {@link com.mymindmirror.backend.controller.ScheduleController}.
 *
 * <p>Contains only documentation annotations. All Spring MVC binding
 * ({@code @PostMapping}, {@code @GetMapping}, {@code @PutMapping}, {@code @PatchMapping},
 * {@code @PathVariable}, {@code @RequestParam}, {@code @RequestBody}, {@code @Valid},
 * {@code @CurrentUser}) lives on the controller.
 */
@Tag(
        name = "Schedule",
        description = "AI-generated daily schedules, task rescheduling, and calendar sync. The scheduler cascades " +
                "through multiple AI models and falls back to a deterministic local scheduler if the AI is " +
                "unavailable, so /generate and /reoptimize never fail due to AI outages — they degrade to the " +
                "fallback scheduler instead. Cross-user access to scheduled tasks is denied with a 404 and the " +
                "message \"Task not found\"."
)
public interface ScheduleApi {

    @Operation(
            summary = "Generate an AI schedule for the current user",
            description = "Collects the user's unscheduled tasks (roadmap, milestone, and custom tasks), calls the " +
                    "AI to build a schedule for the next 3 days, and falls back to a deterministic scheduler if " +
                    "the AI is unavailable. Completes with a plain-text body \"Schedule generated\". The optional " +
                    "'mode' query parameter restricts which tasks are considered; the only special value is " +
                    "'custom' (custom tasks only). Any other value (including the default 'all') schedules every " +
                    "task type."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Schedule generated successfully. Response body is the plain text " +
                            "\"Schedule generated\".",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string",
                            example = "Schedule generated"))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated or invalid JWT.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Database or cache (Redis) temporarily unavailable. Redis is used for " +
                            "gamification stats eviction during schedule generation.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<String> generateSchedule(
            @Parameter(hidden = true) User currentUser,
            @Parameter(description = "Scheduling mode. Default 'all'. Special value 'custom' restricts to custom tasks only.")
            String mode);

    @Operation(
            summary = "Re-optimize the rest of today",
            description = "Regenerates the remainder of today's schedule, deleting only incomplete scheduled tasks " +
                    "that fall on the current day and preserving completed ones. Falls back to a deterministic " +
                    "scheduler if the AI is unavailable. Completes with a plain-text body \"Today re-optimized\"."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Today's schedule re-optimized. Response body is the plain text " +
                            "\"Today re-optimized\".",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string",
                            example = "Today re-optimized"))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated or invalid JWT.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Database or cache (Redis) temporarily unavailable.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<String> reoptimizeToday(
            @Parameter(hidden = true) User currentUser);

    @Operation(
            summary = "List scheduled tasks in a date range",
            description = "Returns all scheduled tasks for the authenticated user whose scheduledDate falls " +
                    "between startDate and endDate (inclusive). Both parameters are required and must be in " +
                    "yyyy-MM-dd format; startDate must not be after endDate. Returns 400 if either is missing, " +
                    "malformed, or out of order."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Scheduled tasks returned (possibly empty list).",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ScheduledTaskResponse.class)))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed: startDate or endDate missing, invalid yyyy-MM-dd format, or " +
                            "startDate is after endDate.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated or invalid JWT.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Database or dependent service temporarily unavailable.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<List<ScheduledTaskResponse>> getScheduledTasks(
            @Parameter(hidden = true) User currentUser,
            @Parameter(description = "Start date in yyyy-MM-dd format.", required = true, example = "2026-01-01")
            String startDate,
            @Parameter(description = "End date in yyyy-MM-dd format.", required = true, example = "2026-12-31")
            String endDate);

    @Operation(
            summary = "Move a scheduled task to a new date and time",
            description = "Updates the scheduled date, start time, and end time of the given scheduled task. " +
                    "Returns 404 if the task does not exist or is not owned by the current user " +
                    "(message: \"Task not found\")."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task moved. Response body is empty."),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed (date, startTime, or endTime missing), malformed JSON, " +
                            "or empty body.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated or invalid JWT.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Scheduled task not found or not owned by the current user. " +
                            "Message: \"Task not found\".",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Database or dependent service temporarily unavailable.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<Void> moveScheduledTask(
            @Parameter(hidden = true) User currentUser,
            @Parameter(description = "Scheduled task UUID", required = true,
                    example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            UUID taskId,
            MoveScheduledTaskRequest request);

    @Operation(
            summary = "Mark a scheduled task as complete",
            description = "Marks the given scheduled task as completed. If the task is linked to a custom task, " +
                    "that custom task (and any other scheduled entries pointing to the same custom task) is also " +
                    "marked complete. Records a TASK gamification event. Returns 404 if the scheduled task does " +
                    "not exist or is not owned by the current user (message: \"Task not found\")."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task completed. Response body is empty."),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated or invalid JWT.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Scheduled task not found or not owned by the current user. " +
                            "Message: \"Task not found\".",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Database or cache (Redis) temporarily unavailable.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<Void> completeTask(
            @Parameter(hidden = true) User currentUser,
            @Parameter(description = "Scheduled task UUID", required = true,
                    example = "3fa85f64-5717-4562-b3fc-2c963faf-afa6")
            UUID taskId);

    @Operation(
            summary = "Manually schedule a custom task onto the calendar",
            description = "Creates a scheduled calendar entry directly (bypassing the AI). Used when the user " +
                    "wants to pick the exact date and time for a custom task. Returns 200 with an empty body on " +
                    "success; validation failure (missing customTaskId, title, date, startTime, or endTime) " +
                    "produces 400."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Scheduled entry created. Response body is empty."),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed (customTaskId, title, date, startTime, or endTime missing), " +
                            "malformed JSON, or empty body.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated or invalid JWT.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Database or dependent service temporarily unavailable.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<Void> scheduleCustomTaskManually(
            @Parameter(hidden = true) User currentUser,
            ScheduleCustomTaskRequest request);
}