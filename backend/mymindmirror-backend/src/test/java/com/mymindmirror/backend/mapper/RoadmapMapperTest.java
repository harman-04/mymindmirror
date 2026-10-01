package com.mymindmirror.backend.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mymindmirror.backend.model.Roadmap;
import com.mymindmirror.backend.model.RoadmapMilestone;
import com.mymindmirror.backend.model.RoadmapResource;
import com.mymindmirror.backend.model.RoadmapTask;
import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.payload.response.ElaborationResponseDto;
import com.mymindmirror.backend.payload.response.RoadmapGenerateResponse;
import com.mymindmirror.backend.payload.response.RoadmapResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(RoadmapMapperTest.TestConfig.class)
class RoadmapMapperTest {

    @Configuration
    @ComponentScan(basePackages = "com.mymindmirror.backend.mapper")
    static class TestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @Autowired
    private RoadmapMapper roadmapMapper;

    // ============================================================
    // Helpers
    // ============================================================

    private Roadmap buildFullRoadmap() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPasswordHash("hashed");

        Roadmap roadmap = new Roadmap();
        roadmap.setId(UUID.randomUUID());
        roadmap.setUser(user);
        roadmap.setTitle("Learn Spring Boot");
        roadmap.setDescription("Master Spring Boot in 4 weeks");
        roadmap.setCreatedAt(LocalDate.of(2026, 9, 1));
        roadmap.setStatus("ACTIVE");
        roadmap.setDurationWeeks(4);
        roadmap.setGeneratedWeeks(4);
        roadmap.setOriginalDurationValue(4);
        roadmap.setOriginalDurationUnit("WEEKS");

        // Tasks
        RoadmapTask task = new RoadmapTask();
        task.setId(UUID.randomUUID());
        task.setRoadmap(roadmap);
        task.setDescription("Setup project");
        task.setDetails("Create Maven project with Spring Initializr");
        task.setSubtasks("[\"Create repo\",\"Add dependencies\"]");
        task.setWeekNumber(1);
        task.setDayNumber(1);
        task.setCompleted(true);
        task.setTaskType("daily");
        task.setImportedToMilestone(false);
        roadmap.setTasks(new ArrayList<>(List.of(task)));

