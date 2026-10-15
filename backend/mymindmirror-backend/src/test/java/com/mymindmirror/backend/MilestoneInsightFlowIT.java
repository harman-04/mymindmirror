package com.mymindmirror.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.mymindmirror.backend.enums.AITask;
import com.mymindmirror.backend.enums.InsightStatus;
import com.mymindmirror.backend.payload.request.MilestoneRequest;
import com.mymindmirror.backend.payload.response.MilestoneInsightResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Milestone insights — AI generation, fallback, ownership")
class MilestoneInsightFlowIT extends AbstractIntegrationTest {

    private String aliceJwt;
    private String bobJwt;

    @BeforeEach
    void setUpUsers() throws Exception {
        aliceJwt = registerAndLogin("alice", "password123");
        bobJwt = registerAndLogin("bob", "password123");
    }

    // =================================================================
    // Helpers
    // =================================================================

    private UUID aliceCreatesMilestone() throws Exception {
        MilestoneRequest request = new MilestoneRequest(
                "Learn Spring Boot",
                "Deep dive into Spring Boot internals",
                null,
                null);

        MvcResult result = mockMvc.perform(post("/api/milestones")
                        .header("Authorization", "Bearer " + aliceJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return UUID.fromString(objectMapper.readTree(
                result.getResponse().getContentAsString()).get("id").asText());
    }

    /** A representative valid insight response — used to stub the AI on happy paths. */
    private MilestoneInsightResponse validInsights() {
        return new MilestoneInsightResponse(
                "You have 3 tasks remaining this week.",
                "You are progressing steadily.",
                List.of("Focus on daily consistency", "Take breaks"),
                "You're doing great — keep going!",
                List.of("Read one chapter daily"),
                InsightStatus.SUCCESS
        );
    }

    // =================================================================
    // Happy path — AI returns a valid response
    // =================================================================

    @Test
    @DisplayName("GET insights with AI success → 200 with populated insight fields")
    void getInsights_aiSuccess_returns200WithContent() throws Exception {
        UUID milestoneId = aliceCreatesMilestone();

        when(aiClientService.generateStructured(
                anyString(), eq(MilestoneInsightResponse.class), any(), eq(AITask.MILESTONE_INSIGHTS)))
                .thenReturn(validInsights());

        mockMvc.perform(get("/api/milestones/{id}/insights", milestoneId)
                        .header("Authorization", "Bearer " + aliceJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingWork").value("You have 3 tasks remaining this week."))
                .andExpect(jsonPath("$.performanceAssessment").value("You are progressing steadily."))
                .andExpect(jsonPath("$.tips.length()").value(2))
                .andExpect(jsonPath("$.tips[0]").value("Focus on daily consistency"))
                .andExpect(jsonPath("$.encouragement").value("You're doing great — keep going!"))
                .andExpect(jsonPath("$.suggestedNewTasks.length()").value(1))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    // =================================================================
    // Fallback — AI fails, service swallows and returns fallback
    // =================================================================

    @Test
    @DisplayName("GET insights with AI failure → 200 with fallback content (no 500)")
    void getInsights_aiFailure_returnsFallback() throws Exception {
        UUID milestoneId = aliceCreatesMilestone();

        when(aiClientService.generateStructured(
                anyString(), eq(MilestoneInsightResponse.class), any(), eq(AITask.MILESTONE_INSIGHTS)))
                .thenThrow(new RuntimeException("AI down"));

        // MilestoneInsightService catches the exception and returns a fallback —
        // controller never sees the throw, so the response is 200 (not 500).
        MvcResult result = mockMvc.perform(get("/api/milestones/{id}/insights", milestoneId)
                        .header("Authorization", "Bearer " + aliceJwt))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());

        // Fallback must still carry the required fields
        assertThat(body.has("remainingWork")).isTrue();
        assertThat(body.has("performanceAssessment")).isTrue();
        assertThat(body.has("tips")).isTrue();
        assertThat(body.has("encouragement")).isTrue();
        assertThat(body.has("suggestedNewTasks")).isTrue();

        // Fallback content is non-null even if the AI never ran
        assertThat(body.get("encouragement").asText()).isNotBlank();
    }

    // =================================================================
    // Ownership — Bob cannot read Alice's milestone insights
    // =================================================================

    @Test
    @DisplayName("Bob GET Alice's insights → 404")
    void getInsights_notOwned_returns404() throws Exception {
        UUID aliceMilestoneId = aliceCreatesMilestone();

        mockMvc.perform(get("/api/milestones/{id}/insights", aliceMilestoneId)
                        .header("Authorization", "Bearer " + bobJwt))
                .andExpect(status().isNotFound());
    }

    // =================================================================
    // Missing milestone — 404
    // =================================================================

    @Test
    @DisplayName("GET insights on non-existent milestone → 404")
    void getInsights_missingMilestone_returns404() throws Exception {
        mockMvc.perform(get("/api/milestones/{id}/insights", UUID.randomUUID())
                        .header("Authorization", "Bearer " + aliceJwt))
                .andExpect(status().isNotFound());
    }

    // =================================================================
    // AI called exactly once per request (no caching yet)
    // =================================================================

    @Test
    @DisplayName("Two consecutive GETs each call the AI (no cache on insights)")
    void getInsights_calledTwice_invokesAiTwice() throws Exception {
        UUID milestoneId = aliceCreatesMilestone();

        when(aiClientService.generateStructured(
                anyString(), eq(MilestoneInsightResponse.class), any(), eq(AITask.MILESTONE_INSIGHTS)))
                .thenReturn(validInsights());

        mockMvc.perform(get("/api/milestones/{id}/insights", milestoneId)
                        .header("Authorization", "Bearer " + aliceJwt))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/milestones/{id}/insights", milestoneId)
                        .header("Authorization", "Bearer " + aliceJwt))
                .andExpect(status().isOk());

        // Verify AI was called twice — no caching is expected on this endpoint today
        org.mockito.Mockito.verify(aiClientService, org.mockito.Mockito.times(2))
                .generateStructured(anyString(),
                        eq(MilestoneInsightResponse.class), any(), eq(AITask.MILESTONE_INSIGHTS));
    }
}