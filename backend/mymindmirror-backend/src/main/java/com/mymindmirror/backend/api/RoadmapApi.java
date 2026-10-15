package com.mymindmirror.backend.api;

import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.payload.request.ImportTaskRequest;
import com.mymindmirror.backend.payload.request.RoadmapGenerateRequest;
import com.mymindmirror.backend.payload.response.ElaborationResponseDto;
import com.mymindmirror.backend.payload.response.ErrorResponse;
import com.mymindmirror.backend.payload.response.RoadmapResponse;

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
 * OpenAPI contract for {@link com.mymindmirror.backend.controller.RoadmapController}.
 *
 * <p>Contains only documentation annotations. All Spring MVC binding
 * ({@code @PostMapping}, {@code @GetMapping}, {@code @DeleteMapping}, {@code @PatchMapping},
 * {@code @PathVariable}, {@code @RequestParam}, {@code @RequestBody}, {@code @Valid},
 * {@code @CurrentUser}) lives on the controller.
 */
@Tag(
        name = "Roadmap",
        description = "AI-generated personalized roadmaps: generate, continue, elaborate, reschedule, and manage " +
                "individual tasks. Initial generation and elaboration fall back to deterministic content when " +
                "the AI is unavailable; continuation, reschedule, and batch-continuation surface a 503 instead. " +
                "Cross-user access is denied with a 404 (message varies by operation)."
)
public interface RoadmapApi {

