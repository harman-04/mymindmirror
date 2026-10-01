package com.mymindmirror.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.mymindmirror.backend.payload.request.CustomTaskRequest;
import com.mymindmirror.backend.payload.request.JournalEntryRequest;
import com.mymindmirror.backend.payload.request.MilestoneRequest;
import com.mymindmirror.backend.payload.request.TaskRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Multi-tenant isolation — cross-user access denied")
class MultiTenantIsolationIT extends AbstractIntegrationTest {

    private String aliceJwt;
    private String bobJwt;

    @BeforeEach
    void createBothUsers() throws Exception {
        aliceJwt = registerAndLogin("alice", "password123");
        bobJwt = registerAndLogin("bob", "password123");
    }

    // =================================================================
    // Helpers
    // =================================================================

    private UUID aliceCreatesJournal() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/journal")
                        .header("Authorization", "Bearer " + aliceJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new JournalEntryRequest("Alice's private thoughts"))))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(readJson(result).get("id").asText());
    }

    private UUID aliceCreatesMilestone() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/milestones")
                        .header("Authorization", "Bearer " + aliceJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new MilestoneRequest("Alice's goal", "private", null, null))))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(readJson(result).get("id").asText());
    }

    private UUID aliceCreatesTask(UUID milestoneId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/milestones/{mid}/tasks", milestoneId)
                        .header("Authorization", "Bearer " + aliceJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TaskRequest("Alice's task", null, null, null, null))))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(readJson(result).get("id").asText());
    }

    private UUID aliceCreatesCustomTask() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/custom-tasks")
                        .header("Authorization", "Bearer " + aliceJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CustomTaskRequest("Alice's private task", null, null, 1.0, "MEDIUM", false))))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(readJson(result).get("id").asText());
    }

    private UUID aliceSchedulesCustomTask(UUID customTaskId) throws Exception {
        LocalDate today = LocalDate.now();

        // 1. Schedule the custom task
        Map<String, Object> scheduleReq = new HashMap<>();
        scheduleReq.put("customTaskId", customTaskId.toString());
        scheduleReq.put("title", "Alice's scheduled item");
        scheduleReq.put("date", today.toString());
        scheduleReq.put("startTime", "09:00:00");
        scheduleReq.put("endTime", "10:00:00");
        scheduleReq.put("priority", "MEDIUM");

        mockMvc.perform(post("/api/schedule/task/custom")
                        .header("Authorization", "Bearer " + aliceJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(scheduleReq)))
                .andExpect(status().isOk());

        // 2. Fetch the scheduled tasks to find its ID
        MvcResult result = mockMvc.perform(get("/api/schedule/tasks")
                        .header("Authorization", "Bearer " + aliceJwt)
                        .param("startDate", today.toString())
                        .param("endDate", today.toString()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode tasks = readJson(result);
        return UUID.fromString(tasks.get(0).get("id").asText());
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    // =================================================================
    // Journal isolation — policy: 404 (not found or not owned)
    // =================================================================

    @Test
    @DisplayName("Bob cannot GET Alice's journal by ID → 404")
    void bob_getAliceJournal_returns404() throws Exception {
        UUID aliceEntryId = aliceCreatesJournal();

        mockMvc.perform(get("/api/journal/{id}", aliceEntryId)
                        .header("Authorization", "Bearer " + bobJwt))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Bob cannot UPDATE Alice's journal → 404")
    void bob_updateAliceJournal_returns404() throws Exception {
        UUID aliceEntryId = aliceCreatesJournal();

        mockMvc.perform(put("/api/journal/{id}", aliceEntryId)
                        .header("Authorization", "Bearer " + bobJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new JournalEntryRequest("Bob's tampering"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value(org.hamcrest.Matchers.containsString("Journal entry not found")));
    }


    @Test
    @DisplayName("Bob cannot DELETE Alice's journal → 404")
    void bob_deleteAliceJournal_returns404() throws Exception {
        UUID aliceEntryId = aliceCreatesJournal();

        mockMvc.perform(delete("/api/journal/{id}", aliceEntryId)
                        .header("Authorization", "Bearer " + bobJwt))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value(org.hamcrest.Matchers.containsString("Journal entry not found")));
    }


    @Test
    @DisplayName("Bob's history does NOT include Alice's entries")
    void bob_history_excludesAliceEntries() throws Exception {
        aliceCreatesJournal();

        mockMvc.perform(get("/api/journal/history")
                        .header("Authorization", "Bearer " + bobJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // =================================================================
    // Milestone isolation — policy: 404
    // =================================================================

    @Test
    @DisplayName("Bob cannot GET Alice's milestone by ID → 404")
    void bob_getAliceMilestone_returns404() throws Exception {
        UUID aliceMilestoneId = aliceCreatesMilestone();

        mockMvc.perform(get("/api/milestones/{id}", aliceMilestoneId)
                        .header("Authorization", "Bearer " + bobJwt))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Bob cannot UPDATE Alice's milestone → 404")
    void bob_updateAliceMilestone_returns404() throws Exception {
        UUID aliceMilestoneId = aliceCreatesMilestone();

        mockMvc.perform(put("/api/milestones/{id}", aliceMilestoneId)
                        .header("Authorization", "Bearer " + bobJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new MilestoneRequest("Bob's tampering", null, null, null))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Bob cannot DELETE Alice's milestone → 404")
    void bob_deleteAliceMilestone_returns404() throws Exception {
        UUID aliceMilestoneId = aliceCreatesMilestone();

        mockMvc.perform(delete("/api/milestones/{id}", aliceMilestoneId)
                        .header("Authorization", "Bearer " + bobJwt))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Bob's milestone list does NOT include Alice's")
    void bob_milestoneList_excludesAliceMilestones() throws Exception {
        aliceCreatesMilestone();

        mockMvc.perform(get("/api/milestones")
                        .header("Authorization", "Bearer " + bobJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // =================================================================
    // Task isolation — policy: 404 (nested under milestone ownership)
    // =================================================================

    @Test
    @DisplayName("Bob cannot CREATE a task on Alice's milestone → 404")
    void bob_createTaskOnAliceMilestone_returns404() throws Exception {
        UUID aliceMilestoneId = aliceCreatesMilestone();

        mockMvc.perform(post("/api/milestones/{mid}/tasks", aliceMilestoneId)
                        .header("Authorization", "Bearer " + bobJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TaskRequest("Bob's task", null, null, null, null))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Bob cannot GET Alice's task → 404")
    void bob_getAliceTask_returns404() throws Exception {
        UUID aliceMilestoneId = aliceCreatesMilestone();
        UUID aliceTaskId = aliceCreatesTask(aliceMilestoneId);

        mockMvc.perform(get("/api/milestones/{mid}/tasks/{tid}",
                        aliceMilestoneId, aliceTaskId)
                        .header("Authorization", "Bearer " + bobJwt))
                .andExpect(status().isNotFound());
    }

    // =================================================================
    // Custom Task isolation — policy: 404 (not found or not owned)
    // =================================================================

    @Test
    @DisplayName("Bob cannot DELETE Alice's custom task → 404")
    void bob_deleteAliceCustomTask_returns404() throws Exception {
        UUID aliceCustomTaskId = aliceCreatesCustomTask();

        mockMvc.perform(delete("/api/custom-tasks/{id}", aliceCustomTaskId)
                        .header("Authorization", "Bearer " + bobJwt))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Custom task not found"));
    }

    // =================================================================
    // Schedule isolation — policy: 404 (not found or not owned)
    // =================================================================

    @Test
    @DisplayName("Bob cannot MOVE Alice's scheduled task → 404")
    void bob_moveAliceScheduledTask_returns404() throws Exception {
        UUID aliceCustomTaskId = aliceCreatesCustomTask();
        UUID aliceScheduledTaskId = aliceSchedulesCustomTask(aliceCustomTaskId);

        Map<String, Object> moveReq = new HashMap<>();
        moveReq.put("date", LocalDate.now().plusDays(1).toString());
        moveReq.put("startTime", LocalTime.of(11, 0).toString());
        moveReq.put("endTime", LocalTime.of(12, 0).toString());

        mockMvc.perform(put("/api/schedule/task/{id}/move", aliceScheduledTaskId)
                        .header("Authorization", "Bearer " + bobJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(moveReq)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Task not found"));
    }
}