package com.mymindmirror.backend.controller;

import com.mymindmirror.backend.annotation.CurrentUser;
import com.mymindmirror.backend.api.AnalyticsApi;
import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.payload.request.AnomalyDetectionRequest;
import com.mymindmirror.backend.payload.response.AnomalyDetectionResponse;
import com.mymindmirror.backend.payload.response.DailyAggregatedDataResponse;
import com.mymindmirror.backend.service.JournalService;
import com.mymindmirror.backend.util.DateRange;
import com.mymindmirror.backend.util.DateUtil;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ml")
@RequiredArgsConstructor
@Slf4j
public class AnalyticsController implements AnalyticsApi {

    private final JournalService journalService;

    @Override
    @GetMapping("/daily-aggregated-data")
    public ResponseEntity<List<DailyAggregatedDataResponse>> getDailyAggregatedData(
            @CurrentUser User currentUser,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        log.info("Received request for daily aggregated data for user: {}", currentUser.getUsername());

        DateRange range = DateUtil.parseDateRange(startDate, endDate, 30);
        List<DailyAggregatedDataResponse> data = journalService.getDailyAggregatedDataForUser(
                currentUser, range.start(), range.end()
        );

        log.info("Found {} daily aggregated data points for user {}.", data.size(), currentUser.getUsername());
        return ResponseEntity.ok(data);
    }

    @Override
    @PostMapping("/anomaly-detection")
    public ResponseEntity<AnomalyDetectionResponse> runAnomalyDetection(
            @CurrentUser User currentUser,
            @Valid @RequestBody AnomalyDetectionRequest request) {

        log.info("Received request to run anomaly detection for user: {}", currentUser.getUsername());

        AnomalyDetectionResponse response =
                journalService.runAnomalyDetection(request.data());

        log.info("Anomaly detection completed for user {}, found {} anomalies.",
                currentUser.getUsername(), response.anomalies().size());
        return ResponseEntity.ok(response);
    }
}