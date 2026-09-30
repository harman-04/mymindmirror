// src/main/java/com/mymindmirror/backend/mapper/TaskMapper.java
package com.mymindmirror.backend.mapper;

import com.mymindmirror.backend.model.Task;
import com.mymindmirror.backend.payload.request.TaskRequest;
import com.mymindmirror.backend.payload.response.TaskResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

//  nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
@Mapper(
        componentModel = "spring",
        uses = JsonMapperHelper.class,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface TaskMapper {

    @Mapping(target = "subtasks", source = "subtasksJson", qualifiedByName = "jsonToStringList")
    TaskResponse toResponse(Task task);;

    List<TaskResponse> toResponseList(List<Task> tasks);

    @Mapping(target = "subtasksJson", source = "subtasks", qualifiedByName = "stringListToJson")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "milestone", ignore = true)
    @Mapping(target = "creationTimestamp", ignore = true)
    @Mapping(target = "status", ignore = true)
    Task toEntity(TaskRequest request);

    @Mapping(target = "subtasksJson", source = "subtasks", qualifiedByName = "stringListToJson")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "milestone", ignore = true)
    @Mapping(target = "creationTimestamp", ignore = true)
    void updateEntity(@MappingTarget Task task, TaskRequest request);
}