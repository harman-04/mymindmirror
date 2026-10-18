package com.mymindmirror.backend.api;

import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.payload.request.ApiKeyRequest;
import com.mymindmirror.backend.payload.response.ApiKeySaveResponse;
import com.mymindmirror.backend.payload.response.ApiKeyStatusResponse;
import com.mymindmirror.backend.payload.response.ErrorResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;

/**
 * OpenAPI contract for {@link com.mymindmirror.backend.controller.ApiKeyController}.
 *
 * <p>Contains only documentation annotations. All Spring MVC binding
 * ({@code @PutMapping}, {@code @GetMapping}, {@code @RequestBody}, {@code @CurrentUser})
 * lives on the controller.
 */
@Tag(
        name = "API Key",
        description = "Manage the authenticated user's personal Gemini API key. Supplying a key enables " +
                "priority AI access with the user's own quota; clearing it falls back to the shared key. " +
                "The stored key is encrypted at rest and never returned in plain text — only a masked hint " +
                "is exposed via the status endpoint."
)
public interface ApiKeyApi {

    @Operation(
            summary = "Set or clear the user's Gemini API key",
            description = "If the supplied 'apiKey' is non-blank, it is validated against Google's API " +
                    "before saving. If Google confirms the key works, it is encrypted, stored, and cached " +
                    "model instances are evicted (status = VERIFIED). If Google rejects the key, the save is " +
                    "refused and the previously stored key (if any) is left untouched (status = REJECTED). " +
                    "If Google cannot be reached, the key is saved anyway (status = UNVERIFIED) so a " +
                    "third-party outage does not block a user from setting up their key. If 'apiKey' is " +
                    "null or blank, the stored key is cleared (status = CLEARED)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Save attempt completed. Inspect the 'status' field for the outcome.",
                    content = @Content(schema = @Schema(implementation = ApiKeySaveResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed (API key exceeds 250 characters), malformed JSON, or empty request body. " +
                            "Note: a blank 'apiKey' field is valid and means 'clear the stored key'.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
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
    ResponseEntity<ApiKeySaveResponse> updateApiKey(
            @Parameter(hidden = true) User currentUser,
            ApiKeyRequest request);

    @Operation(
            summary = "Get the current API key status",
            description = "Returns whether the user is using their own Gemini key (usingOwnKey), a masked hint of " +
                    "the stored key (maskedKey, null when no personal key is set), and a human-readable message. " +
                    "The result is cached in Redis for fast repeated access."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Status returned.",
                    content = @Content(schema = @Schema(implementation = ApiKeyStatusResponse.class))
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
    ResponseEntity<ApiKeyStatusResponse> getApiKeyStatus(
            @Parameter(hidden = true) User currentUser);
}