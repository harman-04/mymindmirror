package com.mymindmirror.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.mymindmirror.backend.enums.AITask;
import com.mymindmirror.backend.payload.request.ImportTaskRequest;
import com.mymindmirror.backend.payload.request.RoadmapGenerateRequest;
import com.mymindmirror.backend.payload.response.ElaborationResponse;
import com.mymindmirror.backend.payload.response.RescheduleResponse;
import com.mymindmirror.backend.payload.response.RoadmapContinuationResponse;
import com.mymindmirror.backend.payload.response.RoadmapGenerateResponse;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Roadmap flow — generate, continue, elaborate, reschedule, delete")
class RoadmapFlowIT extends AbstractIntegrationTest {

    private String aliceJwt;

    @BeforeEach
    void loginAlice() throws Exception {
        aliceJwt = registerAndLogin("alice", "password123");
    }

    // =================================================================
    // Helpers
    // =================================================================

    /** Stub a valid RoadmapGenerateResponse for happy-path tests. */
    private void stubInitialGeneration() {
        RoadmapGenerateResponse.Task task1 = new RoadmapGenerateResponse.Task(
                1, 1, "Read docs", "daily", "Details", List.of("sub1"));
        RoadmapGenerateResponse.Task task2 = new RoadmapGenerateResponse.Task(
                2, 1, "Practice coding", "daily", "Details", List.of("sub2"));

        RoadmapGenerateResponse response = new RoadmapGenerateResponse(
                "Learn Spring Boot",
                4,
                List.of(task1, task2),   // ← 2 tasks now
                List.of(),
                List.of(),
                false
        );

        when(aiClientService.generateStructured(
                anyString(), eq(RoadmapGenerateResponse.class), any(), eq(AITask.ROADMAP_INITIAL)))
                .thenReturn(response);
    }

    /** Call POST /generate and return the new roadmap's UUID. */
    private UUID generateRoadmap(String goal) throws Exception {
        RoadmapGenerateRequest request = new RoadmapGenerateRequest(
                goal, null, 4, "WEEKS");

        MvcResult result = mockMvc.perform(post("/api/roadmap/generate")
                        .header("Authorization", "Bearer " + aliceJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        return UUID.fromString(objectMapper.readTree(
                result.getResponse().getContentAsString()).get("id").asText());
    }

    private JsonNode getFirstRoadmap() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/roadmap")
                        .header("Authorization", "Bearer " + aliceJwt))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode roadmaps = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(roadmaps.size()).isGreaterThan(0);
        return roadmaps.get(0);
    }

    /** Extract first task UUID from a roadmap JSON node. */
    private UUID firstTaskId(JsonNode roadmap) {
        JsonNode tasks = roadmap.get("tasks");
        assertThat(tasks.size()).isGreaterThan(0);
        return UUID.fromString(tasks.get(0).get("id").asText());
    }

    // =================================================================
    // Generate
    // =================================================================

    @Test
    @DisplayName("Generate with AI success → 200 + roadmap persisted with tasks")
    void generateRoadmap_aiSuccess_persistsRoadmap() throws Exception {
        stubInitialGeneration();

        UUID id = generateRoadmap("Learn Spring Boot");

        mockMvc.perform(get("/api/roadmap")
                        .header("Authorization", "Bearer " + aliceJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id.toString()))
                .andExpect(jsonPath("$[0].title").value("Learn Spring Boot"))
                .andExpect(jsonPath("$[0].tasks.length()").value(2));
    }

