package com.mymindmirror.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mymindmirror.backend.enums.Status;
import com.mymindmirror.backend.mapper.MilestoneMapper;
import com.mymindmirror.backend.model.Milestone;
import com.mymindmirror.backend.model.Task;
import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.payload.request.ImportGrowthTipRequest;
import com.mymindmirror.backend.payload.request.MilestoneRequest;
import com.mymindmirror.backend.payload.response.MilestoneInsightResponse;
import com.mymindmirror.backend.payload.response.MilestoneResponse;
import com.mymindmirror.backend.security.JwtUtil;
import com.mymindmirror.backend.security.services.UserDetailsImpl;
import com.mymindmirror.backend.service.MilestoneInsightService;
import com.mymindmirror.backend.service.MilestoneService;
import com.mymindmirror.backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

@WebMvcTest(MilestoneController.class)
class MilestoneControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MilestoneService milestoneService;

    @MockitoBean
    private MilestoneInsightService milestoneInsightService;

    @MockitoBean
    private UserService userService; // for @CurrentUser resolver

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private MilestoneMapper milestoneMapper;

    @MockitoBean
    private JwtUtil jwtUtil;

    private User currentUser;
    private UUID userId;
    private UUID milestoneId;
    private UUID taskId;
    private Milestone mockMilestone;
    private MilestoneResponse mockResponse;
    private Task mockTask;
    private MilestoneInsightResponse mockInsightResponse;

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

        mockMilestone = new Milestone();
        mockMilestone.setId(milestoneId);
        mockMilestone.setUser(currentUser);
        mockMilestone.setTitle("Test Milestone");
        mockMilestone.setDescription("Test Description");
        mockMilestone.setDueDate(LocalDate.now().plusDays(7));
        mockMilestone.setStatus(Status.PENDING);
        mockMilestone.setCreationDate(LocalDate.now());

        mockTask = new Task();
        mockTask.setId(taskId);
        mockTask.setDescription("Test Task");
        mockTask.setStatus(Status.PENDING);

        mockResponse = new MilestoneResponse(
                milestoneId,
                "Test Milestone",
                "Test Description",
                LocalDate.now(),
                LocalDate.now().plusDays(7),
                Status.PENDING,
                0.0,
                List.of()
        );

        mockInsightResponse = new MilestoneInsightResponse(
                "Remaining work",
                "Performance assessment",
                List.of("Tip 1", "Tip 2"),
                "Encouragement",
                List.of("New task 1"),
                com.mymindmirror.backend.enums.InsightStatus.SUCCESS
        );
    }

    // ===== CREATE MILESTONE =====

    @Test
    void createMilestone_shouldReturn201() throws Exception {
        MilestoneRequest request = new MilestoneRequest("Test Milestone", "Test Description", LocalDate.now().plusDays(7), Status.PENDING);
        when(milestoneService.createMilestoneAsDTO(any(User.class), anyString(), anyString(), any(LocalDate.class)))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/api/milestones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(milestoneId.toString()))
                .andExpect(jsonPath("$.title").value("Test Milestone"));
    }

    @Test
    void createMilestone_shouldReturn400_whenTitleMissing() throws Exception {
        MilestoneRequest request = new MilestoneRequest(null, "Description", null, null);
        mockMvc.perform(post("/api/milestones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ===== GET ALL MILESTONES =====

    @Test
    void getAllMilestones_shouldReturn200() throws Exception {
        List<MilestoneResponse> responses = List.of(mockResponse);
        when(milestoneService.getAllMilestonesForUserAsDTO(any(User.class))).thenReturn(responses);

        mockMvc.perform(get("/api/milestones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(milestoneId.toString()));
    }

    // ===== GET MILESTONE BY ID =====

    @Test
    void getMilestoneById_shouldReturn200() throws Exception {
        // Mock the actual method called by the controller
        when(milestoneService.getMilestoneByIdForUser(eq(milestoneId), any(User.class)))
                .thenReturn(Optional.of(mockMilestone));
        when(milestoneMapper.toResponse(any(Milestone.class))).thenReturn(mockResponse);

        mockMvc.perform(get("/api/milestones/{id}", milestoneId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(milestoneId.toString()));
    }


    @Test
    void getMilestoneById_shouldReturn404_whenNotFound() throws Exception {
        when(milestoneService.getMilestoneResponseById(eq(milestoneId), any(User.class)))
                .thenThrow(new IllegalArgumentException("Milestone not found"));

        mockMvc.perform(get("/api/milestones/{id}", milestoneId))
                .andExpect(status().isNotFound());
    }

    // ===== UPDATE MILESTONE =====

    @Test
    void updateMilestone_shouldReturn200() throws Exception {
        MilestoneRequest request = new MilestoneRequest("Updated Title", "Updated Description", LocalDate.now().plusDays(14), Status.IN_PROGRESS);
        when(milestoneService.updateMilestoneAsDTO(eq(milestoneId), any(User.class), anyString(), anyString(), any(LocalDate.class), any(Status.class)))
                .thenReturn(mockResponse);

        mockMvc.perform(put("/api/milestones/{id}", milestoneId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(milestoneId.toString()));
    }

    @Test
    void updateMilestone_shouldReturn400_whenTitleMissing() throws Exception {
        MilestoneRequest request = new MilestoneRequest(null, "Description", null, null);
        mockMvc.perform(put("/api/milestones/{id}", milestoneId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ===== DELETE MILESTONE =====

    @Test
    void deleteMilestone_shouldReturn200() throws Exception {
        doNothing().when(milestoneService).deleteMilestone(eq(milestoneId), any(User.class));

        mockMvc.perform(delete("/api/milestones/{id}", milestoneId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Milestone deleted successfully!"));
    }

    // ===== GET MILESTONE INSIGHTS =====

    @Test
    void getMilestoneInsights_shouldReturn200() throws Exception {
        when(milestoneService.getMilestoneByIdForUser(eq(milestoneId), any(User.class)))
                .thenReturn(Optional.of(mockMilestone));
        when(milestoneInsightService.getMilestoneInsights(any(Milestone.class))).thenReturn(mockInsightResponse);

        mockMvc.perform(get("/api/milestones/{id}/insights", milestoneId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingWork").value("Remaining work"));
    }

    @Test
    void getMilestoneInsights_shouldReturn404_whenMilestoneNotFound() throws Exception {
        when(milestoneService.getMilestoneByIdForUser(eq(milestoneId), any(User.class)))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/milestones/{id}/insights", milestoneId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMilestoneInsights_shouldReturn500_whenServiceThrowsException() throws Exception {
        when(milestoneService.getMilestoneByIdForUser(eq(milestoneId), any(User.class)))
                .thenReturn(Optional.of(mockMilestone));
        when(milestoneInsightService.getMilestoneInsights(any(Milestone.class)))
                .thenThrow(new RuntimeException("AI service unavailable"));

        mockMvc.perform(get("/api/milestones/{id}/insights", milestoneId))
                .andExpect(status().isInternalServerError());
    }

    // ===== IMPORT GROWTH TIP =====

    @Test
    void importGrowthTip_shouldReturn200() throws Exception {
        ImportGrowthTipRequest request = new ImportGrowthTipRequest("Test growth tip text");
        List<Task> tasks = List.of(mockTask);
        when(milestoneService.importGrowthTipAsTask(any(User.class), anyString())).thenReturn(tasks);

        mockMvc.perform(post("/api/milestones/import-growth-tip")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("1 tasks added to Milestones"));
    }

    @Test
    void importGrowthTip_shouldReturn400_whenTipTextMissing() throws Exception {
        ImportGrowthTipRequest request = new ImportGrowthTipRequest(null);
        mockMvc.perform(post("/api/milestones/import-growth-tip")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMilestoneInsights_shouldReturn503_whenDatabaseUnavailable() throws Exception {
        when(milestoneService.getMilestoneByIdForUser(eq(milestoneId), any(User.class)))
                .thenThrow(new org.springframework.dao.DataAccessResourceFailureException("DB down"));

        mockMvc.perform(get("/api/milestones/{id}/insights", milestoneId))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    @DisplayName("getMilestoneInsights → 200 with fallback when AI service fails")
    void getMilestoneInsights_returnsFallbackWhenAiFails() throws Exception {
        when(milestoneService.getMilestoneByIdForUser(eq(milestoneId), any(User.class)))
                .thenReturn(Optional.of(mockMilestone));
        when(milestoneInsightService.getMilestoneInsights(any(Milestone.class)))
                .thenReturn(MilestoneInsightResponse.createFallback());

        mockMvc.perform(get("/api/milestones/{id}/insights", milestoneId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ERROR"));
    }
}