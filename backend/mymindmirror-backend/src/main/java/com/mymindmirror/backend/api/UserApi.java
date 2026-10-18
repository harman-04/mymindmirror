package com.mymindmirror.backend.api;

import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.payload.request.ChangePasswordRequest;
import com.mymindmirror.backend.payload.request.UpdateRoadmapPreferencesRequest;
import com.mymindmirror.backend.payload.request.UpdateUserPreferencesRequest;
import com.mymindmirror.backend.payload.request.UserProfileRequest;
import com.mymindmirror.backend.payload.response.ErrorResponse;
import com.mymindmirror.backend.payload.response.MessageResponse;
import com.mymindmirror.backend.payload.response.UserFullProfileResponse;
import com.mymindmirror.backend.payload.response.UserPreferencesResponse;
import com.mymindmirror.backend.payload.response.UserProfileResponse;
import com.mymindmirror.backend.payload.response.UserRoadmapPreferencesDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;

/**
 * OpenAPI contract for {@link com.mymindmirror.backend.controller.UserController}.
 *
 * <p>Contains only documentation annotations. All Spring MVC binding
 * ({@code @PutMapping}, {@code @DeleteMapping}, {@code @GetMapping}, {@code @RequestBody},
 * {@code @Valid}, {@code @CurrentUser}) lives on the controller.
 */
@Tag(
        name = "User",
        description = "Authenticated user management: profile updates, password change, roadmap and general " +
                "preferences, and the aggregated full profile view. All endpoints require a valid JWT."
)
public interface UserApi {

    @Operation(
            summary = "Update the current user's profile",
            description = "Updates username and/or email for the authenticated user. Fields are optional: " +
                    "null or blank values are ignored. Username must be unique across users; email must be unique " +
                    "and valid. Returns the updated profile on success."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Profile updated (or unchanged if no valid fields were supplied).",
                    content = @Content(schema = @Schema(implementation = UserProfileResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed, or the new username/email is already taken.",
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
    ResponseEntity<UserProfileResponse> updateUserProfile(
            @Parameter(hidden = true) User currentUser,
            UserProfileRequest request);

    @Operation(
            summary = "Delete the current user's profile",
            description = "Permanently deletes the authenticated user along with all associated data " +
                    "across all storage layers (MySQL, PostgreSQL vectors, Redis chat sessions, " +
                    "Redis caches, and JVM memory). The database operations execute in a single transaction. " +
                    "Returns a confirmation message on success."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "User and all associated data deleted.",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "User not found.",
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
    ResponseEntity<MessageResponse> deleteUserProfile(
            @Parameter(hidden = true) User currentUser);

    @Operation(
            summary = "Change the current user's password",
            description = "Verifies the supplied current password, then re-encrypts all journal entries of the " +
                    "user with the new password-derived key before updating the stored hash. Fails with 400 if the " +
                    "current password is wrong or if the new password is identical to the old one. Failures during " +
                    "re-encryption abort the change and surface as 500."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Password changed successfully.",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed, current password is incorrect, or new password equals the old one.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated or invalid JWT.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Password change aborted because journal entries could not be re-encrypted.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Database or cache temporarily unavailable.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<MessageResponse> changePassword(
            @Parameter(hidden = true) User currentUser,
            ChangePasswordRequest request);

    @Operation(
            summary = "Update roadmap preferences",
            description = "Updates the user's roadmap preferences (difficulty, language, learning style, " +
                    "hours per week, avoid weekends). Blank/null fields are ignored. Missing preferences are " +
                    "created with defaults before applying the update."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Roadmap preferences updated.",
                    content = @Content(schema = @Schema(implementation = UserRoadmapPreferencesDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed (invalid difficulty, learning style, language length, or hours).",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated or invalid JWT.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Database or cache temporarily unavailable.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<UserRoadmapPreferencesDto> updateRoadmapPreferences(
            @Parameter(hidden = true) User currentUser,
            UpdateRoadmapPreferencesRequest request);

    @Operation(
            summary = "Get the full user profile",
            description = "Returns an aggregated view of the user's identity, API key status (masked), roadmap " +
                    "preferences, and general scheduling preferences in one call. Result is cached in Redis for " +
                    "fast repeated access."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Full profile returned.",
                    content = @Content(schema = @Schema(implementation = UserFullProfileResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated or invalid JWT.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Cache (Redis) or database temporarily unavailable.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<UserFullProfileResponse> getUserFullProfile(
            @Parameter(hidden = true) User currentUser);

    @Operation(
            summary = "Update user preferences",
            description = "Updates the user's general scheduling preferences: available-hours JSON, timezone, " +
                    "energy peak, wake/sleep/lunch times, and daily-habits JSON. Null fields are ignored. Missing " +
                    "preferences are created with defaults before applying the update."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Preferences updated.",
                    content = @Content(schema = @Schema(implementation = UserPreferencesResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed (e.g. invalid energy peak value).",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated or invalid JWT.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Database or cache temporarily unavailable.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<UserPreferencesResponse> updateUserPreferences(
            @Parameter(hidden = true) User currentUser,
            UpdateUserPreferencesRequest request);
}