        // Resources
        RoadmapResource resource = new RoadmapResource();
        resource.setId(UUID.randomUUID());
        resource.setRoadmap(roadmap);
        resource.setName("Spring Docs");
        resource.setUrl("https://spring.io/docs");
        resource.setType("article");
        roadmap.setResources(new ArrayList<>(List.of(resource)));

// Milestones
        RoadmapMilestone milestone = new RoadmapMilestone();
        milestone.setId(UUID.randomUUID());
        milestone.setRoadmap(roadmap);
        milestone.setName("Foundation complete");
        milestone.setWeekNumber(2);
        milestone.setAchieved(false);
        roadmap.setMilestones(new ArrayList<>(List.of(milestone)));
        return roadmap;
    }

    // ============================================================
    // toResponse()
    // ============================================================

    @Test
    @DisplayName("toResponse() should map a full roadmap including tasks, resources, milestones")
    void toResponse_shouldMapFullRoadmap() {
        Roadmap roadmap = buildFullRoadmap();

        RoadmapResponse response = roadmapMapper.toResponse(roadmap);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(roadmap.getId());
        assertThat(response.title()).isEqualTo("Learn Spring Boot");
        assertThat(response.description()).isEqualTo("Master Spring Boot in 4 weeks");
        assertThat(response.createdAt()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.durationWeeks()).isEqualTo(4);
        assertThat(response.generatedWeeks()).isEqualTo(4);
        assertThat(response.originalDurationValue()).isEqualTo(4);
        assertThat(response.originalDurationUnit()).isEqualTo("WEEKS");
    }

    @Test
    @DisplayName("toResponse() should map nested tasks with deserialized subtasks")
    void toResponse_shouldMapTasksWithSubtasks() {
        Roadmap roadmap = buildFullRoadmap();

        RoadmapResponse response = roadmapMapper.toResponse(roadmap);

        assertThat(response.tasks()).hasSize(1);
        RoadmapResponse.TaskDto taskDto = response.tasks().get(0);
        assertThat(taskDto.description()).isEqualTo("Setup project");
        assertThat(taskDto.weekNumber()).isEqualTo(1);
        assertThat(taskDto.dayNumber()).isEqualTo(1);
        assertThat(taskDto.completed()).isTrue();
        assertThat(taskDto.taskType()).isEqualTo("daily");
        assertThat(taskDto.subtasks()).containsExactly("Create repo", "Add dependencies");
        assertThat(taskDto.importedToMilestone()).isFalse();
    }

    @Test
    @DisplayName("toResponse() should map nested resources")
    void toResponse_shouldMapResources() {
        Roadmap roadmap = buildFullRoadmap();

        RoadmapResponse response = roadmapMapper.toResponse(roadmap);

        assertThat(response.resources()).hasSize(1);
        RoadmapResponse.ResourceDto resourceDto = response.resources().get(0);
        assertThat(resourceDto.name()).isEqualTo("Spring Docs");
        assertThat(resourceDto.url()).isEqualTo("https://spring.io/docs");
        assertThat(resourceDto.type()).isEqualTo("article");
    }

    @Test
    @DisplayName("toResponse() should map nested milestones")
    void toResponse_shouldMapMilestones() {
        Roadmap roadmap = buildFullRoadmap();

        RoadmapResponse response = roadmapMapper.toResponse(roadmap);

        assertThat(response.milestones()).hasSize(1);
        RoadmapResponse.MilestoneDto milestoneDto = response.milestones().get(0);
        assertThat(milestoneDto.name()).isEqualTo("Foundation complete");
        assertThat(milestoneDto.weekNumber()).isEqualTo(2);
        assertThat(milestoneDto.achieved()).isFalse();
    }

    @Test
    @DisplayName("toResponse() should return null when roadmap is null")
    void toResponse_shouldReturnNull_whenInputNull() {
        assertThat(roadmapMapper.toResponse(null)).isNull();
    }

    @Test
    @DisplayName("toResponse() should handle empty tasks/resources/milestones")
    void toResponse_shouldHandleEmptyCollections() {
        Roadmap roadmap = buildFullRoadmap();
        roadmap.setTasks(new ArrayList<>());
        roadmap.setResources(new ArrayList<>());
        roadmap.setMilestones(new ArrayList<>());

        RoadmapResponse response = roadmapMapper.toResponse(roadmap);

        assertThat(response.tasks()).isEmpty();
        assertThat(response.resources()).isEmpty();
        assertThat(response.milestones()).isEmpty();
    }

    // ============================================================
    // toResponseList()
    // ============================================================

    @Test
    @DisplayName("toResponseList() should map a list of roadmaps")
    void toResponseList_shouldMapList() {
        List<Roadmap> roadmaps = List.of(buildFullRoadmap(), buildFullRoadmap());

        List<RoadmapResponse> responses = roadmapMapper.toResponseList(roadmaps);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).title()).isEqualTo("Learn Spring Boot");
        assertThat(responses.get(1).title()).isEqualTo("Learn Spring Boot");
    }

    @Test
    @DisplayName("toResponseList() should return null for null list (MapStruct default)")
    void toResponseList_shouldHandleNull() {
        assertThat(roadmapMapper.toResponseList(null)).isNull();
    }

    @Test
    @DisplayName("toResponseList() should return empty list for empty input")
    void toResponseList_shouldHandleEmptyList() {
        assertThat(roadmapMapper.toResponseList(List.of())).isEmpty();
    }

    // ============================================================
    // toTaskDto()
    // ============================================================

    @Test
    @DisplayName("toTaskDto() should deserialize subtasks JSON into a list")
    void toTaskDto_shouldDeserializeSubtasks() {
        RoadmapTask task = new RoadmapTask();
        task.setId(UUID.randomUUID());
        task.setDescription("Do X");
        task.setSubtasks("[\"a\",\"b\",\"c\"]");
        task.setTaskType("daily");

        RoadmapResponse.TaskDto dto = roadmapMapper.toTaskDto(task);

        assertThat(dto.description()).isEqualTo("Do X");
        assertThat(dto.subtasks()).containsExactly("a", "b", "c");
        assertThat(dto.taskType()).isEqualTo("daily");
    }

    @Test
    @DisplayName("toTaskDto() should return empty subtasks when JSON is null")
    void toTaskDto_shouldHandleNullSubtasks() {
        RoadmapTask task = new RoadmapTask();
        task.setId(UUID.randomUUID());
        task.setDescription("Do X");
        task.setSubtasks(null);

        RoadmapResponse.TaskDto dto = roadmapMapper.toTaskDto(task);

        assertThat(dto.subtasks()).isEmpty();
    }

    @Test
    @DisplayName("toTaskDto() should return null when task is null")
    void toTaskDto_shouldReturnNull_whenInputNull() {
        assertThat(roadmapMapper.toTaskDto(null)).isNull();
    }

    // ============================================================
    // toResourceDto() and toMilestoneDto()
    // ============================================================

    @Test
    @DisplayName("toResourceDto() should map all fields")
    void toResourceDto_shouldMapAllFields() {
        RoadmapResource resource = new RoadmapResource();
        resource.setId(UUID.randomUUID());
        resource.setName("Docs");
        resource.setUrl("https://x");
        resource.setType("video");

        RoadmapResponse.ResourceDto dto = roadmapMapper.toResourceDto(resource);

        assertThat(dto.id()).isEqualTo(resource.getId());
        assertThat(dto.name()).isEqualTo("Docs");
        assertThat(dto.url()).isEqualTo("https://x");
        assertThat(dto.type()).isEqualTo("video");
    }

    @Test
    @DisplayName("toResourceDto() should return null when input is null")
    void toResourceDto_shouldReturnNull_whenInputNull() {
        assertThat(roadmapMapper.toResourceDto(null)).isNull();
    }

    @Test
    @DisplayName("toMilestoneDto() should map all fields")
    void toMilestoneDto_shouldMapAllFields() {
        RoadmapMilestone milestone = new RoadmapMilestone();
        milestone.setId(UUID.randomUUID());
        milestone.setName("Milestone 1");
        milestone.setWeekNumber(3);
        milestone.setAchieved(true);

        RoadmapResponse.MilestoneDto dto = roadmapMapper.toMilestoneDto(milestone);

        assertThat(dto.id()).isEqualTo(milestone.getId());
        assertThat(dto.name()).isEqualTo("Milestone 1");
        assertThat(dto.weekNumber()).isEqualTo(3);
        assertThat(dto.achieved()).isTrue();
    }

    @Test
    @DisplayName("toMilestoneDto() should return null when input is null")
    void toMilestoneDto_shouldReturnNull_whenInputNull() {
        assertThat(roadmapMapper.toMilestoneDto(null)).isNull();
    }

    // ============================================================
    // toEntity() — AI DTO → Entity bridge
    // ============================================================

    @Test
    @DisplayName("toEntity() should rename AI fields: week→weekNumber, day→dayNumber, type→taskType")
    void toEntity_shouldRenameAiFields() {
        RoadmapGenerateResponse.Task aiTask = new RoadmapGenerateResponse.Task(
                2,             // day → dayNumber
                3,             // week → weekNumber
                "Learn Spring Security",
                "daily",       // type → taskType
                "Detailed instructions",
                List.of("step1", "step2")
        );

        RoadmapTask entity = roadmapMapper.toEntity(aiTask);

        assertThat(entity).isNotNull();
        assertThat(entity.getWeekNumber()).isEqualTo(3);       // ✅ renamed
        assertThat(entity.getDayNumber()).isEqualTo(2);        // ✅ renamed
        assertThat(entity.getTaskType()).isEqualTo("daily");   // ✅ renamed
        assertThat(entity.getDescription()).isEqualTo("Learn Spring Security");
        assertThat(entity.getDetails()).isEqualTo("Detailed instructions");
        // ✅ subtasks List<String> → JSON string
        assertThat(entity.getSubtasks()).contains("step1").contains("step2");
    }

    @Test
    @DisplayName("toEntity() should ignore id, roadmap, completed, importedToMilestone")
    void toEntity_shouldIgnoreManagedFields() {
        RoadmapGenerateResponse.Task aiTask = new RoadmapGenerateResponse.Task(
                1, 1, "Task", "daily", "Details", List.of()
        );

        RoadmapTask entity = roadmapMapper.toEntity(aiTask);

        assertThat(entity.getId()).isNull();
        assertThat(entity.getRoadmap()).isNull();
        assertThat(entity.isCompleted()).isFalse();
        assertThat(entity.getImportedToMilestone()).isFalse();
    }

    @Test
    @DisplayName("toEntity() should handle empty subtasks list")
    void toEntity_shouldHandleEmptySubtasks() {
        RoadmapGenerateResponse.Task aiTask = new RoadmapGenerateResponse.Task(
                1, 1, "Task", "daily", "Details", List.of()
        );

        RoadmapTask entity = roadmapMapper.toEntity(aiTask);

        // JsonMapperHelper.stringListToJson returns null for empty list
        assertThat(entity.getSubtasks()).isNull();
    }

    @Test
    @DisplayName("toEntity() should handle null subtasks list")
    void toEntity_shouldHandleNullSubtasks() {
        RoadmapGenerateResponse.Task aiTask = new RoadmapGenerateResponse.Task(
                1, 1, "Task", "daily", "Details", null
        );

        RoadmapTask entity = roadmapMapper.toEntity(aiTask);

        assertThat(entity.getSubtasks()).isNull();
    }

    @Test
    @DisplayName("toEntity() should return null when AI task is null")
    void toEntity_shouldReturnNull_whenInputNull() {
        assertThat(roadmapMapper.toEntity(null)).isNull();
    }

    @Test
    @DisplayName("toEntity() should handle unicode subtasks")
    void toEntity_shouldHandleUnicodeSubtasks() {
        RoadmapGenerateResponse.Task aiTask = new RoadmapGenerateResponse.Task(
                1, 1, "Task", "daily", "Details", List.of("नमस्ते", "🌍")
        );

        RoadmapTask entity = roadmapMapper.toEntity(aiTask);

        assertThat(entity.getSubtasks()).contains("नमस्ते").contains("🌍");
    }

    // ============================================================
