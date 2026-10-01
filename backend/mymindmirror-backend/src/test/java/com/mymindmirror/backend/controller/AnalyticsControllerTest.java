package com.mymindmirror.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.payload.response.Anomaly;
import com.mymindmirror.backend.payload.response.AnomalyDetectionResponse;
import com.mymindmirror.backend.payload.response.DailyAggregatedDataResponse;
import com.mymindmirror.backend.security.JwtUtil;
import com.mymindmirror.backend.security.services.UserDetailsImpl;
import com.mymindmirror.backend.service.JournalService;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AnalyticsController.class)
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JournalService journalService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private JwtUtil jwtUtil;

    private User currentUser;
    private UUID userId;
    private List<DailyAggregatedDataResponse> mockDailyData;
    private AnomalyDetectionResponse mockAnomalyResult;

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

        mockDailyData = List.of(
                new DailyAggregatedDataResponse(LocalDate.now().minusDays(2), 0.8, 120L),
                new DailyAggregatedDataResponse(LocalDate.now().minusDays(1), 0.5, 80L),
                new DailyAggregatedDataResponse(LocalDate.now(), 0.9, 150L)
        );

        mockAnomalyResult = new AnomalyDetectionResponse(
                List.of(), "No significant anomalies detected.");
    }

    // ===== DAILY AGGREGATED DATA (unchanged) =====

    @Test
    void getDailyAggregatedData_shouldReturn200_withDefaultDates() throws Exception {
        when(journalService.getDailyAggregatedDataForUser(any(User.class), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(mockDailyData);

        mockMvc.perform(get("/api/ml/daily-aggregated-data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].averageMood").value(0.8));
    }

    @Test
    void getDailyAggregatedData_shouldReturn200_withCustomDates() throws Exception {
        String startDate = LocalDate.now().minusDays(5).toString();
        String endDate = LocalDate.now().toString();

        when(journalService.getDailyAggregatedDataForUser(any(User.class), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(mockDailyData);

        mockMvc.perform(get("/api/ml/daily-aggregated-data")
                        .param("startDate", startDate)
                        .param("endDate", endDate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void getDailyAggregatedData_shouldReturn400_whenInvalidDateFormat() throws Exception {
        mockMvc.perform(get("/api/ml/daily-aggregated-data")
                        .param("startDate", "invalid-date"))
                .andExpect(status().isBadRequest());
    }

    // ===== ANOMALY DETECTION =====

    @Test
    void runAnomalyDetection_shouldReturn200_whenNoAnomalies() throws Exception {
        when(journalService.runAnomalyDetection(anyList())).thenReturn(mockAnomalyResult);

        String body = objectMapper.writeValueAsString(Map.of("data", mockDailyData));

        mockMvc.perform(post("/api/ml/anomaly-detection")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("No significant anomalies detected."))
                .andExpect(jsonPath("$.anomalies").isArray())
                .andExpect(jsonPath("$.anomalies").isEmpty());
    }

    @Test
    void runAnomalyDetection_shouldReturn200_withAnomalies() throws Exception {
        Anomaly anomaly = new Anomaly(
                "2026-01-15", List.of("mood"), "Your average mood dropped significantly.");
        AnomalyDetectionResponse responseWithAnomalies = new AnomalyDetectionResponse(
                List.of(anomaly), "Detected 1 unusual journaling patterns.");
        when(journalService.runAnomalyDetection(anyList())).thenReturn(responseWithAnomalies);

        String body = objectMapper.writeValueAsString(Map.of("data", mockDailyData));

        mockMvc.perform(post("/api/ml/anomaly-detection")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Detected 1 unusual journaling patterns."))
                .andExpect(jsonPath("$.anomalies.length()").value(1))
                .andExpect(jsonPath("$.anomalies[0].type[0]").value("mood"))
                .andExpect(jsonPath("$.anomalies[0].date").value("2026-01-15"));
    }

    @Test
    void runAnomalyDetection_shouldReturn400_whenDataArrayEmpty() throws Exception {
        mockMvc.perform(post("/api/ml/anomaly-detection")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"data\": []}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void runAnomalyDetection_shouldReturn400_whenDataFieldMissing() throws Exception {
        mockMvc.perform(post("/api/ml/anomaly-detection")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void runAnomalyDetection_shouldReturn400_whenDataFieldNull() throws Exception {
        mockMvc.perform(post("/api/ml/anomaly-detection")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"data\": null}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void runAnomalyDetection_shouldReturn400_whenBodyIsLiteralNull() throws Exception {
        mockMvc.perform(post("/api/ml/anomaly-detection")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("null"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void runAnomalyDetection_shouldReturn500_whenServiceThrowsException() throws Exception {
        when(journalService.runAnomalyDetection(anyList()))
                .thenThrow(new RuntimeException("AI service error"));

        String body = objectMapper.writeValueAsString(Map.of("data", mockDailyData));

        mockMvc.perform(post("/api/ml/anomaly-detection")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isInternalServerError());
    }
}