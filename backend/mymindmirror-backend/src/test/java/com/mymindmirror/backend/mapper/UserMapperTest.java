package com.mymindmirror.backend.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.model.UserPreferences;
import com.mymindmirror.backend.model.UserRoadmapPreferences;
import com.mymindmirror.backend.payload.response.UserPreferencesResponse;
import com.mymindmirror.backend.payload.response.UserProfileResponse;
import com.mymindmirror.backend.payload.response.UserResponse;
import com.mymindmirror.backend.payload.response.UserRoadmapPreferencesDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(UserMapperTest.TestConfig.class)
class UserMapperTest {

    @Configuration
    @ComponentScan(basePackages = "com.mymindmirror.backend.mapper")
    static class TestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @Autowired
    private UserMapper userMapper;

    // ============================================================
    // toResponse() – User → UserResponse
    // ============================================================

    @Test
    @DisplayName("toResponse() should map all user fields")
    void toResponse_shouldMapAllFields() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPasswordHash("hashed-secret");

        UserResponse dto = userMapper.toResponse(user);

        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(user.getId());
        assertThat(dto.username()).isEqualTo("testuser");
        assertThat(dto.email()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("toResponse() should return null when user is null")
    void toResponse_shouldReturnNull_whenInputNull() {
        assertThat(userMapper.toResponse(null)).isNull();
    }

    // ============================================================
    // toProfileResponse() – User → UserProfileResponse
    // ============================================================

    @Test
    @DisplayName("toProfileResponse() should map all fields")
    void toProfileResponse_shouldMapAllFields() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPasswordHash("hashed");

        UserProfileResponse dto = userMapper.toProfileResponse(user);

        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(user.getId());
        assertThat(dto.username()).isEqualTo("testuser");
        assertThat(dto.email()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("toProfileResponse() should return null when user is null")
    void toProfileResponse_shouldReturnNull_whenInputNull() {
        assertThat(userMapper.toProfileResponse(null)).isNull();
    }

    // ============================================================
    // toPreferencesResponse() – UserPreferences → UserPreferencesResponse
    // ============================================================

    @Test
    @DisplayName("toPreferencesResponse() should map all fields and convert LocalTime to String")
    void toPreferencesResponse_shouldMapAllFields() {
        UserPreferences prefs = new UserPreferences();
        prefs.setAvailableHoursJson("{\"monday\":[[\"09:00\",\"12:00\"]]}");
        prefs.setTimezone("Asia/Kolkata");
        prefs.setEnergyPeak("MORNING");
        prefs.setWakeTime(LocalTime.of(7, 0));
        prefs.setSleepTime(LocalTime.of(23, 0));
        prefs.setLunchTime(LocalTime.of(13, 30));
        prefs.setDailyHabitsJson("[\"30 min workout\"]");

        UserPreferencesResponse dto = userMapper.toPreferencesResponse(prefs);

        assertThat(dto).isNotNull();
        assertThat(dto.availableHoursJson()).isEqualTo("{\"monday\":[[\"09:00\",\"12:00\"]]}");
        assertThat(dto.timezone()).isEqualTo("Asia/Kolkata");
        assertThat(dto.energyPeak()).isEqualTo("MORNING");
        // ✅ LocalTime → String via expression
        assertThat(dto.wakeTime()).isEqualTo("07:00");
        assertThat(dto.sleepTime()).isEqualTo("23:00");
        assertThat(dto.lunchTime()).isEqualTo("13:30");
        assertThat(dto.dailyHabitsJson()).isEqualTo("[\"30 min workout\"]");
    }

    @Test
    @DisplayName("toPreferencesResponse() should return null for null LocalTime fields")
    void toPreferencesResponse_shouldHandleNullLocalTimes() {
        UserPreferences prefs = new UserPreferences();
        prefs.setAvailableHoursJson("{}");
        prefs.setTimezone("UTC");
        prefs.setEnergyPeak("EVENING");
        prefs.setWakeTime(null);
        prefs.setSleepTime(null);
        prefs.setLunchTime(null);
        prefs.setDailyHabitsJson("[]");

        UserPreferencesResponse dto = userMapper.toPreferencesResponse(prefs);

        assertThat(dto.wakeTime()).isNull();
        assertThat(dto.sleepTime()).isNull();
        assertThat(dto.lunchTime()).isNull();
    }

    @Test
    @DisplayName("toPreferencesResponse() should return null when input is null")
    void toPreferencesResponse_shouldReturnNull_whenInputNull() {
        assertThat(userMapper.toPreferencesResponse(null)).isNull();
    }

    // ============================================================
    // toRoadmapPreferencesDto() – UserRoadmapPreferences → DTO
    // ============================================================

    @Test
    @DisplayName("toRoadmapPreferencesDto() should map all fields")
    void toRoadmapPreferencesDto_shouldMapAllFields() {
        UserRoadmapPreferences prefs = new UserRoadmapPreferences();
        prefs.setDifficulty("INTERMEDIATE");
        prefs.setLanguagePreference("hi");
        prefs.setLearningStyle("VISUAL");
        prefs.setHoursPerWeek(15);
        prefs.setAvoidWeekends(true);

        UserRoadmapPreferencesDto dto = userMapper.toRoadmapPreferencesDto(prefs);

        assertThat(dto).isNotNull();
        assertThat(dto.difficulty()).isEqualTo("INTERMEDIATE");
        assertThat(dto.languagePreference()).isEqualTo("hi");
        assertThat(dto.learningStyle()).isEqualTo("VISUAL");
        assertThat(dto.hoursPerWeek()).isEqualTo(15);
        assertThat(dto.avoidWeekends()).isTrue();
    }

    @Test
    @DisplayName("toRoadmapPreferencesDto() should return null when input is null")
    void toRoadmapPreferencesDto_shouldReturnNull_whenInputNull() {
        assertThat(userMapper.toRoadmapPreferencesDto(null)).isNull();
    }
}