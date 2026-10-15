package com.mymindmirror.backend.payload.response;
import java.util.List;
import java.util.Map;

public record JournalAnalysisResponse(
        Map<String, Double> emotions,
        List<String> coreConcerns,
        String summary,
        List<String> growthTips,
        List<String> keyPhrases
) {}