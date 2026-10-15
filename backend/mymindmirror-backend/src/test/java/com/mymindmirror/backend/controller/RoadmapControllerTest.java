package com.mymindmirror.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.payload.request.ImportTaskRequest;
import com.mymindmirror.backend.payload.request.RoadmapGenerateRequest;
import com.mymindmirror.backend.payload.response.ElaborationResponseDto;
import com.mymindmirror.backend.payload.response.RoadmapResponse;
import com.mymindmirror.backend.security.JwtUtil;
import com.mymindmirror.backend.security.services.UserDetailsImpl;
import com.mymindmirror.backend.service.RoadmapService;
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

@WebMvcTest(RoadmapController.class)
class RoadmapControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RoadmapService roadmapService;

    @MockitoBean
    private UserService userService; // for @CurrentUser resolver

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private JwtUtil jwtUtil; // for JwtRequestFilter

    private User currentUser;
    private UUID userId;
    private UUID roadmapId;
    private UUID taskId;
    private RoadmapResponse mockResponse;
    private ElaborationResponseDto mockElaborationResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        roadmapId = UUID.randomUUID();
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

        // RoadmapResponse mock
        RoadmapResponse.TaskDto taskDto = new RoadmapResponse.TaskDto(
                taskId, "Test Task", 1, 1, false, "daily", "details", List.of("subtask"), false
        );
        mockResponse = new RoadmapResponse(
                roadmapId,
                "Test Roadmap",
                "Test Description",
                LocalDate.now(),
                "ACTIVE",
                4,
                List.of(taskDto),
                List.of(),
                List.of(),
                4,
                4,
                "WEEKS"
        );

        // ElaborationResponseDto mock
        mockElaborationResponse = new ElaborationResponseDto(
                taskId,
                "Detailed instructions",
                List.of("sub1", "sub2")
        );
    }

    // ===== GENERATE ROADMAP =====

    @Test
    void generateRoadmap_shouldReturn200() throws Exception {
        RoadmapGenerateRequest request = new RoadmapGenerateRequest(
                "Learn Spring Boot", 4, 4, "WEEKS"
        );
        when(roadmapService.generateRoadmapAsDTO(any(User.class), anyString(), any(), any(), anyString()))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/api/roadmap/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(roadmapId.toString()))
                .andExpect(jsonPath("$.title").value("Test Roadmap"));
    }

    @Test
    void generateRoadmap_shouldReturn400_whenGoalMissing() throws Exception {
        RoadmapGenerateRequest request = new RoadmapGenerateRequest(null, 4, 4, "WEEKS");
        mockMvc.perform(post("/api/roadmap/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ===== GET ALL ROADMAPS =====

    @Test
    void getRoadmaps_shouldReturn200() throws Exception {
        List<RoadmapResponse> responses = List.of(mockResponse);
        when(roadmapService.getUserRoadmaps(any(User.class))).thenReturn(responses);

        mockMvc.perform(get("/api/roadmap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(roadmapId.toString()));
    }

    // ===== DELETE ROADMAP =====

    @Test
    void deleteRoadmap_shouldReturn200() throws Exception {
        doNothing().when(roadmapService).deleteRoadmap(eq(roadmapId), any(User.class));

        mockMvc.perform(delete("/api/roadmap/{id}", roadmapId))
                .andExpect(status().isOk());
    }

    // ===== CONTINUE ROADMAP =====

    @Test
    void continueRoadmap_shouldReturn200() throws Exception {
        when(roadmapService.continueRoadmap(eq(roadmapId), any(User.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/roadmap/{id}/continue", roadmapId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(roadmapId.toString()));
    }

    // ===== CONTINUE BATCH =====

    @Test
    void continueRoadmapBatch_shouldReturn200() throws Exception {
        when(roadmapService.continueRoadmapBatch(eq(roadmapId), any(User.class), anyInt()))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/api/roadmap/{id}/continue-batch", roadmapId)
                        .param("weeksToGenerate", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(roadmapId.toString()));
    }

    @Test
    void continueRoadmapBatch_shouldUseDefaultWeeks_whenParamOmitted() throws Exception {
        when(roadmapService.continueRoadmapBatch(eq(roadmapId), any(User.class), isNull()))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/api/roadmap/{id}/continue-batch", roadmapId))
                .andExpect(status().isOk());
    }

    // ===== ELABORATE TASK =====

    @Test
    void elaborateTask_shouldReturn200() throws Exception {
        when(roadmapService.elaborateTask(eq(taskId), any(User.class), eq(false)))
                .thenReturn(mockElaborationResponse);

        mockMvc.perform(post("/api/roadmap/task/{taskId}/elaborate", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId.toString()))
                .andExpect(jsonPath("$.details").value("Detailed instructions"))
                .andExpect(jsonPath("$.subtasks.length()").value(2));
    }

    @Test
    void elaborateTask_shouldReturn200_withEnhance() throws Exception {
        when(roadmapService.elaborateTask(eq(taskId), any(User.class), eq(true)))
                .thenReturn(mockElaborationResponse);

        mockMvc.perform(post("/api/roadmap/task/{taskId}/elaborate", taskId)
                        .param("enhance", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId.toString()));
    }

    // ===== RESCHEDULE =====

    @Test
    void rescheduleRoadmap_shouldReturn200() throws Exception {
        when(roadmapService.rescheduleRoadmap(eq(roadmapId), any(User.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/roadmap/{id}/reschedule", roadmapId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(roadmapId.toString()));
    }

    // ===== IMPORT TASK TO MILESTONE =====

    @Test
    void importTaskToMilestone_shouldReturn200() throws Exception {
        doNothing().when(roadmapService).importTaskToMilestone(eq(roadmapId), eq(taskId), any(User.class));

        ImportTaskRequest request = new ImportTaskRequest(roadmapId, taskId);
        mockMvc.perform(post("/api/roadmap/import-task")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Task imported successfully"));
    }

    // ===== TOGGLE TASK COMPLETION =====

    @Test
    void toggleTaskCompletion_shouldReturn200() throws Exception {
        doNothing().when(roadmapService).toggleTaskCompletion(eq(taskId), any(User.class));

        mockMvc.perform(patch("/api/roadmap/task/{taskId}/toggle", taskId))
                .andExpect(status().isOk());
    }

    @Test
    void importTaskToMilestone_shouldReturn400_whenRoadmapIdNull() throws Exception {
        // raw JSON — can't use ImportTaskRequest since record allows nulls
        String body = "{\"taskId\":\"" + taskId + "\"}";

        mockMvc.perform(post("/api/roadmap/import-task")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(roadmapService);
    }

    @Test
    void importTaskToMilestone_shouldReturn400_whenTaskIdNull() throws Exception {
        String body = "{\"roadmapId\":\"" + roadmapId + "\"}";

        mockMvc.perform(post("/api/roadmap/import-task")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(roadmapService);
    }
}