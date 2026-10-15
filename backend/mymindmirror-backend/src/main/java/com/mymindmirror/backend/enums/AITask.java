package com.mymindmirror.backend.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import java.util.List;

@Getter
@RequiredArgsConstructor
public enum AITask {
    // ------------------------------------------------------------------------
    // PLAIN TEXT TASKS (generate) - Utilizing Gemma & Flash
    // ------------------------------------------------------------------------
    TODAY_REFLECTION(List.of("gemma-4-31b-it", "gemma-4-26b-a4b-it", "gemini-3.6-flash", "gemini-3.5-flash")),
    REFLECTIVE_QUESTION(List.of("gemini-3.1-flash-lite", "gemma-4-26b-a4b-it", "gemini-3.6-flash", "gemini-3.5-flash")),
    REFLECTION_CHAT(List.of("gemini-3.5-flash", "gemini-3.6-flash", "gemini-3.1-pro", "gemini-2.5-pro")),

    // ------------------------------------------------------------------------
    // STRUCTURED JSON TASKS (generateStructured) - GEMINI ONLY
    // We cascade from high-quota/Lite models to high-reasoning/Flash models.
    // If a Lite model hallucinates bad JSON, it instantly falls over to a smarter Flash model!
    // ------------------------------------------------------------------------
    JOURNAL_ANALYSIS(List.of("gemini-3.5-flash-lite", "gemini-3.1-flash-lite", "gemini-3.6-flash", "gemini-3.5-flash", "gemini-2.5-flash", "gemini-2.5-flash-lite")),
    ROADMAP_INITIAL(List.of("gemini-3.5-flash-lite", "gemini-3.1-flash-lite", "gemini-3.6-flash", "gemini-3.5-flash", "gemini-2.5-flash")),
    ROADMAP_EXTENSION(List.of("gemini-3.5-flash-lite", "gemini-3.1-flash-lite", "gemini-3.6-flash", "gemini-3.5-flash", "gemini-2.5-flash")),
    ROADMAP_NEXT_STEPS(List.of("gemini-3.5-flash-lite", "gemini-3.1-flash-lite", "gemini-3.6-flash", "gemini-3.5-flash", "gemini-2.5-flash")),
    ROADMAP_ELABORATION(List.of("gemini-3.5-flash-lite", "gemini-3.1-flash-lite", "gemini-3.6-flash", "gemini-3.5-flash", "gemini-2.5-flash")),
    ROADMAP_RESCHEDULE(List.of("gemini-3.5-flash-lite", "gemini-3.1-flash-lite", "gemini-3.6-flash", "gemini-3.5-flash", "gemini-2.5-flash")),
    MILESTONE_INSIGHTS(List.of("gemini-3.5-flash-lite", "gemini-3.1-flash-lite", "gemini-3.6-flash", "gemini-3.5-flash", "gemini-2.5-flash")),
    PARSE_GROWTH_TIP(List.of("gemini-3.5-flash-lite", "gemini-3.1-flash-lite", "gemini-3.6-flash", "gemini-3.5-flash", "gemini-2.5-flash")),
    SCHEDULE_GENERATION(List.of("gemini-3.5-flash-lite", "gemini-3.1-flash-lite", "gemini-3.6-flash", "gemini-3.5-flash", "gemini-2.5-flash")),
    SCHEDULE_REOPTIMIZATION(List.of("gemini-3.5-flash-lite", "gemini-3.1-flash-lite", "gemini-3.6-flash", "gemini-3.5-flash", "gemini-2.5-flash"));

    private final List<String> modelCascade;
}