package com.mymindmirror.backend.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mymindmirror.backend.enums.Status;
import com.mymindmirror.backend.model.Milestone;
import com.mymindmirror.backend.model.Task;
import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.payload.response.MilestoneResponse;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(MilestoneMapperTest.TestConfig.class)
class MilestoneMapperTest {

    @Configuration
    @ComponentScan(basePackages = "com.mymindmirror.backend.mapper")
    static class TestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @Autowired
    private MilestoneMapper milestoneMapper;

    // ============================================================
    // Helpers
    // ============================================================

    private Milestone buildFullMilestone() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPasswordHash("hashed");

        Milestone milestone = new Milestone();
        milestone.setId(UUID.randomUUID());
        milestone.setUser(user);
        milestone.setTitle("Launch MVP");
        milestone.setDescription("Ship the minimum viable product");
        milestone.setCreationDate(LocalDate.of(2026, 9, 1));
        milestone.setDueDate(LocalDate.of(2026, 12, 31));
        milestone.setStatus(Status.IN_PROGRESS);

        Task task = new Task();
        task.setId(UUID.randomUUID());
        task.setMilestone(milestone);
        task.setDescription("Design UI");
        task.setCreationTimestamp(LocalDateTime.of(2026, 9, 2, 10, 0));
        task.setDueDate(LocalDate.of(2026, 9, 15));
        task.setStatus(Status.PENDING);
        task.setDetails("Sketch wireframes and design mockups");
        task.setSubtasksJson("[\"Wireframes\",\"Mockups\"]");
        task.setRoadmapTaskId(UUID.randomUUID());

