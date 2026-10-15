package com.mymindmirror.backend.api;

import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.payload.request.JournalEntryRequest;
import com.mymindmirror.backend.payload.response.ErrorResponse;
import com.mymindmirror.backend.payload.response.JournalEntryResponse;
import com.mymindmirror.backend.payload.response.MoodDataResponse;
import com.mymindmirror.backend.payload.response.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * OpenAPI contract for {@link com.mymindmirror.backend.controller.JournalController}.
 *
 * <p>This interface contains ONLY documentation annotations. All Spring MVC binding
 * (request mappings, {@code @RequestBody}, {@code @PathVariable}, {@code @RequestParam},
 * {@code @CurrentUser}, {@code @Valid}, {@code @PageableDefault}) lives on the controller
 * implementation. SpringDoc merges both sources when generating {@code /v3/api-docs}.
 */
@Tag(
        name = "Journal",
        description = "Secure, encrypted journaling. Entries are AES-encrypted at rest; reads decrypt server-side. " +
                "Includes keyword, mood, and semantic search plus AI-powered analytics. " +
                "AI analysis (emotions, summary, growth tips) runs asynchronously after create/update, so newly " +
                "created or edited entries may briefly have moodScore = null and empty AI fields."
)
public interface JournalApi {

    // ==================================================================
    // CRUD
    // ==================================================================

