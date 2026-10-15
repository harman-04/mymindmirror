package com.mymindmirror.backend.mapper;

import com.mymindmirror.backend.model.CustomTask;
import com.mymindmirror.backend.payload.request.CustomTaskRequest;
import com.mymindmirror.backend.payload.response.CustomTaskResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CustomTaskMapper {

    CustomTaskResponse toResponse(CustomTask task);
    List<CustomTaskResponse> toResponseList(List<CustomTask> tasks);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    CustomTask toEntity(CustomTaskRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntity(@MappingTarget CustomTask task, CustomTaskRequest request);
}