package com.mymindmirror.backend.api;

import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.payload.request.CustomTaskRequest;
import com.mymindmirror.backend.payload.response.CustomTaskResponse;
import com.mymindmirror.backend.payload.response.ErrorResponse;

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
 * OpenAPI contract for {@link com.mymindmirror.backend.controller.CustomTaskController}.
 *
 * <p>Contains only documentation annotations. All Spring MVC binding
 * ({@code @GetMapping}, {@code @PostMapping}, {@code @PutMapping}, {@code @DeleteMapping},
 * {@code @PathVariable}, {@code @RequestBody}, {@code @Valid}, {@code @CurrentUser})
 * lives on the controller.
 */
@Tag(
        name = "Custom Tasks",
        description = "Standalone user tasks (not tied to milestones or roadmaps). Custom tasks can be scheduled " +
                "onto the user's calendar via the Schedule endpoints. Cross-user access is denied with a 404 " +
                "and the message \"Custom task not found\"."
)
public interface CustomTaskApi {

    @Operation(
            summary = "List all custom tasks for the authenticated user",
            description = "Returns every custom task owned by the current user, ordered by creation date " +
                    "descending. Includes completed and incomplete tasks."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Custom tasks returned (possibly empty list).",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = CustomTaskResponse.class)))
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
    ResponseEntity<List<CustomTaskResponse>> getUserTasks(
            @Parameter(hidden = true) User currentUser);

    @Operation(
            summary = "Create a custom task",
            description = "Creates a new custom task owned by the authenticated user. Missing optional fields are " +
                    "defaulted by the service (estimatedHours = 1.0, priority = MEDIUM, description = \"\"). " +
                    "A TASK_CREATE gamification event is recorded, awarding XP."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Custom task created.",
                    content = @Content(schema = @Schema(implementation = CustomTaskResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed (title blank or estimatedHours missing), malformed JSON, " +
                            "or empty body.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated or invalid JWT.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Database or cache (Redis) temporarily unavailable. Redis is used for " +
                            "gamification stats eviction on task creation.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<CustomTaskResponse> createCustomTask(
            @Parameter(hidden = true) User currentUser,
            CustomTaskRequest request);

    @Operation(
            summary = "Update an existing custom task",
            description = "Updates the custom task's fields. If the task transitions from not-completed to completed " +
                    "and it has any linked scheduled calendar entries, those entries are updated to match (title, " +
                    "priority, completed flag). A TASK gamification event is recorded on completion. " +
                    "Returns 404 if the task does not exist or is not owned by the current user."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Custom task updated.",
                    content = @Content(schema = @Schema(implementation = CustomTaskResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed (title blank or estimatedHours missing), malformed JSON, " +
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
                    description = "Custom task not found or not owned by the current user. " +
                            "Message: \"Custom task not found\".",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Database or cache (Redis) temporarily unavailable.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<CustomTaskResponse> updateCustomTask(
            @Parameter(hidden = true) User currentUser,
            @Parameter(description = "Custom task UUID", required = true,
                    example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            UUID id,
            CustomTaskRequest request);

    @Operation(
            summary = "Delete a custom task",
            description = "Deletes the custom task and all associated calendar entries (linked scheduled tasks " +
                    "with the same customTaskId). Returns 204 No Content on success. Returns 404 if the task does " +
                    "not exist or is not owned by the current user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Custom task and its scheduled entries deleted."),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated or invalid JWT.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Custom task not found or not owned by the current user. " +
                            "Message: \"Custom task not found\".",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Database or dependent service temporarily unavailable.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<Void> deleteCustomTask(
            @Parameter(hidden = true) User currentUser,
            @Parameter(description = "Custom task UUID", required = true,
                    example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            UUID id);
}