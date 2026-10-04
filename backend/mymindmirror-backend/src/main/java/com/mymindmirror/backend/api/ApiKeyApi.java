package com.mymindmirror.backend.api;

import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.payload.request.ApiKeyRequest;
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
            description = "If the supplied 'apiKey' is non-blank, it is encrypted and stored as the user's personal " +
                    "Gemini key, and the user's cached AI model instances are evicted so the new key takes effect " +
                    "immediately. If 'apiKey' is null or blank, the stored key is cleared and the user falls back " +
                    "to the shared key. Both paths return 204 No Content."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Key updated or cleared. No response body."
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Malformed JSON or empty request body. Note: a blank 'apiKey' field is valid " +
                            "and means 'clear the stored key' — it does not trigger validation.",
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
    ResponseEntity<Void> updateApiKey(
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