// toElaborationDto() — null input
// ============================================================

    @Test
    @DisplayName("toElaborationDto() should return null when task is null")
    void toElaborationDto_shouldReturnNull_whenInputNull() {
        assertThat(roadmapMapper.toElaborationDto(null)).isNull();
    }

    @Test
    @DisplayName("toElaborationDto() should map a task with subtasks")
    void toElaborationDto_shouldMapTaskWithSubtasks() {
        RoadmapTask task = new RoadmapTask();
        task.setId(UUID.randomUUID());
        task.setDetails("Detailed instructions");
        task.setSubtasks("[\"a\",\"b\"]");

        ElaborationResponseDto dto = roadmapMapper.toElaborationDto(task);

        assertThat(dto.id()).isEqualTo(task.getId());
        assertThat(dto.details()).isEqualTo("Detailed instructions");
        assertThat(dto.subtasks()).containsExactly("a", "b");
    }

    @Test
    @DisplayName("toElaborationDto() should return empty subtasks when JSON is null")
    void toElaborationDto_shouldHandleNullSubtasks() {
        RoadmapTask task = new RoadmapTask();
        task.setId(UUID.randomUUID());
        task.setDetails("Details");
        task.setSubtasks(null);

        ElaborationResponseDto dto = roadmapMapper.toElaborationDto(task);

        assertThat(dto.subtasks()).isEmpty();
    }

