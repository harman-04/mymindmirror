package com.mymindmirror.backend.service;

import com.mymindmirror.backend.enums.AITask;
import com.mymindmirror.backend.enums.GamificationAction;
import com.mymindmirror.backend.enums.InsightStatus;
import com.mymindmirror.backend.enums.Status;
import com.mymindmirror.backend.model.Milestone;
import com.mymindmirror.backend.model.Task;
import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.payload.response.MilestoneInsightResponse;
import com.mymindmirror.backend.service.ai.DynamicAiClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MilestoneInsightServiceTest {

    @Mock
    private DynamicAiClientService aiClientService;

    @Mock
    private GamificationService gamificationService;

    @InjectMocks
    private MilestoneInsightService milestoneInsightService;

    private User user;
    private UUID userId;
    private Milestone milestone;
    private Task task1;
    private Task task2;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        user.setUsername("testuser");

        milestone = new Milestone();
        milestone.setId(UUID.randomUUID());
        milestone.setTitle("Learn Spring Boot");
        milestone.setDescription("Complete the Spring Boot masterclass");
        milestone.setUser(user);
        milestone.setStatus(Status.IN_PROGRESS);
        milestone.setDueDate(LocalDate.now().plusDays(7));

        task1 = new Task();
        task1.setId(UUID.randomUUID());
        task1.setDescription("Setup project");
        task1.setDueDate(LocalDate.now().plusDays(2));
        task1.setStatus(Status.COMPLETED);

        task2 = new Task();
        task2.setId(UUID.randomUUID());
        task2.setDescription("Create REST endpoints");
        task2.setDueDate(LocalDate.now().plusDays(5));
        task2.setStatus(Status.PENDING);

        milestone.setTasks(List.of(task1, task2));
    }

    // ========== Success Path ==========

    @Test
    void getMilestoneInsights_shouldReturnInsightsAndAwardGamification() {
        // Given
        MilestoneInsightResponse expectedResponse = new MilestoneInsightResponse(
                "Remaining work: create REST endpoints",
                "Good progress, but some tasks pending",
                List.of("Focus on completing REST endpoints"),
                "Keep going!",
                List.of("Add security layer"),
                InsightStatus.SUCCESS
        );

        when(aiClientService.generateStructured(
                anyString(),
                eq(MilestoneInsightResponse.class),
                eq(userId),
                eq(AITask.MILESTONE_INSIGHTS)
        )).thenReturn(expectedResponse);

        // When
        MilestoneInsightResponse result = milestoneInsightService.getMilestoneInsights(milestone);

        // Then
        assertThat(result).isSameAs(expectedResponse);
        verify(gamificationService).recordActivity(user, GamificationAction.AI_INSIGHT);
        verify(aiClientService).generateStructured(anyString(), eq(MilestoneInsightResponse.class), eq(userId), eq(AITask.MILESTONE_INSIGHTS));
    }

    @Test
    void getMilestoneInsights_shouldBuildPromptWithCorrectDetails() {
        // Given
        MilestoneInsightResponse expectedResponse = new MilestoneInsightResponse(
                "", "", List.of(), "", List.of(), InsightStatus.SUCCESS
        );
        when(aiClientService.generateStructured(anyString(), eq(MilestoneInsightResponse.class), eq(userId), eq(AITask.MILESTONE_INSIGHTS)))
                .thenReturn(expectedResponse);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);

        // When
        milestoneInsightService.getMilestoneInsights(milestone);

        // Then
        verify(aiClientService).generateStructured(promptCaptor.capture(), eq(MilestoneInsightResponse.class), eq(userId), eq(AITask.MILESTONE_INSIGHTS));
        String prompt = promptCaptor.getValue();
        assertThat(prompt).contains("Learn Spring Boot");
        assertThat(prompt).contains("Complete the Spring Boot masterclass");
        assertThat(prompt).contains("Setup project");
        assertThat(prompt).contains("Create REST endpoints");
        assertThat(prompt).contains("IN_PROGRESS");
        assertThat(prompt).contains(milestone.getDueDate().toString());
    }

    // ========== AI Failure ==========

    @Test
    void getMilestoneInsights_shouldReturnFallback_whenAIFails() {
        // Given
        when(aiClientService.generateStructured(
                anyString(),
                eq(MilestoneInsightResponse.class),
                eq(userId),
                eq(AITask.MILESTONE_INSIGHTS)
        )).thenThrow(new RuntimeException("AI service unavailable"));

        // When
        MilestoneInsightResponse result = milestoneInsightService.getMilestoneInsights(milestone);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(InsightStatus.ERROR);
        assertThat(result.tips()).contains("Check your Gemini API key and quota.");
        verify(gamificationService, never()).recordActivity(any(), any());
    }

    // ========== Edge Cases ==========

    @Test
    void getMilestoneInsights_shouldHandleNullDueDate() {
        // Given
        milestone.setDueDate(null);
        MilestoneInsightResponse expectedResponse = new MilestoneInsightResponse(
                "", "", List.of(), "", List.of(), InsightStatus.SUCCESS
        );
        when(aiClientService.generateStructured(anyString(), eq(MilestoneInsightResponse.class), eq(userId), eq(AITask.MILESTONE_INSIGHTS)))
                .thenReturn(expectedResponse);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);

        // When
        milestoneInsightService.getMilestoneInsights(milestone);

        // Then
        verify(aiClientService).generateStructured(promptCaptor.capture(), eq(MilestoneInsightResponse.class), eq(userId), eq(AITask.MILESTONE_INSIGHTS));
        assertThat(promptCaptor.getValue()).contains("Due Date: Not set");
    }

    @Test
    void getMilestoneInsights_shouldHandleEmptyTasks() {
        // Given
        milestone.setTasks(List.of());
        MilestoneInsightResponse expectedResponse = new MilestoneInsightResponse(
                "", "", List.of(), "", List.of(), InsightStatus.SUCCESS
        );
        when(aiClientService.generateStructured(anyString(), eq(MilestoneInsightResponse.class), eq(userId), eq(AITask.MILESTONE_INSIGHTS)))
                .thenReturn(expectedResponse);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);

        // When
        milestoneInsightService.getMilestoneInsights(milestone);

        // Then
        verify(aiClientService).generateStructured(promptCaptor.capture(), eq(MilestoneInsightResponse.class), eq(userId), eq(AITask.MILESTONE_INSIGHTS));
        String prompt = promptCaptor.getValue();
        assertThat(prompt).contains("No specific tasks defined for this milestone.");
        // No tasks listed
        assertThat(prompt).doesNotContain("Setup project");
    }

    @Test
    void getMilestoneInsights_shouldHandleNullTaskStatusOrDueDate() {
        // Given
        Task taskWithNulls = new Task();
        taskWithNulls.setDescription("Task with nulls");
        taskWithNulls.setStatus(null);
        taskWithNulls.setDueDate(null);
        milestone.setTasks(List.of(taskWithNulls));

        MilestoneInsightResponse expectedResponse = new MilestoneInsightResponse(
                "", "", List.of(), "", List.of(), InsightStatus.SUCCESS
        );
        when(aiClientService.generateStructured(anyString(), eq(MilestoneInsightResponse.class), eq(userId), eq(AITask.MILESTONE_INSIGHTS)))
                .thenReturn(expectedResponse);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);

        // When
        milestoneInsightService.getMilestoneInsights(milestone);

        // Then
        verify(aiClientService).generateStructured(promptCaptor.capture(), eq(MilestoneInsightResponse.class), eq(userId), eq(AITask.MILESTONE_INSIGHTS));
        String prompt = promptCaptor.getValue();
        assertThat(prompt).contains("Task with nulls");
        assertThat(prompt).contains("Status: PENDING"); // default
        assertThat(prompt).contains("Due: No due date");
    }
}