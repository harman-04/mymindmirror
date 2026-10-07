package com.mymindmirror.backend;

import com.mymindmirror.backend.payload.request.ApiKeyRequest;
import com.mymindmirror.backend.payload.request.ChangePasswordRequest;
import com.mymindmirror.backend.payload.request.UpdateRoadmapPreferencesRequest;
import com.mymindmirror.backend.payload.request.UpdateUserPreferencesRequest;
import com.mymindmirror.backend.payload.request.UserProfileRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalTime;
import static org.assertj.core.api.Assertions.assertThat;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Profile flow — read, update, password, API key, preferences, delete")
class ProfileFlowIT extends AbstractIntegrationTest {

    private String aliceJwt;

    @BeforeEach
    void loginAlice() throws Exception {
        aliceJwt = registerAndLogin("alice", "password123");
    }

    // =================================================================
    // Read
    // =================================================================

    @Test
    @DisplayName("GET /profile-full returns the authenticated user's profile")
    void getProfileFull_returnsOwnProfile() throws Exception {
        mockMvc.perform(get("/api/users/profile-full")
                        .header("Authorization", "Bearer " + aliceJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.email").value("alice@test.com"))
                .andExpect(jsonPath("$.usingOwnKey").value(false));
    }

    // =================================================================
    // Update profile — username + email
    // =================================================================

    @Test
    @DisplayName("PUT /profile updates username and email")
    void updateProfile_changesUsernameAndEmail() throws Exception {
        UserProfileRequest request = new UserProfileRequest("alice_new", "new@test.com");

        mockMvc.perform(put("/api/users/profile")
                        .header("Authorization", "Bearer " + aliceJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice_new"))
                .andExpect(jsonPath("$.email").value("new@test.com"));
    }

    @Test
    @DisplayName("PUT /profile with invalid email → 400 with validation message")
    void updateProfile_invalidEmail_returns400() throws Exception {
        UserProfileRequest request = new UserProfileRequest("alice", "not-an-email");

        mockMvc.perform(put("/api/users/profile")
                        .header("Authorization", "Bearer " + aliceJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value(org.hamcrest.Matchers.containsString("Email should be valid")));
    }

    @Test
    @DisplayName("PUT /profile with short username → 400 with size message")
    void updateProfile_shortUsername_returns400() throws Exception {
        UserProfileRequest request = new UserProfileRequest("ab", "alice@test.com");

        mockMvc.perform(put("/api/users/profile")
                        .header("Authorization", "Bearer " + aliceJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value(org.hamcrest.Matchers.containsString(
                                "Username must be between 3 and 50 characters")));
    }

    @Test
    @DisplayName("PUT /profile with taken username → 400 'Username already taken.'")
    void updateProfile_takenUsername_returns400() throws Exception {
        // Create Bob so his username is taken
        registerAndLogin("bob", "password123");

        UserProfileRequest request = new UserProfileRequest("bob", "alice@test.com");

        mockMvc.perform(put("/api/users/profile")
                        .header("Authorization", "Bearer " + aliceJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Username already taken."));
    }

    // =================================================================
    // Change password
    // =================================================================

    @Test
    @DisplayName("PUT /profile/password success — new password works on next login")
    void changePassword_success_newPasswordWorks() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("password123", "newPassword456");

        mockMvc.perform(put("/api/users/profile/password")
                        .header("Authorization", "Bearer " + aliceJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Old password rejected
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"password123\"}"))
                .andExpect(status().isUnauthorized());

        // New password accepted
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"newPassword456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @DisplayName("PUT /profile/password with wrong current → 400 'Incorrect current password.'")
    void changePassword_wrongCurrent_returns400() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("wrongCurrent", "newPassword456");

        mockMvc.perform(put("/api/users/profile/password")
                        .header("Authorization", "Bearer " + aliceJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Incorrect current password."));
    }

    @Test
    @DisplayName("PUT /profile/password with short new password → 400 size message")
    void changePassword_shortNew_returns400() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("password123", "abc");

        mockMvc.perform(put("/api/users/profile/password")
                        .header("Authorization", "Bearer " + aliceJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value(org.hamcrest.Matchers.containsString(
                                "New password must be between 6 and 72 characters")));
    }

    @Test
    @DisplayName("PUT /profile/password with same as old → 400")
    void changePassword_sameAsOld_returns400() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("password123", "password123");

        mockMvc.perform(put("/api/users/profile/password")
                        .header("Authorization", "Bearer " + aliceJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("New password cannot be the same as the old password."));
    }

    // =================================================================
    // Roadmap preferences
    // =================================================================

    @Test
    @DisplayName("PUT /roadmap-preferences updates all fields")
    void updateRoadmapPreferences_updatesAll() throws Exception {
        UpdateRoadmapPreferencesRequest request = new UpdateRoadmapPreferencesRequest(
                "ADVANCED", "fr", "VISUAL", 20, true);

        mockMvc.perform(put("/api/users/roadmap-preferences")
                        .header("Authorization", "Bearer " + aliceJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.difficulty").value("ADVANCED"))
                .andExpect(jsonPath("$.languagePreference").value("fr"))
                .andExpect(jsonPath("$.learningStyle").value("VISUAL"))
                .andExpect(jsonPath("$.hoursPerWeek").value(20))
                .andExpect(jsonPath("$.avoidWeekends").value(true));
    }

    @Test
    @DisplayName("PUT /roadmap-preferences invalid difficulty → 400 pattern message")
    void updateRoadmapPreferences_invalidDifficulty_returns400() throws Exception {
        UpdateRoadmapPreferencesRequest request = new UpdateRoadmapPreferencesRequest(
                "MASTER", "en", "READING", 10, false);

        mockMvc.perform(put("/api/users/roadmap-preferences")
                        .header("Authorization", "Bearer " + aliceJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value(org.hamcrest.Matchers.containsString(
                                "Difficulty must be BEGINNER, INTERMEDIATE, or ADVANCED")));
    }

    @Test
    @DisplayName("PUT /roadmap-preferences hoursPerWeek = 0 → 400 Min violation")
    void updateRoadmapPreferences_hoursZero_returns400() throws Exception {
        UpdateRoadmapPreferencesRequest request = new UpdateRoadmapPreferencesRequest(
                "BEGINNER", "en", "READING", 0, false);

        mockMvc.perform(put("/api/users/roadmap-preferences")
                        .header("Authorization", "Bearer " + aliceJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value(org.hamcrest.Matchers.containsString(
                                "Hours per week must be at least 1")));
    }

    // =================================================================
    // User preferences (lifestyle)
    // =================================================================

    @Test
    @DisplayName("PUT /preferences updates lifestyle fields")
    void updateUserPreferences_updatesLifestyle() throws Exception {
        UpdateUserPreferencesRequest request = new UpdateUserPreferencesRequest(
                "{\"monday\":[[\"09:00\",\"12:00\"]]}",
                "Europe/Paris",
                "EVENING",
                LocalTime.of(6, 30),
                LocalTime.of(23, 30),
                LocalTime.of(13, 0),
                "[\"meditation\"]"
        );

        mockMvc.perform(put("/api/users/preferences")
                        .header("Authorization", "Bearer " + aliceJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timezone").value("Europe/Paris"))
                .andExpect(jsonPath("$.energyPeak").value("EVENING"));
    }

    // =================================================================
    // API key
    // =================================================================

    @Test
    @DisplayName("PUT /api-key with valid key → 204, status flips to usingOwnKey=true")
    void updateApiKey_setsOwnKey() throws Exception {
        ApiKeyRequest request = new ApiKeyRequest("my-fake-gemini-key-1234");

        mockMvc.perform(put("/api/users/api-key")
                        .header("Authorization", "Bearer " + aliceJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/api-key/status")
                        .header("Authorization", "Bearer " + aliceJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usingOwnKey").value(true));
    }

    @Test
    @DisplayName("PUT /api-key with blank key → 204, clears the stored key")
    void updateApiKey_blankClears() throws Exception {
        // First set a key
        mockMvc.perform(put("/api/users/api-key")
                        .header("Authorization", "Bearer " + aliceJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApiKeyRequest("fake-key"))))
                .andExpect(status().isNoContent());

        // Then clear it
        mockMvc.perform(put("/api/users/api-key")
                        .header("Authorization", "Bearer " + aliceJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApiKeyRequest(""))))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/api-key/status")
                        .header("Authorization", "Bearer " + aliceJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usingOwnKey").value(false));
    }

    // =================================================================
    // Delete account cascade
    // =================================================================

    @Test
    @DisplayName("DELETE /profile removes user + all cascade data")
    void deleteProfile_cascadesAllData() throws Exception {
        // Create data so cascade has something to remove
        mockMvc.perform(post("/api/journal")
                        .header("Authorization", "Bearer " + aliceJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rawText\":\"Delete cascade test\"}"))
                .andExpect(status().isCreated());

        // Confirm data exists
        assertThat(journalEntryRepository.count()).isEqualTo(1);
        assertThat(userRepository.count()).isEqualTo(1);

        // Delete account
        mockMvc.perform(delete("/api/users/profile")
                        .header("Authorization", "Bearer " + aliceJwt))
                .andExpect(status().isOk());

        // Everything gone
        assertThat(userRepository.count()).isEqualTo(0);
        assertThat(journalEntryRepository.count()).isEqualTo(0);
    }
}