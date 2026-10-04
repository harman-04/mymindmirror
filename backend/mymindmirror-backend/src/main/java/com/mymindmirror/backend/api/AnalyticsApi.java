package com.mymindmirror.backend.api;

import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.payload.request.AnomalyDetectionRequest;
import com.mymindmirror.backend.payload.response.AnomalyDetectionResponse;
import com.mymindmirror.backend.payload.response.DailyAggregatedDataResponse;
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

/**
 * OpenAPI contract for {@link com.mymindmirror.backend.controller.AnalyticsController}.
 *
 * <p>Contains only documentation annotations. All Spring MVC binding
 * ({@code @GetMapping}, {@code @PostMapping}, {@code @RequestParam}, {@code @RequestBody},
 * {@code @Valid}, {@code @CurrentUser}) lives on the controller.
 */
@Tag(
        name = "Analytics",
        description = "Per-user analytics derived from journal activity. Provides daily aggregated data " +
                "(average mood and total words per day) for the given window, plus in-memory anomaly " +
                "detection using an EWMA-based z-score model. Anomaly detection does not persist or read " +
                "from the database — it operates purely on the supplied payload."
)
public interface AnalyticsApi {

    @Operation(
            summary = "Get daily aggregated data for the user",
            description = "Returns one entry per day in the requested window, each with the day's average " +
                    "mood score and total words written. Dates are in yyyy-MM-dd format. Defaults to the " +
                    "last 30 days when startDate is omitted; endDate defaults to today. Returns 400 if a " +
                    "date is malformed or if startDate is after endDate."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Daily aggregates returned (possibly empty list). Each element contains " +
                            "date, averageMood, and totalWords.",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = DailyAggregatedDataResponse.class)))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid date format or startDate is after endDate. Message: " +
                            "\"Invalid date format: '<value>'. Use yyyy-MM-dd.\" or " +
                            "\"Start date (<start>) cannot be after end date (<end>).\".",
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
    ResponseEntity<List<DailyAggregatedDataResponse>> getDailyAggregatedData(
            @Parameter(hidden = true) User currentUser,
            @Parameter(description = "Start date in yyyy-MM-dd format. Optional. Defaults to 30 days ago.")
            String startDate,
            @Parameter(description = "End date in yyyy-MM-dd format. Optional. Defaults to today.")
            String endDate);

    @Operation(
            summary = "Run anomaly detection on the supplied daily data",
            description = "Executes an EWMA-based z-score anomaly detector on the payload's daily aggregated " +
                    "data. Returns a list of detected anomalies plus a human-readable summary message. " +
                    "The detection is purely in-memory: no database or cache access occurs. The minimum " +
                    "number of days required for detection, the EWMA span, and the mood/word thresholds " +
                    "are configurable (defaults: 5 days, span 7, thresholds 0.6 and 0.8)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Detection completed. Response body contains the list of detected anomalies " +
                            "(possibly empty) and a summary message. Insufficient data and empty inputs also " +
                            "return 200 with an explanatory message.",
                    content = @Content(schema = @Schema(implementation = AnomalyDetectionResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed — 'data' is missing, null, or empty; malformed JSON; " +
                            "empty body; or literal null body.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated or invalid JWT.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected runtime error inside the detector.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Authentication resolution could not reach the database. Detection itself " +
                            "does not touch the database, so once the request reaches the controller this " +
                            "status is not produced.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<AnomalyDetectionResponse> runAnomalyDetection(
            @Parameter(hidden = true) User currentUser,
            AnomalyDetectionRequest request);
}