    @Operation(
            summary = "Create a new journal entry",
            description = "Stores an encrypted journal entry. AI analysis (emotions, summary, key phrases, growth tips) " +
                    "runs asynchronously after the response is returned. The entry will have moodScore = null and empty AI " +
                    "fields until analysis completes (typically a few seconds)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Entry created. AI analysis is scheduled asynchronously.",
                    content = @Content(schema = @Schema(implementation = JournalEntryResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed (rawText is blank), malformed JSON, or empty body. " +
                            "Response body is the unified ErrorResponse.",
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
    ResponseEntity<JournalEntryResponse> createJournalEntry(
            @Parameter(hidden = true) User currentUser,
            JournalEntryRequest request);

    @Operation(
            summary = "Update an existing journal entry",
            description = "Updates the raw text of an entry. If the text changed, AI analysis is reset and re-run " +
                    "asynchronously. Returns 404 if the entry does not exist or is not owned by the current user."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Entry updated. Re-analysis scheduled if text changed.",
                    content = @Content(schema = @Schema(implementation = JournalEntryResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed (rawText is blank), malformed JSON, or empty body.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated or invalid JWT.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Entry not found or not owned. Message is \"Journal entry not found with ID: <id>\".",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Database or dependent service temporarily unavailable.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<JournalEntryResponse> updateJournalEntry(
            @Parameter(hidden = true) User currentUser,
            @Parameter(description = "Journal entry UUID", required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            UUID id,
            JournalEntryRequest request);

    @Operation(
            summary = "Delete a journal entry",
            description = "Removes the entry and its associated vector embedding. Returns 204 No Content on success. " +
                    "Returns 404 if the entry does not exist or is not owned by the current user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Entry deleted successfully."),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated or invalid JWT.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Entry not found or not owned. Message is \"Journal entry not found with ID: <id>\".",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Database or dependent service temporarily unavailable.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<Void> deleteJournalEntry(
            @Parameter(hidden = true) User currentUser,
            @Parameter(description = "Journal entry UUID", required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            UUID id);

    @Operation(
            summary = "Get a single journal entry by ID",
            description = "Returns one entry with decrypted raw text and full AI analysis. Returns 404 if the entry " +
                    "does not exist or is not owned by the current user."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Entry found.",
                    content = @Content(schema = @Schema(implementation = JournalEntryResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated or invalid JWT.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Entry not found or not owned. Message is \"Journal entry not found or not owned.\".",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Database or dependent service temporarily unavailable.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<JournalEntryResponse> getJournalEntry(
            @Parameter(hidden = true) User currentUser,
            @Parameter(description = "Journal entry UUID", required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            UUID id);

    // ==================================================================
    // History & Analytics
    // ==================================================================

    @Operation(
            summary = "Get journal history (unpaginated)",
            description = "Returns all entries within the given date range. Date format: yyyy-MM-dd. " +
                    "If both dates are omitted, returns everything (1900-01-01 to 2100-12-31). " +
                    "If only startDate is given, end defaults to today. If only endDate is given, start defaults to " +
                    "30 days ago. Returns 400 if start > end or if a date cannot be parsed. " +
                    "Use /history/paginated for large datasets."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Entries returned (possibly empty list).",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = JournalEntryResponse.class)))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid date format or start date is after end date.",
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
    ResponseEntity<List<JournalEntryResponse>> getJournalHistory(
            @Parameter(hidden = true) User currentUser,
            @Parameter(description = "Start date (yyyy-MM-dd). Optional.")
            String startDate,
            @Parameter(description = "End date (yyyy-MM-dd). Optional.")
            String endDate);

    @Operation(
            summary = "Get paginated journal history",
            description = "Returns a page of entries with full page metadata. Defaults to 20 entries per page, " +
                    "sorted by creation timestamp descending. Date format: yyyy-MM-dd. If both dates are omitted, " +
                    "returns everything. Returns 400 if start > end or if a date cannot be parsed."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Page returned.",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid date format or start date is after end date.",
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
    ResponseEntity<PageResponse<JournalEntryResponse>> getJournalHistoryPaginated(
            @Parameter(hidden = true) User currentUser,
            @Parameter(description = "Start date (yyyy-MM-dd). Optional.")
            String startDate,
            @Parameter(description = "End date (yyyy-MM-dd). Optional.")
            String endDate,
            Pageable pageable);

    @Operation(
            summary = "Get mood data for charting",
            description = "Returns a list of {date, moodScore} pairs for entries within the date range. " +
                    "Entries not yet analysed (moodScore null) are skipped. Date format: yyyy-MM-dd. " +
                    "Defaults to the last 30 days when startDate is omitted. Returns 400 if start > end or invalid date."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Mood data returned (possibly empty list).",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = MoodDataResponse.class)))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid date format or start date is after end date.",
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
    ResponseEntity<List<MoodDataResponse>> getMoodData(
            @Parameter(hidden = true) User currentUser,
            @Parameter(description = "Start date (yyyy-MM-dd). Optional. Defaults to 30 days ago.")
            String startDate,
            @Parameter(description = "End date (yyyy-MM-dd). Optional. Defaults to today.")
            String endDate);

    @Operation(
            summary = "Get top key phrases (trends)",
            description = "Aggregates key phrases extracted from the user's entries and returns the top N by frequency " +
                    "as a phrase -> count map, sorted descending by count. Date format: yyyy-MM-dd. " +
                    "Defaults to the last 90 days when startDate is omitted. The limit is capped at 100 by the service."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Trends returned as a phrase -> count map (possibly empty).",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid date format or start date is after end date.",
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
    ResponseEntity<Map<String, Long>> getJournalTrends(
            @Parameter(hidden = true) User currentUser,
            @Parameter(description = "Start date (yyyy-MM-dd). Optional. Defaults to 90 days ago.")
            String startDate,
            @Parameter(description = "End date (yyyy-MM-dd). Optional. Defaults to today.")
            String endDate,
            @Parameter(description = "Maximum number of phrases to return. Capped at 100 by the service.")
            int limit);

    @Operation(
            summary = "Get key phrase frequencies (all-time)",
            description = "Returns the count of each key phrase across all of the user's entries with no date filter. " +
                    "Result is cached in Redis for fast repeated access, so failures of Redis surface as 503."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Frequencies returned as a phrase -> count map (possibly empty).",
                    content = @Content(schema = @Schema(implementation = Map.class))
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
    ResponseEntity<Map<String, Long>> getKeyPhraseFrequencies(
            @Parameter(hidden = true) User currentUser);

    // ==================================================================
    // Search
    // ==================================================================

    @Operation(
            summary = "Search entries by keyword (substring, decrypted)",
            description = "Decrypts each entry server-side and returns those whose raw text contains the keyword, " +
                    "case-insensitive. Suitable for small histories; use semantic search for concept-level queries."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Matching entries returned (possibly empty).",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = JournalEntryResponse.class)))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Missing required 'keyword' request parameter.",
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
    ResponseEntity<List<JournalEntryResponse>> searchJournalEntriesByKeyword(
            @Parameter(hidden = true) User currentUser,
            @Parameter(description = "Keyword to search in decrypted raw text.", required = true)
            String keyword);

    @Operation(
            summary = "Search entries by mood score range",
            description = "Returns entries whose analysed moodScore falls within [minMood, maxMood]. " +
                    "Range is [-1.0, 1.0]. If both parameters are omitted, defaults to the full range. " +
                    "Returns 400 if minMood > maxMood."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Matching entries returned (possibly empty).",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = JournalEntryResponse.class)))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "minMood > maxMood or non-numeric parameter value.",
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
    ResponseEntity<List<JournalEntryResponse>> searchJournalEntriesByMood(
            @Parameter(hidden = true) User currentUser,
            @Parameter(description = "Minimum mood score. Optional. Defaults to -1.0.")
            Double minMood,
            @Parameter(description = "Maximum mood score. Optional. Defaults to 1.0.")
            Double maxMood);

    @Operation(
            summary = "Semantic concept search (vector similarity)",
            description = "Embeds the query concept and returns entries whose vector embedding is closest above the " +
                    "similarity threshold. Finds conceptually related entries even without exact keyword overlap. " +
                    "Requires the vector store to be populated (this happens automatically after AI analysis). " +
                    "Returns 503 if the vector store/Postgres is unreachable."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Conceptually matching entries returned, ranked by similarity (possibly empty).",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = JournalEntryResponse.class)))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Missing required 'concept' request parameter.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated or invalid JWT.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Semantic search is temporarily unavailable. Message: " +
                            "\"Semantic search is temporarily unavailable. Please try again in a moment.\"",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<List<JournalEntryResponse>> searchJournalEntriesSemantically(
            @Parameter(hidden = true) User currentUser,
            @Parameter(description = "Concept for semantic vector search.", required = true)
            String concept);
}