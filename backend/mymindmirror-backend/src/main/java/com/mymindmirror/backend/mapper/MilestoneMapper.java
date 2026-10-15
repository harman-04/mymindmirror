// src/main/java/com/mymindmirror/backend/mapper/MilestoneMapper.java
package com.mymindmirror.backend.mapper;

import com.mymindmirror.backend.model.Milestone;
import com.mymindmirror.backend.model.Task;
import com.mymindmirror.backend.payload.response.MilestoneResponse;
import com.mymindmirror.backend.payload.response.TaskResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = JsonMapperHelper.class)
public interface MilestoneMapper {

    MilestoneResponse toResponse(Milestone milestone);
    List<MilestoneResponse> toResponseList(List<Milestone> milestones);

    @Mapping(target = "subtasks", source = "subtasksJson", qualifiedByName = "jsonToStringList")
    TaskResponse toTaskResponse(Task task);
}