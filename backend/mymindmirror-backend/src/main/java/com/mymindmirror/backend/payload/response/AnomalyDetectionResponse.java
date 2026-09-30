package com.mymindmirror.backend.payload.response;

import java.util.List;

public record AnomalyDetectionResponse(
        List<Anomaly> anomalies,
        String message
) {}