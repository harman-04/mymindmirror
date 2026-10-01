package com.mymindmirror.backend.controller;

import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.security.JwtUtil;
import com.mymindmirror.backend.security.services.UserDetailsImpl;
import com.mymindmirror.backend.service.JournalService;
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

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReflectionController.class)
class ReflectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JournalService journalService;

    @MockitoBean
    private UserService userService; // for @CurrentUser resolver

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private JwtUtil jwtUtil;

    private User currentUser;
    private UUID userId;

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
    }

    // ===== GENERATE DAILY REFLECTION =====

    @Test
    void generateDailyReflection_shouldReturn200() throws Exception {
        when(journalService.generateDailyReflection(any(User.class)))
                .thenReturn("You have grown immensely this week. Keep going!");

        mockMvc.perform(post("/api/reflection/generate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reflection").value("You have grown immensely this week. Keep going!"));
    }

    @Test
    void generateDailyReflection_shouldReturn200_whenNoEntries() throws Exception {
        when(journalService.generateDailyReflection(any(User.class)))
                .thenReturn("Journal an entry today to get your daily reflection!");

        mockMvc.perform(post("/api/reflection/generate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reflection").value("Journal an entry today to get your daily reflection!"));
    }

    @Test
    void generateDailyReflection_shouldReturn500_whenServiceThrowsException() throws Exception {
        when(journalService.generateDailyReflection(any(User.class)))
                .thenThrow(new RuntimeException("AI service error"));

        mockMvc.perform(post("/api/reflection/generate"))
                .andExpect(status().isInternalServerError());
    }
}