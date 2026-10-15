package com.mymindmirror.backend.mapper;

import com.mymindmirror.backend.model.Roadmap;
import com.mymindmirror.backend.model.RoadmapMilestone;
import com.mymindmirror.backend.model.RoadmapResource;
import com.mymindmirror.backend.model.RoadmapTask;
import com.mymindmirror.backend.payload.response.ElaborationResponseDto;
import com.mymindmirror.backend.payload.response.RoadmapGenerateResponse;
import com.mymindmirror.backend.payload.response.RoadmapResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = JsonMapperHelper.class)
public interface RoadmapMapper {

    // --- Output Mappers (Entity to DTO) ---
    RoadmapResponse toResponse(Roadmap roadmap);
    List<RoadmapResponse> toResponseList(List<Roadmap> roadmaps);

    @Mapping(target = "subtasks", source = "subtasks", qualifiedByName = "jsonToStringList")
    @Mapping(target = "taskType", source = "taskType")
    RoadmapResponse.TaskDto toTaskDto(RoadmapTask task);

    RoadmapResponse.ResourceDto toResourceDto(RoadmapResource resource);
    RoadmapResponse.MilestoneDto toMilestoneDto(RoadmapMilestone milestone);

    @Mapping(target = "subtasks", source = "subtasks", qualifiedByName = "jsonToStringList")
    ElaborationResponseDto toElaborationDto(RoadmapTask task);

    // --- Input Mappers (AI Response DTO to Entity) ---

    // 💡 CRITICAL FIX: Bridges the naming gap between AI DTO and MySQL Entity
    @Mapping(target = "weekNumber", source = "week")
    @Mapping(target = "dayNumber", source = "day")
    @Mapping(target = "taskType", source = "type")
    @Mapping(target = "subtasks", source = "subtasks", qualifiedByName = "stringListToJson")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "roadmap", ignore = true)
    @Mapping(target = "completed", ignore = true)
    @Mapping(target = "importedToMilestone", ignore = true)
    RoadmapTask toEntity(RoadmapGenerateResponse.Task task);


}