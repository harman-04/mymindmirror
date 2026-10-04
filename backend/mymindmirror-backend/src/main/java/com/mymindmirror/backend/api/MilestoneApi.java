package com.mymindmirror.backend.api;

import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.payload.request.ImportGrowthTipRequest;
import com.mymindmirror.backend.payload.request.MilestoneRequest;
import com.mymindmirror.backend.payload.response.ErrorResponse;
import com.mymindmirror.backend.payload.response.MessageResponse;
import com.mymindmirror.backend.payload.response.MilestoneInsightResponse;
import com.mymindmirror.backend.payload.response.MilestoneResponse;

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
 * OpenAPI contract for {@link com.mymindmirror.backend.controller.MilestoneController}.
 *
 * <p>Contains only documentation annotations. All Spring MVC binding
 * ({@code @PostMapping}, {@code @GetMapping}, {@code @PutMapping}, {@code @DeleteMapping},
 * {@code @PathVariable}, {@code @RequestBody}, {@code @Valid}, {@code @CurrentUser})
 * lives on the controller.
 */
@Tag(
        name = "Milestones",
        description = "Manage goal-oriented milestones for the authenticated user. A milestone groups tasks " +
                "toward a specific objective with an optional due date. Cross-user access is denied via 404 " +
                "(never 403), so no information is leaked about other users' resources."
)
public interface MilestoneApi {

    @Operation(
            summary = "Create a new milestone",
            description = "Creates a milestone owned by the authenticated user. New milestones start with status " +
                    "PENDING. A MILESTONE_CREATE gamification event is recorded (awards XP and may unlock badges)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Milestone created.",
                    content = @Content(schema = @Schema(implementation = MilestoneResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed (e.g. missing title), malformed JSON, or empty body.",
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
                            "gamification stats eviction on milestone creation.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<MilestoneResponse> createMilestone(
            @Parameter(hidden = true) User currentUser,
            MilestoneRequest milestoneRequest);

    @Operation(
            summary = "List all milestones for the authenticated user",
            description = "Returns every milestone owned by the current user, ordered by creation date descending. " +
                    "Each milestone includes its eagerly-loaded tasks."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Milestones returned (possibly empty list).",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = MilestoneResponse.class)))
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
    ResponseEntity<List<MilestoneResponse>> getAllMilestones(
            @Parameter(hidden = true) User currentUser);

    @Operation(
            summary = "Get a milestone by ID",
            description = "Returns a single milestone owned by the authenticated user, including its tasks. " +
                    "Returns 404 if the milestone does not exist or is not owned by the current user."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Milestone found.",
                    content = @Content(schema = @Schema(implementation = MilestoneResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated or invalid JWT.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Milestone not found or not owned by the current user. " +
                            "Message: \"Milestone not found or not owned.\".",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Database or dependent service temporarily unavailable.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<MilestoneResponse> getMilestoneById(
            @Parameter(hidden = true) User currentUser,
            @Parameter(description = "Milestone UUID", required = true,
                    example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            UUID id);

    @Operation(
            summary = "Update an existing milestone",
            description = "Updates the title, description, due date, or status of a milestone. All fields in " +
                    "the request body are optional; only non-null/non-blank values are applied. Returns 404 if " +
                    "the milestone does not exist or is not owned by the current user."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Milestone updated.",
                    content = @Content(schema = @Schema(implementation = MilestoneResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed (e.g. invalid status value), malformed JSON, or empty body.",
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
                    description = "Database or dependent service temporarily unavailable.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<MilestoneResponse> updateMilestone(
            @Parameter(hidden = true) User currentUser,
            @Parameter(description = "Milestone UUID", required = true,
                    example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            UUID id,
            MilestoneRequest milestoneRequest);

    @Operation(
            summary = "Delete a milestone",
            description = "Deletes the milestone and all of its tasks (cascade). Returns a confirmation message. " +
                    "Returns 404 if the milestone does not exist or is not owned by the current user."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Milestone deleted.",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))
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
                    description = "Database or dependent service temporarily unavailable.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<MessageResponse> deleteMilestone(
            @Parameter(hidden = true) User currentUser,
            @Parameter(description = "Milestone UUID", required = true,
                    example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            UUID id);

    @Operation(
            summary = "Get AI-generated insights for a milestone",
            description = "Analyzes the milestone and its tasks using the AI to produce: remaining-work summary, " +
                    "performance assessment, actionable tips, encouragement, and suggested new tasks. " +
                    "The insight service catches AI and cache failures internally and returns a fallback payload " +
                    "with status = ERROR at HTTP 200. Returns 404 if the milestone does not exist or is not owned " +
                    "by the current user. Database outages propagate as 503."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Insights returned. When the AI succeeds, 'status' is SUCCESS and fields are " +
                            "populated by the model. When the AI fails, 'status' is ERROR and the payload contains " +
                            "a deterministic fallback — the response code remains 200.",
                    content = @Content(schema = @Schema(implementation = MilestoneInsightResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated or invalid JWT.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Milestone not found or not owned by the current user. " +
                            "Message: \"Milestone not found or not owned.\".",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected runtime error outside the AI pipeline (rare). AI and Redis failures " +
                            "are caught in the service and returned as HTTP 200 with a fallback.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Database temporarily unavailable. Redis failures inside the insight service are " +
                            "caught internally and returned as HTTP 200 with a fallback insight payload — they do not " +
                            "surface as 503 on this endpoint.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<MilestoneInsightResponse> getMilestoneInsights(
            @Parameter(hidden = true) User currentUser,
            @Parameter(description = "Milestone UUID", required = true,
                    example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            UUID id);

    @Operation(
            summary = "Import a growth tip as a milestone with tasks",
            description = "Parses a free-text self-help / growth tip using the AI, then creates (or reuses) an " +
                    "'AI Growth Tips' milestone containing the extracted actionable tasks. On success returns a " +
                    "message with the number of tasks created. On AI exhaustion returns 503 with a message that " +
                    "depends on whether the user is using the shared key or their own key."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Growth tip imported. Response body contains a message of the form " +
                            "\"<N> tasks added to Milestones\".",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed (tipText is blank), malformed JSON, or empty body.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated or invalid JWT.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "AI returned zero tasks and no fallback is possible. " +
                            "Message: \"No tasks extracted from growth tip\".",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "AI models exhausted. Message depends on the key source: for shared-key users, " +
                            "\"The shared AI service is currently at capacity...\"; for users with their own key, " +
                            "\"Your Gemini API key has hit its rate limit...\". Database down also produces 503.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<MessageResponse> importGrowthTip(
            @Parameter(hidden = true) User currentUser,
            ImportGrowthTipRequest request);
}