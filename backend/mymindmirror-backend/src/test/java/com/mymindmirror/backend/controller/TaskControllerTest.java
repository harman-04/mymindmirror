package com.mymindmirror.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mymindmirror.backend.enums.Status;
import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.payload.request.TaskRequest;
import com.mymindmirror.backend.payload.response.TaskResponse;
import com.mymindmirror.backend.security.JwtUtil;
import com.mymindmirror.backend.security.services.UserDetailsImpl;
import com.mymindmirror.backend.service.TaskService;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TaskService taskService;

    @MockitoBean
    private UserService userService; // for @CurrentUser resolver

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private JwtUtil jwtUtil;

    private User currentUser;
    private UUID userId;
    private UUID milestoneId;
    private UUID taskId;
    private TaskResponse mockTaskResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        milestoneId = UUID.randomUUID();
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

        mockTaskResponse = new TaskResponse(
                taskId,
                "Test Task",
                LocalDateTime.now(),
                LocalDate.now().plusDays(3),
                Status.PENDING,
                "Details",
                List.of("sub1", "sub2"),
                null
        );
    }

    // ===== CREATE TASK =====

    @Test
    void createTask_shouldReturn201() throws Exception {
        TaskRequest request = new TaskRequest(
                "Test Task",
                LocalDate.now().plusDays(3),
                Status.PENDING,
                "Details",
                List.of("sub1", "sub2")
        );
        when(taskService.createTask(eq(milestoneId), any(User.class), any(TaskRequest.class)))
                .thenReturn(mockTaskResponse);

        mockMvc.perform(post("/api/milestones/{milestoneId}/tasks", milestoneId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(taskId.toString()))
                .andExpect(jsonPath("$.description").value("Test Task"));
    }

    @Test
    void createTask_shouldReturn400_whenDescriptionMissing() throws Exception {
        // Send a request with a null description
        TaskRequest request = new TaskRequest(null, null, null, null, null);

        mockMvc.perform(post("/api/milestones/{milestoneId}/tasks", milestoneId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ===== GET ALL TASKS FOR MILESTONE =====

    @Test
    void getAllTasksForMilestone_shouldReturn200() throws Exception {
        List<TaskResponse> tasks = List.of(mockTaskResponse);
        when(taskService.getAllTasksForMilestone(eq(milestoneId), any(User.class))).thenReturn(tasks);

        mockMvc.perform(get("/api/milestones/{milestoneId}/tasks", milestoneId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(taskId.toString()));
    }

    // ===== GET TASK BY ID =====

    @Test
    void getTaskById_shouldReturn200() throws Exception {
        when(taskService.getTaskByIdForMilestoneAndUser(eq(taskId), eq(milestoneId), any(User.class)))
                .thenReturn(Optional.of(mockTaskResponse));

        mockMvc.perform(get("/api/milestones/{milestoneId}/tasks/{taskId}", milestoneId, taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId.toString()));
    }

    @Test
    void getTaskById_shouldReturn404_whenNotFound() throws Exception {
        when(taskService.getTaskByIdForMilestoneAndUser(eq(taskId), eq(milestoneId), any(User.class)))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/milestones/{milestoneId}/tasks/{taskId}", milestoneId, taskId))
                .andExpect(status().isNotFound());
    }

    // ===== UPDATE TASK =====

    @Test
    void updateTask_shouldReturn200() throws Exception {
        TaskRequest request = new TaskRequest(
                "Updated Task",
                LocalDate.now().plusDays(5),
                Status.IN_PROGRESS,
                "Updated details",
                List.of("sub3")
        );
        when(taskService.updateTask(eq(taskId), eq(milestoneId), any(User.class), any(TaskRequest.class)))
                .thenReturn(mockTaskResponse);

        mockMvc.perform(put("/api/milestones/{milestoneId}/tasks/{taskId}", milestoneId, taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId.toString()));
    }

    // ===== DELETE TASK =====

    @Test
    void deleteTask_shouldReturn200() throws Exception {
        doNothing().when(taskService).deleteTask(eq(taskId), eq(milestoneId), any(User.class));

        mockMvc.perform(delete("/api/milestones/{milestoneId}/tasks/{taskId}", milestoneId, taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Task deleted successfully!"));
    }
}