        milestone.setTasks(new ArrayList<>(List.of(task)));
        return milestone;
    }

    // ============================================================
    // toResponse()
    // ============================================================

    @Test
    @DisplayName("toResponse() should map a full milestone")
    void toResponse_shouldMapFullMilestone() {
        Milestone milestone = buildFullMilestone();

        MilestoneResponse response = milestoneMapper.toResponse(milestone);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(milestone.getId());
        assertThat(response.title()).isEqualTo("Launch MVP");
        assertThat(response.description()).isEqualTo("Ship the minimum viable product");
        assertThat(response.creationDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(response.dueDate()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(response.status()).isEqualTo(Status.IN_PROGRESS);
    }

    @Test
    @DisplayName("toResponse() should compute completionPercentage from tasks (0% when none completed)")
    void toResponse_shouldComputeCompletionZero() {
        Milestone milestone = buildFullMilestone();

        MilestoneResponse response = milestoneMapper.toResponse(milestone);

        assertThat(response.completionPercentage()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("toResponse() should compute completionPercentage = 100 when all tasks completed")
    void toResponse_shouldComputeCompletionHundred() {
        Milestone milestone = buildFullMilestone();
        milestone.getTasks().forEach(t -> t.setStatus(Status.COMPLETED));

        MilestoneResponse response = milestoneMapper.toResponse(milestone);

        assertThat(response.completionPercentage()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("toResponse() should compute completionPercentage = 50 with mixed tasks")
    void toResponse_shouldComputeCompletionHalf() {
        Milestone milestone = buildFullMilestone();

        Task second = new Task();
        second.setId(UUID.randomUUID());
        second.setMilestone(milestone);
        second.setDescription("Write API");
        second.setCreationTimestamp(LocalDateTime.now());
        second.setStatus(Status.COMPLETED);
        milestone.getTasks().add(second);

        MilestoneResponse response = milestoneMapper.toResponse(milestone);

        assertThat(response.completionPercentage()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("toResponse() should map nested tasks with deserialized subtasks")
    void toResponse_shouldMapNestedTasks() {
        Milestone milestone = buildFullMilestone();

        MilestoneResponse response = milestoneMapper.toResponse(milestone);

        assertThat(response.tasks()).hasSize(1);
        TaskResponse taskDto = response.tasks().get(0);
        assertThat(taskDto.description()).isEqualTo("Design UI");
        assertThat(taskDto.status()).isEqualTo(Status.PENDING);
        assertThat(taskDto.details()).isEqualTo("Sketch wireframes and design mockups");
        assertThat(taskDto.subtasks()).containsExactly("Wireframes", "Mockups");
        assertThat(taskDto.roadmapTaskId()).isNotNull();
    }

    @Test
    @DisplayName("toResponse() should return null when milestone is null")
    void toResponse_shouldReturnNull_whenInputNull() {
        assertThat(milestoneMapper.toResponse(null)).isNull();
    }

    @Test
    @DisplayName("toResponse() should handle null tasks list")
    void toResponse_shouldHandleNullTasksList() {
        Milestone milestone = buildFullMilestone();
        milestone.setTasks(null);

        MilestoneResponse response = milestoneMapper.toResponse(milestone);

        assertThat(response).isNotNull();
        assertThat(response.tasks()).isNull();  // MapStruct default for null list
    }

    @Test
    @DisplayName("toResponse() should handle empty tasks list (0% completion)")
    void toResponse_shouldHandleEmptyTasksList() {
        Milestone milestone = buildFullMilestone();
        milestone.setTasks(new ArrayList<>());

        MilestoneResponse response = milestoneMapper.toResponse(milestone);

        assertThat(response.tasks()).isEmpty();
        assertThat(response.completionPercentage()).isEqualTo(0.0);
    }

    // ============================================================
    // toResponseList()
    // ============================================================

    @Test
    @DisplayName("toResponseList() should map a list of milestones")
    void toResponseList_shouldMapList() {
        List<Milestone> milestones = List.of(buildFullMilestone(), buildFullMilestone());

        List<MilestoneResponse> responses = milestoneMapper.toResponseList(milestones);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).title()).isEqualTo("Launch MVP");
    }

    @Test
    @DisplayName("toResponseList() should return null when input is null (MapStruct default)")
    void toResponseList_shouldReturnNull_whenInputNull() {
        assertThat(milestoneMapper.toResponseList(null)).isNull();
    }

    @Test
    @DisplayName("toResponseList() should return empty list for empty input")
    void toResponseList_shouldReturnEmptyList() {
        assertThat(milestoneMapper.toResponseList(List.of())).isEmpty();
    }

    // ============================================================
    // toTaskResponse()
    // ============================================================

    @Test
    @DisplayName("toTaskResponse() should map a task with subtasks JSON")
    void toTaskResponse_shouldMapTaskWithSubtasks() {
        Task task = new Task();
        task.setId(UUID.randomUUID());
        task.setDescription("Do X");
        task.setStatus(Status.PENDING);
        task.setSubtasksJson("[\"a\",\"b\",\"c\"]");
        task.setDetails("Details here");
        task.setDueDate(LocalDate.of(2026, 10, 1));
        task.setCreationTimestamp(LocalDateTime.now());

        TaskResponse dto = milestoneMapper.toTaskResponse(task);

        assertThat(dto.id()).isEqualTo(task.getId());
        assertThat(dto.description()).isEqualTo("Do X");
        assertThat(dto.subtasks()).containsExactly("a", "b", "c");
        assertThat(dto.details()).isEqualTo("Details here");
        assertThat(dto.dueDate()).isEqualTo(LocalDate.of(2026, 10, 1));
    }

    @Test
    @DisplayName("toTaskResponse() should return empty subtasks when JSON is null")
    void toTaskResponse_shouldHandleNullSubtasks() {
        Task task = new Task();
        task.setId(UUID.randomUUID());
        task.setDescription("Do X");
        task.setSubtasksJson(null);

        TaskResponse dto = milestoneMapper.toTaskResponse(task);

        assertThat(dto.subtasks()).isEmpty();
    }

    @Test
    @DisplayName("toTaskResponse() should return empty subtasks for malformed JSON")
    void toTaskResponse_shouldHandleMalformedSubtasksJson() {
        Task task = new Task();
        task.setId(UUID.randomUUID());
        task.setDescription("Do X");
        task.setSubtasksJson("not valid json");

        TaskResponse dto = milestoneMapper.toTaskResponse(task);

        assertThat(dto.subtasks()).isEmpty();
    }

    @Test
    @DisplayName("toTaskResponse() should return null when task is null")
    void toTaskResponse_shouldReturnNull_whenInputNull() {
        assertThat(milestoneMapper.toTaskResponse(null)).isNull();
    }

    @Test
    @DisplayName("toTaskResponse() should preserve unicode in subtasks")
    void toTaskResponse_shouldPreserveUnicode() {
        Task task = new Task();
        task.setId(UUID.randomUUID());
        task.setDescription("Task");
        task.setSubtasksJson("[\"नमस्ते\",\"🌍\"]");

        TaskResponse dto = milestoneMapper.toTaskResponse(task);

        assertThat(dto.subtasks()).containsExactly("नमस्ते", "🌍");
    }
}