    @Operation(
            summary = "Generate a personalized roadmap",
            description = "Builds a roadmap for the given goal using the user's roadmap preferences (difficulty, " +
                    "language, learning style, hours/week, avoid weekends). The service caps detailed generation " +
                    "at 12 weeks even if a longer timeframe is requested. On AI failure, a deterministic fallback " +
                    "roadmap is persisted, so this endpoint does not return 503 for AI outages."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Roadmap generated. When the AI fails, the persisted roadmap is the fallback " +
                            "variant and its title contains \"Personalized Roadmap\".",
                    content = @Content(schema = @Schema(implementation = RoadmapResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed (goal is blank), malformed JSON, or empty body.",
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
                            "gamification stats eviction during roadmap creation.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<RoadmapResponse> generateRoadmap(
            @Parameter(hidden = true) User currentUser,
            RoadmapGenerateRequest request);

    @Operation(
            summary = "List all roadmaps for the authenticated user",
            description = "Returns every roadmap owned by the current user, ordered by creation date descending. " +
                    "Each roadmap includes its tasks, resources, and milestones."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Roadmaps returned (possibly empty list).",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = RoadmapResponse.class)))
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
    ResponseEntity<List<RoadmapResponse>> getUserRoadmaps(
            @Parameter(hidden = true) User currentUser);

    @Operation(
            summary = "Delete a roadmap",
            description = "Deletes the roadmap and its cascading tasks, resources, and milestones. Returns 200 with " +
                    "an empty body on success. Returns 404 if the roadmap does not exist or is not owned by the " +
                    "current user (message: \"Roadmap not found\")."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Roadmap deleted. Response body is empty."),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated or invalid JWT.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Roadmap not found or not owned by the current user. " +
                            "Message: \"Roadmap not found\".",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Database or dependent service temporarily unavailable.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<Void> deleteRoadmap(
            @Parameter(hidden = true) User currentUser,
            @Parameter(description = "Roadmap UUID", required = true,
                    example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            UUID id);

    @Operation(
            summary = "Import a roadmap task into a milestone",
            description = "Copies the given roadmap task into a milestone named \"Roadmap: <roadmap title>\", " +
                    "creating the milestone if it does not already exist. The roadmap task is marked as imported. " +
                    "Returns 200 with the plain-text body \"Task imported successfully\"."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Task imported. Response body is the plain text \"Task imported successfully\".",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string",
                            example = "Task imported successfully"))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed (missing roadmapId or taskId), malformed JSON, or empty body.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated or invalid JWT.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Roadmap not found, or the task does not belong to the roadmap. " +
                            "Messages: \"Roadmap not found\" or \"Task not found in roadmap\".",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Database or cache (Redis) temporarily unavailable.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<String> importTaskToMilestone(
            @Parameter(hidden = true) User currentUser,
            ImportTaskRequest request);

    @Operation(
            summary = "Toggle a roadmap task's completion",
            description = "Flips the completed flag of the given roadmap task. When the task becomes completed, " +
                    "a TASK gamification event is recorded, and if all tasks in the roadmap are now completed, " +
                    "the ROADMAP_FINISHER badge is awarded. Returns 200 with an empty body. Returns 404 if the " +
                    "task does not exist or is not owned by the current user (message: \"Task not found\")."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task completion toggled. Response body is empty."),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated or invalid JWT.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Task not found or not owned by the current user. " +
                            "Message: \"Task not found\".",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Database or cache (Redis) temporarily unavailable.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<Void> toggleTaskCompletion(
            @Parameter(hidden = true) User currentUser,
            @Parameter(description = "Roadmap task UUID", required = true,
                    example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            UUID taskId);

    @Operation(
            summary = "Continue a roadmap (3 more weeks)",
            description = "Asks the AI to generate 3 additional weeks of tasks, continuing from the highest week " +
                    "currently present. Requires at least one completed task; if none exist, returns 400. " +
                    "If the AI fails or returns no tasks, returns 503 with an AI-specific message."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Continuation saved. Returns the updated roadmap with new tasks appended.",
                    content = @Content(schema = @Schema(implementation = RoadmapResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "No completed tasks yet. " +
                            "Message: \"No completed tasks yet. Complete some tasks before continuing.\".",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated or invalid JWT.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Roadmap not found or not owned by the current user. " +
                            "Message: \"Roadmap not found\".",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "AI couldn't generate the next roadmap steps. " +
                            "Message: \"AI couldn't generate the next roadmap steps this time. Please try again " +
                            "in a moment.\". Also used when the database is unavailable.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<RoadmapResponse> continueRoadmap(
            @Parameter(hidden = true) User currentUser,
            @Parameter(description = "Roadmap UUID", required = true,
                    example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            UUID id);

    @Operation(
            summary = "Elaborate a roadmap task with AI",
            description = "Generates detailed instructions and subtasks for the given roadmap task. When 'enhance' " +
                    "is true, the AI is asked for a longer, more comprehensive elaboration. On AI failure, a " +
                    "deterministic fallback is used (task description as details, empty subtasks), so this " +
                    "endpoint does not surface AI 503s. Records an ELABORATE_TASK gamification event."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Elaboration saved and returned. On AI failure, 'details' falls back to the " +
                            "task description and 'subtasks' is empty.",
                    content = @Content(schema = @Schema(implementation = ElaborationResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated or invalid JWT.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Task not found or not owned by the current user. " +
                            "Message: \"Task not found\".",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Database or cache (Redis) temporarily unavailable.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<ElaborationResponseDto> elaborateTask(
            @Parameter(hidden = true) User currentUser,
            @Parameter(description = "Roadmap task UUID", required = true,
                    example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            UUID taskId,
            @Parameter(description = "When true, requests a longer, more comprehensive elaboration.")
            boolean enhance);

    @Operation(
            summary = "Reschedule a roadmap",
            description = "Asks the AI to reassign weeks for the incomplete tasks in the roadmap. If all tasks are " +
                    "already completed, returns 400. On AI failure, returns 503 with an AI-specific message."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Roadmap rescheduled. Returns the updated roadmap.",
                    content = @Content(schema = @Schema(implementation = RoadmapResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "All tasks are already completed. " +
                            "Message: \"All tasks are already completed.\".",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated or invalid JWT.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Roadmap not found or not owned by the current user. " +
                            "Message: \"Roadmap not found\".",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "AI couldn't reschedule the roadmap. " +
                            "Message: \"AI couldn't reschedule your roadmap this time. Please try again in a " +
                            "moment.\". Also used when the database is unavailable.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<RoadmapResponse> rescheduleRoadmap(
            @Parameter(hidden = true) User currentUser,
            @Parameter(description = "Roadmap UUID", required = true,
                    example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            UUID id);

    @Operation(
            summary = "Continue a roadmap in batch",
            description = "Generates the next chunk of weeks for the roadmap, replacing any existing tasks for " +
                    "those weeks. The chunk size defaults to min(remaining weeks, 12) unless 'weeksToGenerate' is " +
                    "supplied. Returns 400 if the roadmap is missing duration data, is already fully generated, " +
                    "or has no weeks left. Returns 503 if the AI fails."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Batch continuation saved. Returns the updated roadmap.",
                    content = @Content(schema = @Schema(implementation = RoadmapResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Roadmap is missing duration data, is already fully generated, or has no weeks " +
                            "left to generate. Messages vary between the three cases.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated or invalid JWT.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Roadmap not found or not owned by the current user. " +
                            "Message: \"Roadmap not found\".",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "AI couldn't generate additional roadmap weeks. " +
                            "Message: \"AI couldn't generate additional roadmap weeks this time. Please try again " +
                            "in a moment.\". Also used when the database is unavailable.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<RoadmapResponse> continueRoadmapBatch(
            @Parameter(hidden = true) User currentUser,
            @Parameter(description = "Roadmap UUID", required = true,
                    example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            UUID id,
            @Parameter(description = "Optional number of weeks to generate in this batch. If omitted, defaults " +
                    "to the smaller of the remaining weeks or 12.")
            Integer weeksToGenerate);
}