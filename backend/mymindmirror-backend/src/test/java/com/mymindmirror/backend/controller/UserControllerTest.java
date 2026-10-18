package com.mymindmirror.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mymindmirror.backend.mapper.UserMapper;
import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.payload.request.ChangePasswordRequest;
import com.mymindmirror.backend.payload.request.UpdateRoadmapPreferencesRequest;
import com.mymindmirror.backend.payload.request.UpdateUserPreferencesRequest;
import com.mymindmirror.backend.payload.request.UserProfileRequest;
import com.mymindmirror.backend.payload.response.UserFullProfileResponse;
import com.mymindmirror.backend.payload.response.UserPreferencesResponse;
import com.mymindmirror.backend.payload.response.UserProfileResponse;
import com.mymindmirror.backend.payload.response.UserRoadmapPreferencesDto;
import com.mymindmirror.backend.security.JwtUtil;
import com.mymindmirror.backend.security.services.UserDetailsImpl;
import com.mymindmirror.backend.service.ApiKeyService;
import com.mymindmirror.backend.service.JournalReflectionService;
import com.mymindmirror.backend.service.UserService;
import com.mymindmirror.backend.service.ai.DynamicAiClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private ApiKeyService apiKeyService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserMapper userMapper; //  Mock the mapper

    @MockitoBean
    private DynamicAiClientService dynamicAiClientService;

    @MockitoBean
    private JournalReflectionService journalReflectionService;

    private User currentUser;
    private UUID userId;
    private UserProfileResponse mockProfileResponse;
    private UserRoadmapPreferencesDto mockRoadmapPrefs;
    private UserPreferencesResponse mockUserPrefs;
    private UserFullProfileResponse mockFullProfile;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        currentUser = new User();
        currentUser.setId(userId);
        currentUser.setUsername("testuser");
        currentUser.setEmail("test@example.com");
        currentUser.setPasswordHash("hashed");

        when(userService.findById(eq(userId))).thenReturn(Optional.of(currentUser));

        UserDetailsImpl userDetails = new UserDetailsImpl(
                userId, currentUser.getUsername(), currentUser.getEmail(), currentUser.getPasswordHash(), null);
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockProfileResponse = new UserProfileResponse(userId, "testuser", "test@example.com");
        mockRoadmapPrefs = new UserRoadmapPreferencesDto("BEGINNER", "en", "READING", 10, false);
        mockUserPrefs = new UserPreferencesResponse(
                "{\"monday\":[[\"09:00\",\"12:00\"]]}",
                "Asia/Kolkata",
                "MORNING",
                "07:00",
                "23:00",
                "13:00",
                "[]"
        );
        mockFullProfile = new UserFullProfileResponse(
                userId,
                "testuser",
                "test@example.com",
                false,
                null,
                mockRoadmapPrefs,
                "{\"monday\":[[\"09:00\",\"12:00\"]]}",
                "Asia/Kolkata",
                "MORNING",
                "07:00",
                "23:00",
                "13:00",
                "[]"
        );
    }

    // ===== UPDATE PROFILE =====

    @Test
    void updateUserProfile_shouldReturn200() throws Exception {
        UserProfileRequest request = new UserProfileRequest("newusername", "new@example.com");
        when(userService.updateUser(eq(userId), any(UserProfileRequest.class))).thenReturn(currentUser);
        when(userMapper.toProfileResponse(any(User.class))).thenReturn(mockProfileResponse); //  mock mapper

        mockMvc.perform(put("/api/users/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    void updateUserProfile_shouldReturn400_whenUsernameTooShort() throws Exception {
        UserProfileRequest request = new UserProfileRequest("ab", "test@example.com"); // min 3 chars
        mockMvc.perform(put("/api/users/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateUserProfile_shouldReturn400_whenEmailInvalid() throws Exception {
        UserProfileRequest request = new UserProfileRequest("newuser", "invalid-email");
        mockMvc.perform(put("/api/users/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ===== DELETE PROFILE =====

    @Test
    void deleteUserProfile_shouldReturn200() throws Exception {
        doNothing().when(userService).deleteUser(eq(userId));

        mockMvc.perform(delete("/api/users/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User profile deleted successfully."));

        // Verify DB deletion
        verify(userService).deleteUser(eq(userId));

        // Verify JVM Cache evictions
        verify(dynamicAiClientService).evictUserModel(eq(userId));
        verify(journalReflectionService).clearUserCache(eq(userId));
    }

    // ===== CHANGE PASSWORD =====

    @Test
    void changePassword_shouldReturn200() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("oldPass", "newPass123");
        doNothing().when(userService).changeUserPassword(eq(userId), eq("oldPass"), eq("newPass123"));

        mockMvc.perform(put("/api/users/profile/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password changed successfully."));
    }

    @Test
    void changePassword_shouldReturn400_whenCurrentPasswordMissing() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest(null, "newPass123");
        mockMvc.perform(put("/api/users/profile/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changePassword_shouldReturn400_whenNewPasswordTooShort() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("oldPass", "123"); // min 6
        mockMvc.perform(put("/api/users/profile/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ===== UPDATE ROADMAP PREFERENCES =====

    @Test
    void updateRoadmapPreferences_shouldReturn200() throws Exception {
        UpdateRoadmapPreferencesRequest request = new UpdateRoadmapPreferencesRequest(
                "INTERMEDIATE", "es", "VISUAL", 15, true
        );
        when(userService.updateRoadmapPreferences(any(User.class), any(UpdateRoadmapPreferencesRequest.class)))
                .thenReturn(mockRoadmapPrefs);

        mockMvc.perform(put("/api/users/roadmap-preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.difficulty").value("BEGINNER"));
    }

    @Test
    void updateRoadmapPreferences_shouldReturn400_whenInvalidDifficulty() throws Exception {
        UpdateRoadmapPreferencesRequest request = new UpdateRoadmapPreferencesRequest(
                "INVALID", "es", "VISUAL", 15, true
        );
        mockMvc.perform(put("/api/users/roadmap-preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateRoadmapPreferences_shouldReturn400_whenInvalidLearningStyle() throws Exception {
        UpdateRoadmapPreferencesRequest request = new UpdateRoadmapPreferencesRequest(
                "BEGINNER", "es", "INVALID", 15, true
        );
        mockMvc.perform(put("/api/users/roadmap-preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ===== GET FULL PROFILE =====

    @Test
    void getUserFullProfile_shouldReturn200() throws Exception {
        when(apiKeyService.getDecryptedApiKey(any(User.class))).thenReturn(null);
        when(userService.getFullUserProfile(any(User.class), isNull())).thenReturn(mockFullProfile);

        mockMvc.perform(get("/api/users/profile-full"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.usingOwnKey").value(false));
    }

    @Test
    void getUserFullProfile_shouldReturn200_withApiKey() throws Exception {
        when(apiKeyService.getDecryptedApiKey(any(User.class))).thenReturn("sk-abcdefghijklmnop");
        UserFullProfileResponse mockWithKey = new UserFullProfileResponse(
                userId,
                "testuser",
                "test@example.com",
                true,
                "••••••••mnop",
                mockRoadmapPrefs,
                "{\"monday\":[[\"09:00\",\"12:00\"]]}",
                "Asia/Kolkata",
                "MORNING",
                "07:00",
                "23:00",
                "13:00",
                "[]"
        );
        when(userService.getFullUserProfile(any(User.class), anyString())).thenReturn(mockWithKey);

        mockMvc.perform(get("/api/users/profile-full"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usingOwnKey").value(true))
                .andExpect(jsonPath("$.maskedKey").value("••••••••mnop"));
    }

    // ===== UPDATE USER PREFERENCES =====

    @Test
    void updateUserPreferences_shouldReturn200() throws Exception {
        UpdateUserPreferencesRequest request = new UpdateUserPreferencesRequest(
                "{\"monday\":[[\"10:00\",\"12:00\"]]}",
                "America/New_York",
                "EVENING",
                LocalTime.of(8, 0),
                LocalTime.of(22, 0),
                LocalTime.of(12, 0),
                "[\"30 min workout\"]"
        );
        when(userService.updateUserPreferences(any(User.class), any(UpdateUserPreferencesRequest.class)))
                .thenReturn(mockUserPrefs);

        mockMvc.perform(put("/api/users/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timezone").value("Asia/Kolkata"));
    }

    @Test
    void updateUserPreferences_shouldReturn400_whenInvalidEnergyPeak() throws Exception {
        UpdateUserPreferencesRequest request = new UpdateUserPreferencesRequest(
                null, null, "INVALID", null, null, null, null
        );
        mockMvc.perform(put("/api/users/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}