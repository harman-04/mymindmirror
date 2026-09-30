package com.mymindmirror.backend.payload.request;

import com.mymindmirror.backend.payload.response.DailyAggregatedDataResponse;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AnomalyDetectionRequest(
        @NotEmpty(message = "At least one data point is required for anomaly detection.")
        List<DailyAggregatedDataResponse> data
) {}