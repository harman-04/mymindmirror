package com.mymindmirror.backend.mapper;

import com.mymindmirror.backend.model.ScheduledTask;
import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.payload.response.ScheduledTaskResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(ScheduleMapperTest.TestConfig.class)
class ScheduleMapperTest {

    @Configuration
    @ComponentScan(basePackages = "com.mymindmirror.backend.mapper")
    static class TestConfig {
        // No extra beans needed – ScheduleMapper has no dependencies
    }

    @Autowired
    private ScheduleMapper scheduleMapper;

    // ============================================================
    // Helpers
    // ============================================================

    private ScheduledTask buildScheduledTask() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPasswordHash("hashed");

        ScheduledTask task = new ScheduledTask();
        task.setId(UUID.randomUUID());
        task.setUser(user);
        task.setRoadmapTaskId(UUID.randomUUID());
        task.setMilestoneTaskId(null);
        task.setCustomTaskId(UUID.randomUUID());
        task.setTitle("Design UI");
        task.setDescription("Sketch wireframes");
        task.setScheduledDate(LocalDate.of(2026, 10, 15));
        task.setStartTime(LocalTime.of(9, 0));
        task.setEndTime(LocalTime.of(10, 30));
        task.setPriority("HIGH");
        task.setCompleted(false);
        task.setReminderSent(false);
        task.setBlockType("WORK_TASK");
        return task;
    }

    // ============================================================
    // toResponse() – ScheduledTask → ScheduledTaskResponse
    // ============================================================

    @Test
    @DisplayName("toResponse() should map all fields including both task-id references")
    void toResponse_shouldMapAllFields() {
        ScheduledTask task = buildScheduledTask();

        ScheduledTaskResponse dto = scheduleMapper.toResponse(task);

        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(task.getId());
        assertThat(dto.title()).isEqualTo("Design UI");
        assertThat(dto.scheduledDate()).isEqualTo(LocalDate.of(2026, 10, 15));
        assertThat(dto.startTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(dto.endTime()).isEqualTo(LocalTime.of(10, 30));
        assertThat(dto.completed()).isFalse();
        assertThat(dto.roadmapTaskId()).isEqualTo(task.getRoadmapTaskId());
        assertThat(dto.milestoneTaskId()).isNull();
        assertThat(dto.customTaskId()).isEqualTo(task.getCustomTaskId());
        assertThat(dto.priority()).isEqualTo("HIGH");
        assertThat(dto.blockType()).isEqualTo("WORK_TASK");
    }

    @Test
    @DisplayName("toResponse() should handle null optional IDs (routine/break blocks)")
    void toResponse_shouldHandleNullOptionalIds() {
        ScheduledTask task = buildScheduledTask();
        task.setRoadmapTaskId(null);
        task.setMilestoneTaskId(null);
        task.setCustomTaskId(null);
        task.setBlockType("ROUTINE");

        ScheduledTaskResponse dto = scheduleMapper.toResponse(task);

        assertThat(dto.roadmapTaskId()).isNull();
        assertThat(dto.milestoneTaskId()).isNull();
        assertThat(dto.customTaskId()).isNull();
        assertThat(dto.blockType()).isEqualTo("ROUTINE");
    }

    @Test
    @DisplayName("toResponse() should map a milestone-task reference correctly")
    void toResponse_shouldMapMilestoneTaskId() {
        ScheduledTask task = buildScheduledTask();
        task.setRoadmapTaskId(null);
        task.setMilestoneTaskId(UUID.randomUUID());

        ScheduledTaskResponse dto = scheduleMapper.toResponse(task);

        assertThat(dto.milestoneTaskId()).isNotNull();
        assertThat(dto.roadmapTaskId()).isNull();
    }

    @Test
    @DisplayName("toResponse() should return null when task is null")
    void toResponse_shouldReturnNull_whenInputNull() {
        assertThat(scheduleMapper.toResponse(null)).isNull();
    }

    // ============================================================
    // toResponseList() – List<ScheduledTask> → List<ScheduledTaskResponse>
    // ============================================================

    @Test
    @DisplayName("toResponseList() should map a list of scheduled tasks")
    void toResponseList_shouldMapList() {
        List<ScheduledTask> tasks = List.of(buildScheduledTask(), buildScheduledTask());

        List<ScheduledTaskResponse> dtos = scheduleMapper.toResponseList(tasks);

        assertThat(dtos).hasSize(2);
        assertThat(dtos.get(0).title()).isEqualTo("Design UI");
        assertThat(dtos.get(1).title()).isEqualTo("Design UI");
    }

    @Test
    @DisplayName("toResponseList() should return null for null input (MapStruct default)")
    void toResponseList_shouldReturnNull_whenInputNull() {
        assertThat(scheduleMapper.toResponseList(null)).isNull();
    }

    @Test
    @DisplayName("toResponseList() should return empty list for empty input")
    void toResponseList_shouldReturnEmptyForEmptyInput() {
        assertThat(scheduleMapper.toResponseList(List.of())).isEmpty();
    }
}