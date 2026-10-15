package com.mymindmirror.backend.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mymindmirror.backend.model.CustomTask;
import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.payload.request.CustomTaskRequest;
import com.mymindmirror.backend.payload.response.CustomTaskResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(CustomTaskMapperTest.TestConfig.class)
class CustomTaskMapperTest {

    @Configuration
    @ComponentScan(basePackages = "com.mymindmirror.backend.mapper")
    static class TestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @Autowired
    private CustomTaskMapper customTaskMapper;

    // ============================================================
    // Helper
    // ============================================================

    private CustomTask buildTask() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPasswordHash("hashed");

        CustomTask task = new CustomTask();
        task.setId(UUID.randomUUID());
        task.setUser(user);
        task.setTitle("Read 30 pages");
        task.setDescription("Daily reading habit");
        task.setDueDate(LocalDate.of(2026, 10, 15));
        task.setEstimatedHours(1.5);
        task.setPriority("HIGH");
        task.setCompleted(false);
        task.setCreatedAt(LocalDate.of(2026, 9, 10));
        return task;
    }

    // ============================================================
    // toResponse()
    // ============================================================

    @Test
    @DisplayName("toResponse() should map all fields")
    void toResponse_shouldMapAllFields() {
        CustomTask task = buildTask();

        CustomTaskResponse dto = customTaskMapper.toResponse(task);

        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(task.getId());
        assertThat(dto.title()).isEqualTo("Read 30 pages");
        assertThat(dto.description()).isEqualTo("Daily reading habit");
        assertThat(dto.dueDate()).isEqualTo(LocalDate.of(2026, 10, 15));
        assertThat(dto.estimatedHours()).isEqualTo(1.5);
        assertThat(dto.priority()).isEqualTo("HIGH");
        assertThat(dto.completed()).isFalse();
        assertThat(dto.createdAt()).isEqualTo(LocalDate.of(2026, 9, 10));
    }

    @Test
    @DisplayName("toResponse() should return null when task is null")
    void toResponse_shouldReturnNull_whenInputNull() {
        assertThat(customTaskMapper.toResponse(null)).isNull();
    }

    // ============================================================
    // toResponseList()
    // ============================================================

    @Test
    @DisplayName("toResponseList() should map a list of tasks")
    void toResponseList_shouldMapList() {
        List<CustomTask> tasks = List.of(buildTask(), buildTask());

        List<CustomTaskResponse> dtos = customTaskMapper.toResponseList(tasks);

        assertThat(dtos).hasSize(2);
        assertThat(dtos.get(0).title()).isEqualTo("Read 30 pages");
    }

    @Test
    @DisplayName("toResponseList() should return null for null input (MapStruct default)")
    void toResponseList_shouldReturnNull_whenInputNull() {
        assertThat(customTaskMapper.toResponseList(null)).isNull();
    }

    @Test
    @DisplayName("toResponseList() should return empty list for empty input")
    void toResponseList_shouldReturnEmptyForEmpty() {
        assertThat(customTaskMapper.toResponseList(List.of())).isEmpty();
    }

    // ============================================================
    // toEntity() – CustomTaskRequest → CustomTask
    // ============================================================

    @Test
    @DisplayName("toEntity() should map all request fields to a new entity")
    void toEntity_shouldMapAllFields() {
        CustomTaskRequest request = new CustomTaskRequest(
                "Write blog post",
                "Draft and publish 1000-word post",
                LocalDate.of(2026, 11, 1),
                2.5,
                "MEDIUM",
                false
        );

        CustomTask task = customTaskMapper.toEntity(request);

        assertThat(task).isNotNull();
        assertThat(task.getTitle()).isEqualTo("Write blog post");
        assertThat(task.getDescription()).isEqualTo("Draft and publish 1000-word post");
        assertThat(task.getDueDate()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(task.getEstimatedHours()).isEqualTo(2.5);
        assertThat(task.getPriority()).isEqualTo("MEDIUM");
        assertThat(task.isCompleted()).isFalse();
    }

    @Test
    @DisplayName("toEntity() should IGNORE id, user, createdAt (set by service)")
    void toEntity_shouldIgnoreManagedFields() {
        CustomTaskRequest request = new CustomTaskRequest(
                "Task", "desc", LocalDate.now(), 1.0, "LOW", false
        );

        CustomTask task = customTaskMapper.toEntity(request);

        assertThat(task.getId()).isNull();
        assertThat(task.getUser()).isNull();
        assertThat(task.getCreatedAt()).isNull();
    }

    @Test
    @DisplayName("toEntity() should return null when request is null")
    void toEntity_shouldReturnNull_whenInputNull() {
        assertThat(customTaskMapper.toEntity(null)).isNull();
    }

    // ============================================================
    // updateEntity() – CustomTaskRequest → existing CustomTask
    // ============================================================

    @Test
    @DisplayName("updateEntity() should update mutable fields and preserve managed fields")
    void updateEntity_shouldUpdateMutableAndPreserveManaged() {
        CustomTask existing = buildTask();
        UUID originalId = existing.getId();
        User originalUser = existing.getUser();
        LocalDate originalCreatedAt = existing.getCreatedAt();

        CustomTaskRequest request = new CustomTaskRequest(
                "Updated title",
                "Updated description",
                LocalDate.of(2026, 12, 31),
                3.0,
                "LOW",
                true
        );

        customTaskMapper.updateEntity(existing, request);

        // ✅ Updated fields
        assertThat(existing.getTitle()).isEqualTo("Updated title");
        assertThat(existing.getDescription()).isEqualTo("Updated description");
        assertThat(existing.getDueDate()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(existing.getEstimatedHours()).isEqualTo(3.0);
        assertThat(existing.getPriority()).isEqualTo("LOW");
        assertThat(existing.isCompleted()).isTrue();

        // ✅ Managed fields untouched
        assertThat(existing.getId()).isEqualTo(originalId);
        assertThat(existing.getUser()).isEqualTo(originalUser);
        assertThat(existing.getCreatedAt()).isEqualTo(originalCreatedAt);
    }

    @Test
    @DisplayName("updateEntity() should handle null optional fields in request")
    void updateEntity_shouldHandleNullOptionalFields() {
        CustomTask existing = buildTask();

        CustomTaskRequest request = new CustomTaskRequest(
                "Title only",
                null,        // description
                null,        // dueDate
                1.0,
                null,        // priority
                null         // completed
        );

        customTaskMapper.updateEntity(existing, request);

        assertThat(existing.getTitle()).isEqualTo("Title only");
        assertThat(existing.getDescription()).isNull();
        assertThat(existing.getDueDate()).isNull();
        assertThat(existing.getPriority()).isNull();
    }

    @Test
    @DisplayName("updateEntity() should not crash when request is null")
    void updateEntity_shouldHandleNullRequest() {
        CustomTask existing = buildTask();
        UUID originalId = existing.getId();
        String originalTitle = existing.getTitle();

        customTaskMapper.updateEntity(existing, null);

        // Nothing changed
        assertThat(existing.getId()).isEqualTo(originalId);
        assertThat(existing.getTitle()).isEqualTo(originalTitle);
    }

    @Test
    @DisplayName("toEntity() should not set completed when request.completed is null")
    void toEntity_shouldHandleNullCompleted() {
        CustomTaskRequest request = new CustomTaskRequest(
                "Title", "desc", LocalDate.now(), 1.0, "MEDIUM", null
        );

        CustomTask task = customTaskMapper.toEntity(request);

        assertThat(task).isNotNull();
        // Default from entity: completed = false (primitive boolean)
        assertThat(task.isCompleted()).isFalse();
    }

    @Test
    @DisplayName("updateEntity() should preserve existing completed when request.completed is null")
    void updateEntity_shouldPreserveCompleted_whenNull() {
        CustomTask existing = buildTask();
        existing.setCompleted(true);   // existing state

        CustomTaskRequest request = new CustomTaskRequest(
                "Title", "desc", LocalDate.now(), 1.0, "MEDIUM", null   // completed null
        );

        customTaskMapper.updateEntity(existing, request);

        // completed NOT overwritten → still true
        assertThat(existing.isCompleted()).isTrue();
    }
}