    @Test
    @DisplayName("Generate with AI failure → fallback roadmap (endpoint still 200)")
    void generateRoadmap_aiFailure_usesFallback() throws Exception {
        when(aiClientService.generateStructured(
                anyString(), eq(RoadmapGenerateResponse.class), any(), eq(AITask.ROADMAP_INITIAL)))
                .thenThrow(new RuntimeException("AI down"));

        UUID id = generateRoadmap("Learn Kotlin");

        mockMvc.perform(get("/api/roadmap")
                        .header("Authorization", "Bearer " + aliceJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value(
                        org.hamcrest.Matchers.containsString("Personalized Roadmap")));
        // Sanity: the roadmap exists
        assertThat(id).isNotNull();
    }

    // =================================================================
    // Delete
    // =================================================================

    @Test
    @DisplayName("Delete → 200 + removed from list")
    void deleteRoadmap_removesFromDB() throws Exception {
        stubInitialGeneration();
        UUID id = generateRoadmap("Delete me");

        mockMvc.perform(delete("/api/roadmap/{id}", id)
                        .header("Authorization", "Bearer " + aliceJwt))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/roadmap")
                        .header("Authorization", "Bearer " + aliceJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Delete random UUID → 404")
    void deleteRoadmap_randomId_returns404() throws Exception {
        mockMvc.perform(delete("/api/roadmap/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + aliceJwt))
                .andExpect(status().isNotFound());
    }

    // =================================================================
    // Continue
    // =================================================================

    @Test
    @DisplayName("Continue with AI success → new task added to roadmap")
    void continueRoadmap_addsTask() throws Exception {
        stubInitialGeneration();
        UUID id = generateRoadmap("Learn Go");

        // Mark the initial task complete so continue is allowed
        // Mark one task complete so continue is allowed
        UUID initialTaskId = firstTaskId(getFirstRoadmap());
        mockMvc.perform(patch("/api/roadmap/task/{id}/toggle", initialTaskId)
                        .header("Authorization", "Bearer " + aliceJwt))
                .andExpect(status().isOk());

        // Stub continuation AI
        RoadmapContinuationResponse continuation = new RoadmapContinuationResponse(List.of(
                new RoadmapGenerateResponse.Task(1, 2, "New Task", "daily", "Details", List.of())
        ));
        when(aiClientService.generateStructured(
                anyString(), eq(RoadmapContinuationResponse.class), any(), eq(AITask.ROADMAP_NEXT_STEPS)))
                .thenReturn(continuation);

        mockMvc.perform(post("/api/roadmap/{id}/continue", id)
                        .header("Authorization", "Bearer " + aliceJwt))
                .andExpect(status().isOk());

        // Roadmap now has 2 tasks
        mockMvc.perform(get("/api/roadmap")
                        .header("Authorization", "Bearer " + aliceJwt))
                .andExpect(jsonPath("$[0].tasks.length()").value(3));   // 2 initial + 1 continuation
    }

    @Test
    @DisplayName("Continue with AI failure → 503 (no fallback for continuation)")
    void continueRoadmap_aiFailure_returns503() throws Exception {
        stubInitialGeneration();
        UUID id = generateRoadmap("Learn Rust");

        UUID initialTaskId = firstTaskId(getFirstRoadmap());
        mockMvc.perform(patch("/api/roadmap/task/{id}/toggle", initialTaskId)
                        .header("Authorization", "Bearer " + aliceJwt))
                .andExpect(status().isOk());

        when(aiClientService.generateStructured(
                anyString(), eq(RoadmapContinuationResponse.class), any(), eq(AITask.ROADMAP_NEXT_STEPS)))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                        "shared AI service at capacity"));

        mockMvc.perform(post("/api/roadmap/{id}/continue", id)
                        .header("Authorization", "Bearer " + aliceJwt))
                .andExpect(status().isServiceUnavailable());
    }

    // =================================================================
    // Elaborate
    // =================================================================

    @Test
    @DisplayName("Elaborate task with AI success → details updated")
    void elaborateTask_updatesDetails() throws Exception {
        stubInitialGeneration();
        generateRoadmap("Elaborate me");
        UUID taskId = firstTaskId(getFirstRoadmap());

        ElaborationResponse aiResponse = new ElaborationResponse(
                "Deep elaboration text", List.of("step1", "step2"), 2.5);

        when(aiClientService.generateStructured(
                anyString(), eq(ElaborationResponse.class), any(), eq(AITask.ROADMAP_ELABORATION)))
                .thenReturn(aiResponse);

        mockMvc.perform(post("/api/roadmap/task/{id}/elaborate", taskId)
                        .header("Authorization", "Bearer " + aliceJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.details").value("Deep elaboration text"))
                .andExpect(jsonPath("$.subtasks.length()").value(2));
    }

    // =================================================================
    // Toggle task completion
    // =================================================================

    @Test
    @DisplayName("Toggle task → completed flag flips")
    void toggleTaskCompletion_flipsFlag() throws Exception {
        stubInitialGeneration();
        generateRoadmap("Toggle task");
        UUID taskId = firstTaskId(getFirstRoadmap());

        // Initially not completed
        mockMvc.perform(get("/api/roadmap")
                        .header("Authorization", "Bearer " + aliceJwt))
                .andExpect(jsonPath("$[0].tasks[0].completed").value(false));

        mockMvc.perform(patch("/api/roadmap/task/{id}/toggle", taskId)
                        .header("Authorization", "Bearer " + aliceJwt))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/roadmap")
                        .header("Authorization", "Bearer " + aliceJwt))
                .andExpect(jsonPath("$[0].tasks[0].completed").value(true));
    }

    // =================================================================
    // Import task to milestone
    // =================================================================

    @Test
    @DisplayName("Import roadmap task to milestones → milestone created with copied task")
    void importTaskToMilestone_createsMilestoneTask() throws Exception {
        stubInitialGeneration();
        generateRoadmap("Import test");
        UUID roadmapId = UUID.fromString(getFirstRoadmap().get("id").asText());
        UUID taskId = firstTaskId(getFirstRoadmap());

        mockMvc.perform(post("/api/roadmap/import-task")
                        .header("Authorization", "Bearer " + aliceJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ImportTaskRequest(roadmapId, taskId))))
                .andExpect(status().isOk());

        // A milestone named "Roadmap: <title>" should now exist
        mockMvc.perform(get("/api/milestones")
                        .header("Authorization", "Bearer " + aliceJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value(
                        org.hamcrest.Matchers.containsString("Learn Spring Boot")));
    }

    // =================================================================
    // Reschedule
    // =================================================================

    @Test
    @DisplayName("Reschedule with AI success → endpoint returns 200")
    void rescheduleRoadmap_aiSuccess_returns200() throws Exception {
        stubInitialGeneration();
        UUID id = generateRoadmap("Reschedule me");

        // Grab both task IDs
        JsonNode roadmap = getFirstRoadmap();
        UUID firstTaskId = UUID.fromString(roadmap.get("tasks").get(0).get("id").asText());
        UUID secondTaskId = UUID.fromString(roadmap.get("tasks").get(1).get("id").asText());

        // Complete task 1 — leaves task 2 incomplete for rescheduling
        mockMvc.perform(patch("/api/roadmap/task/{id}/toggle", firstTaskId)
                        .header("Authorization", "Bearer " + aliceJwt))
                .andExpect(status().isOk());

        // Reschedule task 2 (the incomplete one)
        RescheduleResponse aiResponse = new RescheduleResponse(
                4,
                List.of(new RescheduleResponse.TaskUpdate(secondTaskId.toString(), 2))
        );

        when(aiClientService.generateStructured(
                anyString(), eq(RescheduleResponse.class), any(), eq(AITask.ROADMAP_RESCHEDULE)))
                .thenReturn(aiResponse);

        mockMvc.perform(post("/api/roadmap/{id}/reschedule", id)
                        .header("Authorization", "Bearer " + aliceJwt))
                .andExpect(status().isOk());
    }
}