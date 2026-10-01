package com.mymindmirror.backend.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mymindmirror.backend.enums.Status;
import com.mymindmirror.backend.model.Task;
import com.mymindmirror.backend.payload.request.TaskRequest;
import com.mymindmirror.backend.payload.response.TaskResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(TaskMapperTest.TestConfig.class)
class TaskMapperTest {

    @Configuration
    @ComponentScan(basePackages = "com.mymindmirror.backend.mapper")
    static class TestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @Autowired
    private TaskMapper taskMapper;

    // ============================================================
    // Helpers
    // ============================================================

    private Task buildTask() {
        Task task = new Task();
        task.setId(UUID.randomUUID());
        task.setDescription("Design UI");
        task.setCreationTimestamp(LocalDateTime.of(2026, 9, 10, 12, 0));
        task.setDueDate(LocalDate.of(2026, 10, 1));
        task.setStatus(Status.IN_PROGRESS);
        task.setDetails("Sketch wireframes");
        task.setSubtasksJson("[\"Wireframes\",\"Mockups\"]");
        task.setRoadmapTaskId(UUID.randomUUID());
        return task;
    }

    // ============================================================
    // toResponse()
    // ============================================================

    @Test
    @DisplayName("toResponse() should map all fields including subtasks deserialization")
    void toResponse_shouldMapAllFields() {
        Task task = buildTask();

        TaskResponse dto = taskMapper.toResponse(task);

        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(task.getId());
        assertThat(dto.description()).isEqualTo("Design UI");
        assertThat(dto.creationTimestamp()).isEqualTo(LocalDateTime.of(2026, 9, 10, 12, 0));
        assertThat(dto.dueDate()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(dto.status()).isEqualTo(Status.IN_PROGRESS);
        assertThat(dto.details()).isEqualTo("Sketch wireframes");
        assertThat(dto.subtasks()).containsExactly("Wireframes", "Mockups");
        assertThat(dto.roadmapTaskId()).isEqualTo(task.getRoadmapTaskId());
    }

    @Test
    @DisplayName("toResponse() should return empty subtasks when JSON is null")
    void toResponse_shouldHandleNullSubtasksJson() {
        Task task = buildTask();
        task.setSubtasksJson(null);

        TaskResponse dto = taskMapper.toResponse(task);

        assertThat(dto.subtasks()).isEmpty();
    }

    @Test
    @DisplayName("toResponse() should return empty subtasks for malformed JSON")
    void toResponse_shouldHandleMalformedSubtasksJson() {
        Task task = buildTask();
        task.setSubtasksJson("not json");

        TaskResponse dto = taskMapper.toResponse(task);

        assertThat(dto.subtasks()).isEmpty();
    }

    @Test
    @DisplayName("toResponse() should return null when task is null")
    void toResponse_shouldReturnNull_whenInputNull() {
        assertThat(taskMapper.toResponse(null)).isNull();
    }

    @Test
    @DisplayName("toResponse() should preserve unicode in subtasks")
    void toResponse_shouldPreserveUnicode() {
        Task task = buildTask();
        task.setSubtasksJson("[\"नमस्ते\",\"🌍\"]");

        TaskResponse dto = taskMapper.toResponse(task);

        assertThat(dto.subtasks()).containsExactly("नमस्ते", "🌍");
    }

    // ============================================================
    // toResponseList()
    // ============================================================

    @Test
    @DisplayName("toResponseList() should map a list of tasks")
    void toResponseList_shouldMapList() {
        List<Task> tasks = List.of(buildTask(), buildTask());

        List<TaskResponse> dtos = taskMapper.toResponseList(tasks);

        assertThat(dtos).hasSize(2);
    }

    @Test
    @DisplayName("toResponseList() should return null for null input")
    void toResponseList_shouldReturnNull_whenInputNull() {
        assertThat(taskMapper.toResponseList(null)).isNull();
    }

    @Test
    @DisplayName("toResponseList() should return empty list for empty input")
    void toResponseList_shouldReturnEmptyForEmptyInput() {
        assertThat(taskMapper.toResponseList(List.of())).isEmpty();
    }

    // ============================================================
    // toEntity() – TaskRequest → Task
    // ============================================================

    @Test
    @DisplayName("toEntity() should map all request fields to a new entity")
    void toEntity_shouldMapAllFields() {
        TaskRequest request = new TaskRequest(
                "Write docs",
                LocalDate.of(2026, 11, 1),
                Status.PENDING,
                "Long instructions",
                List.of("a", "b")
        );

        Task task = taskMapper.toEntity(request);

        assertThat(task).isNotNull();
        assertThat(task.getDescription()).isEqualTo("Write docs");
        assertThat(task.getDueDate()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(task.getDetails()).isEqualTo("Long instructions");
        // ✅ subtasks (List) → subtasksJson (String)
        assertThat(task.getSubtasksJson()).contains("a").contains("b");
    }



    @Test
    @DisplayName("toEntity() should IGNORE id and milestone, but retain constructor defaults for creationTimestamp & status")
    void toEntity_shouldIgnoreManagedFields() {
        TaskRequest request = new TaskRequest(
                "Task", LocalDate.now(), Status.IN_PROGRESS, "details", List.of("a")
        );

        Task task = taskMapper.toEntity(request);

        // ✅ Ignored — no source for these, so remain null
        assertThat(task.getId()).isNull();
        assertThat(task.getMilestone()).isNull();

        // ⚠️ The Task() constructor sets defaults, so these are NOT null:
        assertThat(task.getCreationTimestamp()).isNotNull();
        assertThat(task.getStatus()).isEqualTo(Status.PENDING);  // constructor default
    }

    @Test
    @DisplayName("toEntity() should handle null subtasks list")
    void toEntity_shouldHandleNullSubtasks() {
        TaskRequest request = new TaskRequest(
                "Task", LocalDate.now(), Status.PENDING, "details", null
        );

        Task task = taskMapper.toEntity(request);

        // JsonMapperHelper.stringListToJson returns null for null list
        assertThat(task.getSubtasksJson()).isNull();
    }

    @Test
    @DisplayName("toEntity() should handle empty subtasks list")
    void toEntity_shouldHandleEmptySubtasks() {
        TaskRequest request = new TaskRequest(
                "Task", LocalDate.now(), Status.PENDING, "details", List.of()
        );

        Task task = taskMapper.toEntity(request);

        assertThat(task.getSubtasksJson()).isNull();
    }

    @Test
    @DisplayName("toEntity() should return null when request is null")
    void toEntity_shouldReturnNull_whenInputNull() {
        assertThat(taskMapper.toEntity(null)).isNull();
    }

    @Test
    @DisplayName("toEntity() should preserve unicode subtasks")
    void toEntity_shouldPreserveUnicode() {
        TaskRequest request = new TaskRequest(
                "Task", LocalDate.now(), Status.PENDING, "details", List.of("नमस्ते", "🌍")
        );

        Task task = taskMapper.toEntity(request);

        assertThat(task.getSubtasksJson()).contains("नमस्ते").contains("🌍");
    }

    // ============================================================
    // updateEntity() – TaskRequest → existing Task (mutate)
    // ============================================================

    @Test
    @DisplayName("updateEntity() should update fields but preserve managed fields")
    void updateEntity_shouldUpdateMutableFields() {
        Task existing = buildTask();
        UUID originalId = existing.getId();
        LocalDateTime originalCreationTs = existing.getCreationTimestamp();

        TaskRequest request = new TaskRequest(
                "Updated description",
                LocalDate.of(2026, 12, 1),
                Status.COMPLETED,
                "Updated details",
                List.of("x", "y")
        );

        taskMapper.updateEntity(existing, request);

        // ✅ Updated fields
        assertThat(existing.getDescription()).isEqualTo("Updated description");
        assertThat(existing.getDueDate()).isEqualTo(LocalDate.of(2026, 12, 1));
        assertThat(existing.getDetails()).isEqualTo("Updated details");
        assertThat(existing.getSubtasksJson()).contains("x").contains("y");

        // ✅ Ignored fields remain unchanged
        assertThat(existing.getId()).isEqualTo(originalId);
        assertThat(existing.getCreationTimestamp()).isEqualTo(originalCreationTs);
    }

    @Test
    @DisplayName("updateEntity() should keep old subtasksJson when request.subtasks is null (NVPMS.IGNORE)")
    void updateEntity_shouldPreserveOldSubtasks_whenRequestSubtasksNull() {
        Task existing = buildTask();
        existing.setSubtasksJson("[\"old\"]");

        TaskRequest request = new TaskRequest(
                "Updated", LocalDate.now(), Status.PENDING, "details", null
        );

        taskMapper.updateEntity(existing, request);

        // ✅ NVPMS.IGNORE: source null → target NOT updated → old value preserved
        assertThat(existing.getSubtasksJson()).isEqualTo("[\"old\"]");
    }
    @Test
    @DisplayName("updateEntity() should overwrite subtasksJson with empty list → null")
    void updateEntity_shouldHandleEmptySubtasks() {
        Task existing = buildTask();
        existing.setSubtasksJson("[\"old\"]");

        TaskRequest request = new TaskRequest(
                "Updated", LocalDate.now(), Status.PENDING, "details", List.of()
        );

        taskMapper.updateEntity(existing, request);

        assertThat(existing.getSubtasksJson()).isNull();
    }



    @Test
    @DisplayName("updateEntity() should NOT crash when request is null")
    void updateEntity_shouldHandleNullRequest() {
        Task existing = buildTask();
        UUID originalId = existing.getId();

        taskMapper.updateEntity(existing, null);

        // Nothing updated – fields remain
        assertThat(existing.getId()).isEqualTo(originalId);
        assertThat(existing.getDescription()).isEqualTo("Design UI");
    }

    @Test
    @DisplayName("updateEntity() should set subtasksJson to null when request.subtasks is empty list")
    void updateEntity_shouldOverwriteSubtasks_whenEmptyList() {
        Task existing = buildTask();
        existing.setSubtasksJson("[\"old\"]");

        TaskRequest request = new TaskRequest(
                "Updated", LocalDate.now(), Status.PENDING, "details", List.of()
        );

        taskMapper.updateEntity(existing, request);

        // ✅ Empty list → qualified method returns null → target overwritten with null
        assertThat(existing.getSubtasksJson()).isNull();
    }

    @Test
    @DisplayName("updateEntity() should skip fields when request has nulls (IGNORE strategy)")
    void updateEntity_shouldSkipNullFields() {
        Task existing = buildTask();
        String originalDescription = existing.getDescription();
        LocalDate originalDueDate = existing.getDueDate();
        Status originalStatus = existing.getStatus();
        String originalDetails = existing.getDetails();

        // Only subtasks provided; everything else null → those fields should not be touched
        TaskRequest request = new TaskRequest(
                null,                // description
                null,                // dueDate
                null,                // status
                null,                // details
                List.of("new")       // subtasks non-null
        );

        taskMapper.updateEntity(existing, request);

        // All four remained unchanged (IGNORE strategy)
        assertThat(existing.getDescription()).isEqualTo(originalDescription);
        assertThat(existing.getDueDate()).isEqualTo(originalDueDate);
        assertThat(existing.getStatus()).isEqualTo(originalStatus);
        assertThat(existing.getDetails()).isEqualTo(originalDetails);
    }
}