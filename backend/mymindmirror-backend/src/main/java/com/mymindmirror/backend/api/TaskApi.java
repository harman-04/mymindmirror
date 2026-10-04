package com.mymindmirror.backend.api;

import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.payload.request.TaskRequest;
import com.mymindmirror.backend.payload.response.ErrorResponse;
import com.mymindmirror.backend.payload.response.MessageResponse;
import com.mymindmirror.backend.payload.response.TaskResponse;

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
 * OpenAPI contract for {@link com.mymindmirror.backend.controller.TaskController}.
 *
 * <p>Contains only documentation annotations. All Spring MVC binding
 * ({@code @PostMapping}, {@code @GetMapping}, {@code @PutMapping}, {@code @DeleteMapping},
 * {@code @PathVariable}, {@code @RequestBody}, {@code @Valid}, {@code @CurrentUser})
 * lives on the controller.
 */
@Tag(
        name = "Tasks",
        description = "Manage tasks within a milestone. All endpoints require the milestone to belong to the " +
                "authenticated user; otherwise a 404 is returned (no information is leaked about other users' " +
                "milestones). Changing a task's status to COMPLETED awards XP, may unlock badges, and " +
                "synchronizes the linked roadmap task if present."
)
public interface TaskApi {

    @Operation(
            summary = "Create a task inside a milestone",
            description = "Creates a task owned by the authenticated user, associated with the given milestone. " +
                    "New tasks default to status PENDING unless the request explicitly specifies a status. " +
                    "The parent milestone's status is recalculated after creation, and a TASK_CREATE gamification " +
                    "event is recorded."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Task created successfully.",
                    content = @Content(schema = @Schema(implementation = TaskResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed (description is blank), malformed JSON, or empty body.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated or invalid JWT.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Milestone not found or not owned by the current user. " +
                            "Message: \"Milestone not found or not owned by user.\".",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Database, cache, or dependent service temporarily unavailable.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<TaskResponse> createTask(
            @Parameter(hidden = true) User currentUser,
            @Parameter(description = "Parent milestone UUID", required = true,
                    example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            UUID milestoneId,
            TaskRequest taskRequest);

    @Operation(
            summary = "List all tasks for a milestone",
            description = "Returns all tasks belonging to the given milestone, ordered by creation timestamp ascending. " +
                    "Returns 404 if the milestone does not exist or is not owned by the current user."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Tasks returned (possibly empty list).",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = TaskResponse.class)))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated or invalid JWT.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Milestone not found or not owned by the current user.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Database or dependent service temporarily unavailable.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<List<TaskResponse>> getAllTasksForMilestone(
            @Parameter(hidden = true) User currentUser,
            @Parameter(description = "Parent milestone UUID", required = true,
                    example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            UUID milestoneId);

    @Operation(
            summary = "Get a single task by ID",
            description = "Returns a task if it belongs to the given milestone and the milestone belongs to the " +
                    "current user. Returns 404 if either the milestone is not owned or the task does not exist " +
                    "within that milestone."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Task found.",
                    content = @Content(schema = @Schema(implementation = TaskResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated or invalid JWT.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Milestone not found or not owned by user, OR task not found within the milestone. " +
                            "Task-specific message: \"Task not found or not owned by user/milestone.\".",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Database or dependent service temporarily unavailable.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<TaskResponse> getTaskById(
            @Parameter(hidden = true) User currentUser,
            @Parameter(description = "Parent milestone UUID", required = true,
                    example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            UUID milestoneId,
            @Parameter(description = "Task UUID", required = true,
                    example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
            UUID taskId);

    @Operation(
            summary = "Update an existing task",
            description = "Updates a task using a TaskRequest DTO. Null fields are ignored (partial update). " +
                    "When the task's status changes to COMPLETED, the user receives XP, badges may be unlocked, " +
                    "and the linked roadmap task (if any) is marked complete. The parent milestone's status is " +
                    "recalculated after the update."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Task updated successfully.",
                    content = @Content(schema = @Schema(implementation = TaskResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed (description is blank), malformed JSON, or empty body.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated or invalid JWT.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Milestone or task not found, or not owned by the current user. " +
                            "Task-specific message: \"Task not found or not owned by user/milestone.\".",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Database or cache (Redis) temporarily unavailable. Redis is used for " +
                            "gamification stats eviction when the status changes to COMPLETED.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<TaskResponse> updateTask(
            @Parameter(hidden = true) User currentUser,
            @Parameter(description = "Parent milestone UUID", required = true,
                    example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            UUID milestoneId,
            @Parameter(description = "Task UUID", required = true,
                    example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
            UUID taskId,
            TaskRequest taskRequest);

    @Operation(
            summary = "Delete a task",
            description = "Deletes a task and recalculates the parent milestone's completion percentage and status. " +
                    "Returns a confirmation message on success."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Task deleted successfully.",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated or invalid JWT.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Milestone or task not found, or not owned by the current user. " +
                            "Task-specific message: \"Task not found or not owned by user/milestone.\".",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Database or dependent service temporarily unavailable.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<MessageResponse> deleteTask(
            @Parameter(hidden = true) User currentUser,
            @Parameter(description = "Parent milestone UUID", required = true,
                    example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            UUID milestoneId,
            @Parameter(description = "Task UUID", required = true,
                    example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
            UUID taskId);
}