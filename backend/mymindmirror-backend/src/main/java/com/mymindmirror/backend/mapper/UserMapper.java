package com.mymindmirror.backend.mapper;

import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.model.UserPreferences;
import com.mymindmirror.backend.model.UserRoadmapPreferences;
import com.mymindmirror.backend.payload.response.UserPreferencesResponse;
import com.mymindmirror.backend.payload.response.UserProfileResponse;
import com.mymindmirror.backend.payload.response.UserResponse;
import com.mymindmirror.backend.payload.response.UserRoadmapPreferencesDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);

    UserProfileResponse toProfileResponse(User user);

    @Mapping(target = "wakeTime", expression = "java(prefs.getWakeTime() != null ? prefs.getWakeTime().toString() : null)")
    @Mapping(target = "sleepTime", expression = "java(prefs.getSleepTime() != null ? prefs.getSleepTime().toString() : null)")
    @Mapping(target = "lunchTime", expression = "java(prefs.getLunchTime() != null ? prefs.getLunchTime().toString() : null)")
    UserPreferencesResponse toPreferencesResponse(UserPreferences prefs);

    UserRoadmapPreferencesDto toRoadmapPreferencesDto(UserRoadmapPreferences prefs);
}