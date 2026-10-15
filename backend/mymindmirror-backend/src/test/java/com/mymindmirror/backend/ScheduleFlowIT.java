package com.mymindmirror.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.mymindmirror.backend.enums.AITask;
import com.mymindmirror.backend.payload.request.CustomTaskRequest;
import com.mymindmirror.backend.payload.response.ScheduleItem;
import com.mymindmirror.backend.payload.response.ScheduleResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Schedule flow — generate, fallback, move, complete")
class ScheduleFlowIT extends AbstractIntegrationTest {

    private String aliceJwt;

    @BeforeEach
    void loginAlice() throws Exception {
        aliceJwt = registerAndLogin("alice", "password123");
    }

    // =================================================================
    // Helpers
    // =================================================================

    /** Create a custom task (guaranteed to be incomplete) so the scheduler can pick it up. */
    private UUID createCustomTask(String title) throws Exception {
        CustomTaskRequest request = new CustomTaskRequest(
                title, null, null, 1.0, "MEDIUM", false);

        MvcResult result = mockMvc.perform(post("/api/custom-tasks")
                        .header("Authorization", "Bearer " + aliceJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return UUID.fromString(objectMapper.readTree(
                result.getResponse().getContentAsString()).get("id").asText());
    }

    /** Stub the AI to return a single ScheduleItem for the given custom task. */
    private void stubAiSchedule(UUID customTaskId, String title, LocalDate date) {
        ScheduleItem item = new ScheduleItem(
                customTaskId.toString(),
                title,
                "WORK_TASK",
                date.toString(),
                "09:00:00",
                "10:00:00"
        );

        ScheduleResponse response = new ScheduleResponse(List.of(item), List.of());

        when(aiClientService.generateStructured(
                anyString(), eq(ScheduleResponse.class), any(), eq(AITask.SCHEDULE_GENERATION)))
                .thenReturn(response);
    }

    /** GET the scheduled tasks within a wide date range; returns the JSON array. */
    private JsonNode fetchScheduledTasks() throws Exception {
        String start = LocalDate.now().minusDays(30).toString();
        String end = LocalDate.now().plusDays(60).toString();

        MvcResult result = mockMvc.perform(get("/api/schedule/tasks")
                        .header("Authorization", "Bearer " + aliceJwt)
                        .param("startDate", start)
                        .param("endDate", end))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private UUID firstScheduledTaskId() throws Exception {
        JsonNode tasks = fetchScheduledTasks();
        assertThat(tasks.size()).isGreaterThan(0);
        return UUID.fromString(tasks.get(0).get("id").asText());
    }

    // =================================================================
    // Generate — AI success
    // =================================================================

    @Test
    @DisplayName("Generate with AI success → scheduled task created and persisted")
    void generateSchedule_aiSuccess_persistsTask() throws Exception {
        UUID customTaskId = createCustomTask("Morning workout");
        stubAiSchedule(customTaskId, "Morning workout", LocalDate.now());

        mockMvc.perform(post("/api/schedule/generate")
                        .header("Authorization", "Bearer " + aliceJwt))
                .andExpect(status().isOk());

        JsonNode tasks = fetchScheduledTasks();
        assertThat(tasks.size()).isEqualTo(1);
        assertThat(tasks.get(0).get("title").asText()).isEqualTo("Morning workout");
        assertThat(tasks.get(0).get("completed").asBoolean()).isFalse();
    }

    // =================================================================
    // Generate — AI failure → fallback scheduler
    // =================================================================

    @Test
    @DisplayName("Generate with AI failure → fallback scheduler runs (endpoint still 200)")
    void generateSchedule_aiFailure_usesFallback() throws Exception {
        createCustomTask("Fallback task");

        when(aiClientService.generateStructured(
                anyString(), eq(ScheduleResponse.class), any(), eq(AITask.SCHEDULE_GENERATION)))
                .thenThrow(new RuntimeException("AI down"));

        mockMvc.perform(post("/api/schedule/generate")
                        .header("Authorization", "Bearer " + aliceJwt))
                .andExpect(status().isOk());

        // Fallback schedules the task into the next available weekday slot
        JsonNode tasks = fetchScheduledTasks();
        assertThat(tasks.size()).isGreaterThan(0);
    }

    // =================================================================
    // Move
    // =================================================================

    @Test
    @DisplayName("Move scheduled task → new date/time persisted")
    void moveScheduledTask_updatesDateAndTime() throws Exception {
        UUID customTaskId = createCustomTask("Movable task");
        stubAiSchedule(customTaskId, "Movable task", LocalDate.now());

        mockMvc.perform(post("/api/schedule/generate")
                        .header("Authorization", "Bearer " + aliceJwt))
                .andExpect(status().isOk());

        UUID taskId = firstScheduledTaskId();

        Map<String, Object> moveRequest = new HashMap<>();
        moveRequest.put("date", LocalDate.now().plusDays(1).toString());
        moveRequest.put("startTime", "14:00:00");
        moveRequest.put("endTime", "15:00:00");

        mockMvc.perform(put("/api/schedule/task/{id}/move", taskId)
                        .header("Authorization", "Bearer " + aliceJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(moveRequest)))
                .andExpect(status().isOk());

        // Verify by re-fetching
        JsonNode tasks = fetchScheduledTasks();
        JsonNode moved = tasks.get(0);
        assertThat(moved.get("scheduledDate").asText())
                .isEqualTo(LocalDate.now().plusDays(1).toString());
    }

    // =================================================================
    // Complete
    // =================================================================

    @Test
    @DisplayName("Complete scheduled task → completed flag flips to true")
    void completeScheduledTask_setsCompletedFlag() throws Exception {
        UUID customTaskId = createCustomTask("Completable task");
        stubAiSchedule(customTaskId, "Completable task", LocalDate.now());

        mockMvc.perform(post("/api/schedule/generate")
                        .header("Authorization", "Bearer " + aliceJwt))
                .andExpect(status().isOk());

        UUID taskId = firstScheduledTaskId();

        mockMvc.perform(patch("/api/schedule/task/{id}/complete", taskId)
                        .header("Authorization", "Bearer " + aliceJwt))
                .andExpect(status().isOk());

        JsonNode tasks = fetchScheduledTasks();
        assertThat(tasks.get(0).get("completed").asBoolean()).isTrue();
    }

    // =================================================================
    // Manual scheduling (bypass AI)
    // =================================================================

    @Test
    @DisplayName("Manually schedule custom task → scheduled task created directly")
    void scheduleCustomTaskManually_createsScheduledTask() throws Exception {
        UUID customTaskId = createCustomTask("Manually scheduled");

        Map<String, Object> request = new HashMap<>();
        request.put("customTaskId", customTaskId.toString());
        request.put("title", "Manually scheduled");
        request.put("date", LocalDate.now().toString());
        request.put("startTime", "10:00:00");
        request.put("endTime", "11:00:00");
        request.put("priority", "HIGH");

        mockMvc.perform(post("/api/schedule/task/custom")
                        .header("Authorization", "Bearer " + aliceJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        JsonNode tasks = fetchScheduledTasks();
        assertThat(tasks.size()).isEqualTo(1);
        assertThat(tasks.get(0).get("title").asText()).isEqualTo("Manually scheduled");
        assertThat(tasks.get(0).get("priority").asText()).isEqualTo("HIGH");
    }

    // =================================================================
    // Re-generate preserves completed tasks
    // =================================================================

    @Test
    @DisplayName("Re-generate preserves completed tasks, deletes incomplete ones")
    void generateSchedule_twice_preservesCompletedDeletesIncomplete() throws Exception {
        // ---- First generate ----
        UUID customTaskId1 = createCustomTask("First task");
        stubAiSchedule(customTaskId1, "First task", LocalDate.now());

        mockMvc.perform(post("/api/schedule/generate")
                        .header("Authorization", "Bearer " + aliceJwt))
                .andExpect(status().isOk());

        // Complete the first scheduled task
        UUID firstScheduledId = firstScheduledTaskId();
        mockMvc.perform(patch("/api/schedule/task/{id}/complete", firstScheduledId)
                        .header("Authorization", "Bearer " + aliceJwt))
                .andExpect(status().isOk());

        // ---- Second generate with a different task ----
        UUID customTaskId2 = createCustomTask("Second task");
        stubAiSchedule(customTaskId2, "Second task", LocalDate.now().plusDays(1));

        mockMvc.perform(post("/api/schedule/generate")
                        .header("Authorization", "Bearer " + aliceJwt))
                .andExpect(status().isOk());

        // ---- Verify: 2 tasks total (1 completed, 1 new) ----
        JsonNode tasks = fetchScheduledTasks();
        assertThat(tasks.size()).isEqualTo(2);

        long completed = 0;
        long incomplete = 0;
        for (JsonNode task : tasks) {
            if (task.get("completed").asBoolean()) {
                completed++;
            } else {
                incomplete++;
            }
        }
        assertThat(completed).as("completed task preserved").isEqualTo(1);
        assertThat(incomplete).as("new task added").isEqualTo(1);
    }
}