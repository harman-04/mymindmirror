package com.mymindmirror.backend.controller;

import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.payload.response.UserStatsResponse;
import com.mymindmirror.backend.security.JwtUtil;
import com.mymindmirror.backend.security.services.UserDetailsImpl;
import com.mymindmirror.backend.service.GamificationService;
import com.mymindmirror.backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GamificationController.class)
class GamificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GamificationService gamificationService;

    @MockitoBean
    private UserService userService; // for @CurrentUser resolver

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private JwtUtil jwtUtil;

    private User currentUser;
    private UUID userId;
    private UserStatsResponse mockStatsResponse;

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

        mockStatsResponse = new UserStatsResponse(
                5,                          // currentStreak
                10,                         // longestStreak
                LocalDate.now(),            // lastActiveDate
                Set.of("FIRST_STEP", "THREE_DAY_STREAK"), // badges
                20,                         // totalTasksCompleted
                1500,                       // experiencePoints
                4,                          // level
                10,                         // totalJournalEntries
                5,                          // totalChats
                3                           // schedulesGenerated
        );
    }

    // ===== GET STATS =====

    @Test
    void getUserStats_shouldReturn200() throws Exception {
        when(gamificationService.getUserStats(any(User.class))).thenReturn(mockStatsResponse);

        mockMvc.perform(get("/api/gamification/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStreak").value(5))
                .andExpect(jsonPath("$.longestStreak").value(10))
                .andExpect(jsonPath("$.experiencePoints").value(1500))
                .andExpect(jsonPath("$.level").value(4))
                .andExpect(jsonPath("$.totalJournalEntries").value(10))
                .andExpect(jsonPath("$.totalChats").value(5))
                .andExpect(jsonPath("$.schedulesGenerated").value(3))

                //  Check the length, then check that the array CONTAINS the values regardless of order
                .andExpect(jsonPath("$.badges.length()").value(2))
                .andExpect(jsonPath("$.badges[?(@ == 'FIRST_STEP')]").exists())
                .andExpect(jsonPath("$.badges[?(@ == 'THREE_DAY_STREAK')]").exists());
    }

    @Test
    void getUserStats_shouldReturn500_whenServiceThrowsException() throws Exception {
        when(gamificationService.getUserStats(any(User.class)))
                .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/api/gamification/stats"))
                .andExpect(status().isInternalServerError());
    }
}