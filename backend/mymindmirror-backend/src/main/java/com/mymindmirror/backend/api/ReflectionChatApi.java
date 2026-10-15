package com.mymindmirror.backend.api;

import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.payload.request.ChatRequest;
import com.mymindmirror.backend.payload.response.ChatResponse;
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
 * OpenAPI contract for {@link com.mymindmirror.backend.controller.ReflectionChatController}.
 *
 * <p>Contains only documentation annotations. All Spring MVC binding
 * ({@code @PostMapping}, {@code @DeleteMapping}, {@code @RequestBody}, {@code @Valid},
 * {@code @RequestParam}, {@code @CurrentUser}) lives on the controller.
 */
@Tag(
        name = "Reflection Chat",
        description = "Chat with an AI coach that uses retrieval-augmented generation (RAG) over the user's " +
                "own journal history. The AI service itself is wrapped internally, so AI failures return a " +
                "friendly fallback message in the 'answer' field rather than an error status. The 503 status " +
                "is reserved for infrastructure failures (database or Redis)."
)
public interface ReflectionChatApi {

    @Operation(
            summary = "Ask the AI reflection coach a question",
            description = "Retrieves the most relevant past journal entries for the given query via vector " +
                    "similarity search, then asks the AI to answer in a warm, empathetic tone using that context. " +
                    "Falls back to the user's most recent entries when no semantically relevant entries are found. " +
                    "If 'rememberChat' is true and a non-blank 'sessionId' is provided, the exchange is stored in " +
                    "Redis for follow-up turns; otherwise the exchange is stateless. On any internal error " +
                    "(including AI outages) the endpoint still returns 200 with a friendly fallback message."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Answer returned. When the AI or an internal component fails, the 'answer' " +
                            "field contains a deterministic fallback such as \"I'm unable to answer right now. " +
                            "Please try again later.\" — the HTTP status remains 200.",
                    content = @Content(schema = @Schema(implementation = ChatResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed (query is blank), malformed JSON, or empty body.",
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
    ResponseEntity<ChatResponse> reflect(
            @Parameter(hidden = true) User currentUser,
            ChatRequest request);

    @Operation(
            summary = "Get a suggested reflective question",
            description = "Generates a single open-ended reflective question that bridges the user's current " +
                    "situation with their past journal history using RAG. The question is cached in-memory for " +
                    "5 minutes per user. On AI failure or when the AI returns a blank/null response, a " +
                    "deterministic default question is returned with HTTP 200."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Question returned. On AI failure, the default question " +
                            "\"What's one thing you've learned about yourself recently?\" is returned instead.",
                    content = @Content(schema = @Schema(implementation = ChatResponse.class))
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
    ResponseEntity<ChatResponse> suggestQuestion(
            @Parameter(hidden = true) User currentUser);

    @Operation(
            summary = "Clear the chat memory for a session",
            description = "Deletes the Redis-stored conversation history for the given sessionId. If the " +
                    "sessionId is blank, no Redis call is made and the endpoint returns 200 with an empty body. " +
                    "The sessionId parameter is required by the signature; missing it produces a 400."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Session cleared. Response body is empty."),
            @ApiResponse(
                    responseCode = "400",
                    description = "Missing required 'sessionId' query parameter.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
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
    ResponseEntity<Void> clearMemory(
            @Parameter(hidden = true) User currentUser,
            @Parameter(description = "Session identifier whose chat memory should be cleared.", required = true)
            String sessionId);
}