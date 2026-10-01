package com.mymindmirror.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.payload.request.MoveScheduledTaskRequest;
import com.mymindmirror.backend.payload.request.ScheduleCustomTaskRequest;
import com.mymindmirror.backend.payload.response.ScheduledTaskResponse;
import com.mymindmirror.backend.security.JwtUtil;
import com.mymindmirror.backend.security.services.UserDetailsImpl;
import com.mymindmirror.backend.service.ScheduleService;
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
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ScheduleController.class)
class ScheduleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ScheduleService scheduleService;

    @MockitoBean
    private UserService userService; // for @CurrentUser resolver

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private JwtUtil jwtUtil; // for JwtRequestFilter

    private User currentUser;
    private UUID userId;
    private UUID taskId;
    private UUID customTaskId;
    private ScheduledTaskResponse mockTaskResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        taskId = UUID.randomUUID();
        customTaskId = UUID.randomUUID();

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

        mockTaskResponse = new ScheduledTaskResponse(
                taskId,
                "Test Task",
                LocalDate.now(),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                false,
                null,
                null,
                null,
                "MEDIUM",
                "WORK_TASK"
        );
    }

    // ===== GENERATE SCHEDULE =====

    @Test
    void generateSchedule_shouldReturn200() throws Exception {
        doNothing().when(scheduleService).generateSchedule(any(User.class), anyString());

        mockMvc.perform(post("/api/schedule/generate")
                        .param("mode", "all"))
                .andExpect(status().isOk())
                .andExpect(content().string("Schedule generated"));
    }

    @Test
    void generateSchedule_shouldUseDefaultMode_whenParamOmitted() throws Exception {
        doNothing().when(scheduleService).generateSchedule(any(User.class), eq("all"));

        mockMvc.perform(post("/api/schedule/generate"))
                .andExpect(status().isOk())
                .andExpect(content().string("Schedule generated"));
    }

    // ===== REOPTIMIZE TODAY =====

    @Test
    void reoptimizeToday_shouldReturn200() throws Exception {
        doNothing().when(scheduleService).reoptimizeToday(any(User.class));

        mockMvc.perform(post("/api/schedule/reoptimize"))
                .andExpect(status().isOk())
                .andExpect(content().string("Today re-optimized"));
    }

    // ===== GET SCHEDULED TASKS =====

    @Test
    void getScheduledTasks_shouldReturn200() throws Exception {
        List<ScheduledTaskResponse> tasks = List.of(mockTaskResponse);
        when(scheduleService.getScheduledTasks(any(User.class), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(tasks);

        // Use valid date strings
        String startDate = LocalDate.now().minusDays(1).toString();
        String endDate = LocalDate.now().plusDays(1).toString();

        mockMvc.perform(get("/api/schedule/tasks")
                        .param("startDate", startDate)
                        .param("endDate", endDate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(taskId.toString()))
                .andExpect(jsonPath("$[0].title").value("Test Task"));
    }

    @Test
    void getScheduledTasks_shouldReturn400_whenStartDateInvalid() throws Exception {
        mockMvc.perform(get("/api/schedule/tasks")
                        .param("startDate", "invalid-date")
                        .param("endDate", LocalDate.now().toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getScheduledTasks_shouldReturn400_whenEndDateInvalid() throws Exception {
        mockMvc.perform(get("/api/schedule/tasks")
                        .param("startDate", LocalDate.now().toString())
                        .param("endDate", "invalid-date"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getScheduledTasks_shouldReturn400_whenStartDateMissing() throws Exception {
        mockMvc.perform(get("/api/schedule/tasks")
                        .param("endDate", LocalDate.now().toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getScheduledTasks_shouldReturn400_whenEndDateMissing() throws Exception {
        mockMvc.perform(get("/api/schedule/tasks")
                        .param("startDate", LocalDate.now().toString()))
                .andExpect(status().isBadRequest());
    }

    // ===== MOVE SCHEDULED TASK =====

    @Test
    void moveScheduledTask_shouldReturn200() throws Exception {
        doNothing().when(scheduleService).moveScheduledTask(
                eq(taskId), any(User.class), any(LocalDate.class), any(LocalTime.class), any(LocalTime.class));

        MoveScheduledTaskRequest request = new MoveScheduledTaskRequest(
                LocalDate.now().plusDays(1),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0)
        );

        mockMvc.perform(put("/api/schedule/task/{taskId}/move", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void moveScheduledTask_shouldReturn400_whenRequestInvalid() throws Exception {
        // Missing date (null)
        MoveScheduledTaskRequest request = new MoveScheduledTaskRequest(
                null,
                LocalTime.of(10, 0),
                LocalTime.of(11, 0)
        );

        mockMvc.perform(put("/api/schedule/task/{taskId}/move", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ===== COMPLETE TASK =====

    @Test
    void completeTask_shouldReturn200() throws Exception {
        doNothing().when(scheduleService).completeTask(eq(taskId), any(User.class));

        mockMvc.perform(patch("/api/schedule/task/{taskId}/complete", taskId))
                .andExpect(status().isOk());
    }

    // ===== SCHEDULE CUSTOM TASK MANUALLY =====

    @Test
    void scheduleCustomTaskManually_shouldReturn200() throws Exception {
        doNothing().when(scheduleService).scheduleCustomTaskManually(
                any(User.class), eq(customTaskId), anyString(), any(LocalDate.class),
                any(LocalTime.class), any(LocalTime.class), anyString());

        ScheduleCustomTaskRequest request = new ScheduleCustomTaskRequest(
                customTaskId,
                "Custom Task",
                LocalDate.now().plusDays(1),
                LocalTime.of(14, 0),
                LocalTime.of(15, 0),
                "HIGH"
        );

        mockMvc.perform(post("/api/schedule/task/custom")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void scheduleCustomTaskManually_shouldReturn400_whenRequestInvalid() throws Exception {
        // Missing title
        ScheduleCustomTaskRequest request = new ScheduleCustomTaskRequest(
                customTaskId,
                null,
                LocalDate.now().plusDays(1),
                LocalTime.of(14, 0),
                LocalTime.of(15, 0),
                "HIGH"
        );

        mockMvc.perform(post("/api/schedule/task/custom")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}