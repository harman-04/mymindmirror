package com.mymindmirror.backend.mapper;

import com.mymindmirror.backend.model.ScheduledTask;
import com.mymindmirror.backend.payload.response.ScheduledTaskResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ScheduleMapper {
    ScheduledTaskResponse toResponse(ScheduledTask task);
    List<ScheduledTaskResponse> toResponseList(List<ScheduledTask> tasks);
}