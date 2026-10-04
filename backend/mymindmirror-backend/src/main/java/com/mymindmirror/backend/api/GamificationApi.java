package com.mymindmirror.backend.api;

import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.payload.response.ErrorResponse;
import com.mymindmirror.backend.payload.response.UserStatsResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;

/**
 * OpenAPI contract for {@link com.mymindmirror.backend.controller.GamificationController}.
 *
 * <p>Contains only documentation annotations. Spring MVC binding
 * ({@code @GetMapping}, {@code @CurrentUser}) lives on the controller.
 */
@Tag(
        name = "Gamification",
        description = "User XP, streaks, levels, and badges. The authenticated user's stats are computed from " +
                "their activity history (journal, tasks, schedules, chats, milestones, roadmap) and cached in " +
                "Redis. Fresh users with no recorded activity receive zeroed stats; the service initializes a " +
                "default record on first read."
)
public interface GamificationApi {

    @Operation(
            summary = "Get the authenticated user's gamification stats",
            description = "Returns the user's current and longest streaks, last active date, badge set, task and " +
                    "journal counters, experience points, current level, chat count, and schedules generated. " +
                    "Level is derived as 1 + (experiencePoints / 500). The response is cached in Redis for fast " +
                    "repeated access and invalidated on every recorded gamification activity."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Stats returned. A fresh user with no recorded activity returns zeroed stats " +
                            "with level = 1 and an empty badges array.",
                    content = @Content(schema = @Schema(implementation = UserStatsResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated or invalid JWT.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected error while computing stats (e.g. corrupted badges JSON, DB fault " +
                            "that does not surface as a resource-failure exception). " +
                            "Message: \"An unexpected error occurred.\".",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Cache (Redis) or database temporarily unavailable. Redis is used by the " +
                            "@Cacheable(GAMIFICATION_STATS) annotation on the service method.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<UserStatsResponse> getUserStats(
            @Parameter(hidden = true) User currentUser);
}