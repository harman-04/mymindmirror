package com.mymindmirror.backend.api;

import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.payload.response.ErrorResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;

import java.util.Map;

/**
 * OpenAPI contract for {@link com.mymindmirror.backend.controller.ReflectionController}.
 */
@Tag(
        name = "Reflection",
        description = "Generate an AI-powered daily reflection from the user's journal entries. The reflection " +
                "summarizes today's overall emotional state and offers a short, encouraging thought. If the " +
                "user has not journaled today, a friendly prompt is returned instead of a reflection. AI " +
                "failures and Redis failures inside the reflection service are caught internally and return a " +
                "fallback string with HTTP 200; database outages propagate as 503."
)
public interface ReflectionApi {

    @Operation(
            summary = "Generate today's reflection",
            description = "Reads the authenticated user's journal entries for today, aggregates their detected " +
                    "emotions and core concerns, and asks the AI to compose a 2–5 sentence reflection in the " +
                    "same language(s) and style as the entries. If no entries exist for today, returns the " +
                    "fixed prompt \"Journal an entry today to get your daily reflection!\" instead of calling " +
                    "the AI. AI failures and Redis cache failures are caught inside the service and returned " +
                    "as a 200 with a fallback reflection string, so this endpoint does not surface those as " +
                    "errors. Database outages propagate to the global exception handler as 503."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Reflection returned as a JSON object with a single 'reflection' field. When " +
                            "the user has no journal entries today, that field contains the fixed prompt " +
                            "\"Journal an entry today to get your daily reflection!\". On AI failure, it contains " +
                            "\"Failed to generate reflection due to an internal error.\".",
                    content = @Content(schema = @Schema(implementation = Map.class,
                            example = "{\"reflection\": \"You have grown immensely this week. Keep going!\"}"))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated or invalid JWT.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected runtime error in the aggregation pipeline (e.g. NullPointerException, " +
                            "unhandled data corruption). AI and Redis failures are caught in the service and " +
                            "return 200 with a fallback instead.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Database temporarily unavailable. Redis failures inside the reflection service are " +
                            "caught internally and returned as HTTP 200 with a fallback reflection string — they do not " +
                            "surface as 503 on this endpoint.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<Map<String, String>> generateDailyReflection(
            @Parameter(hidden = true) User currentUser);
}