// ============================================================
// toResponse() with NULL collections — triggers the three internal list helpers
// ============================================================

    @Test
    @DisplayName("toResponse() should call list helper with null when tasks is null")
    void toResponse_shouldCoverTaskListNullBranch() {
        Roadmap roadmap = buildFullRoadmap();
        roadmap.setTasks(null);

        RoadmapResponse response = roadmapMapper.toResponse(roadmap);

        assertThat(response).isNotNull();
        assertThat(response.tasks()).isNull();  // helper returns null
    }

    @Test
    @DisplayName("toResponse() should call list helper with null when resources is null")
    void toResponse_shouldCoverResourceListNullBranch() {
        Roadmap roadmap = buildFullRoadmap();
        roadmap.setResources(null);

        RoadmapResponse response = roadmapMapper.toResponse(roadmap);

        assertThat(response).isNotNull();
        assertThat(response.resources()).isNull();
    }

    @Test
    @DisplayName("toResponse() should call list helper with null when milestones is null")
    void toResponse_shouldCoverMilestoneListNullBranch() {
        Roadmap roadmap = buildFullRoadmap();
        roadmap.setMilestones(null);

        RoadmapResponse response = roadmapMapper.toResponse(roadmap);

        assertThat(response).isNotNull();
        assertThat(response.milestones()).isNull();
    }
}