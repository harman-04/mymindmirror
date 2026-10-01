package com.mymindmirror.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.payload.request.CustomTaskRequest;
import com.mymindmirror.backend.payload.response.CustomTaskResponse;
import com.mymindmirror.backend.security.JwtUtil;
import com.mymindmirror.backend.security.services.UserDetailsImpl;
import com.mymindmirror.backend.service.CustomTaskService;
import com.mymindmirror.backend.service.UserService;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CustomTaskController.class)
class CustomTaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CustomTaskService customTaskService;

    @MockitoBean
    private UserService userService; // for @CurrentUser resolver

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private JwtUtil jwtUtil;

    private User currentUser;
    private UUID userId;
    private UUID taskId;
    private CustomTaskResponse mockTaskResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        taskId = UUID.randomUUID();

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

        mockTaskResponse = new CustomTaskResponse(
                taskId,
                "Test Custom Task",
                "Description",
                LocalDate.now().plusDays(3),
                2.0,
                "MEDIUM",
                false,
                LocalDate.now()
        );
    }

    // ===== GET ALL CUSTOM TASKS =====

    @Test
    void getUserTasks_shouldReturn200() throws Exception {
        List<CustomTaskResponse> tasks = List.of(mockTaskResponse);
        when(customTaskService.getUserTasks(any(User.class))).thenReturn(tasks);

        mockMvc.perform(get("/api/custom-tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(taskId.toString()))
                .andExpect(jsonPath("$[0].title").value("Test Custom Task"));
    }

    // ===== CREATE CUSTOM TASK =====

    @Test
    void createCustomTask_shouldReturn201() throws Exception {
        CustomTaskRequest request = new CustomTaskRequest(
                "New Task",
                "Description",
                LocalDate.now().plusDays(5),
                1.5,
                "HIGH",
                false
        );
        when(customTaskService.createCustomTask(any(User.class), any(CustomTaskRequest.class)))
                .thenReturn(mockTaskResponse);

        mockMvc.perform(post("/api/custom-tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(taskId.toString()));
    }

    @Test
    void createCustomTask_shouldReturn400_whenTitleMissing() throws Exception {
        CustomTaskRequest request = new CustomTaskRequest(
                null,
                "Description",
                LocalDate.now().plusDays(5),
                1.5,
                "HIGH",
                false
        );
        mockMvc.perform(post("/api/custom-tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCustomTask_shouldReturn400_whenEstimatedHoursMissing() throws Exception {
        CustomTaskRequest request = new CustomTaskRequest(
                "Task",
                "Description",
                LocalDate.now().plusDays(5),
                null, // missing estimatedHours
                "HIGH",
                false
        );
        mockMvc.perform(post("/api/custom-tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ===== UPDATE CUSTOM TASK =====

    @Test
    void updateCustomTask_shouldReturn200() throws Exception {
        CustomTaskRequest request = new CustomTaskRequest(
                "Updated Task",
                "Updated Description",
                LocalDate.now().plusDays(7),
                2.5,
                "LOW",
                true
        );
        when(customTaskService.updateCustomTask(eq(taskId), any(User.class), any(CustomTaskRequest.class)))
                .thenReturn(mockTaskResponse);

        mockMvc.perform(put("/api/custom-tasks/{id}", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId.toString()));
    }

    @Test
    void updateCustomTask_shouldReturn400_whenTitleMissing() throws Exception {
        CustomTaskRequest request = new CustomTaskRequest(
                null,
                "Updated Description",
                LocalDate.now().plusDays(7),
                2.5,
                "LOW",
                true
        );
        mockMvc.perform(put("/api/custom-tasks/{id}", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ===== DELETE CUSTOM TASK =====

    @Test
    void deleteCustomTask_shouldReturn204() throws Exception {
        doNothing().when(customTaskService).deleteCustomTask(eq(taskId), any(User.class));

        mockMvc.perform(delete("/api/custom-tasks/{id}", taskId))
                .andExpect(status().isNoContent());
    }
}