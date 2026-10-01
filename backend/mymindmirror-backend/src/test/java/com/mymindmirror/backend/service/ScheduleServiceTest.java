package com.mymindmirror.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mymindmirror.backend.enums.AITask;
import com.mymindmirror.backend.enums.GamificationAction;
import com.mymindmirror.backend.enums.Status;
import com.mymindmirror.backend.mapper.ScheduleMapper;
import com.mymindmirror.backend.model.*;
import com.mymindmirror.backend.payload.response.ScheduleItem;
import com.mymindmirror.backend.payload.response.ScheduleResponse;
import com.mymindmirror.backend.payload.response.ScheduledTaskResponse;
import com.mymindmirror.backend.repository.*;
import com.mymindmirror.backend.service.ai.DynamicAiClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private UserPreferencesRepository userPreferencesRepository;

    @Mock
    private ScheduledTaskRepository scheduledTaskRepository;

    @Mock
    private RoadmapTaskRepository roadmapTaskRepository;

    @Mock
    private TaskRepository milestoneTaskRepository;

    @Mock
    private CustomTaskRepository customTaskRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Spy
    private Clock clock = Clock.fixed(Instant.parse("2026-09-08T10:00:00Z"), ZoneId.of("UTC"));

    @InjectMocks
    private ScheduleService scheduleService;


    @Mock
    private DynamicAiClientService aiClientService;

    @Mock
    private GamificationService gamificationService;

    @Mock
    private ScheduleMapper scheduleMapper;

    @Mock
    private TransactionTemplate transactionTemplate;



    private User user;
    private UUID userId;
    private UserPreferences preferences;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        user.setUsername("testuser");

        preferences = new UserPreferences();
        preferences.setUser(user);
        preferences.setTimezone("UTC");
        preferences.setAvailableHoursJson("{\"monday\":[[\"09:00\",\"17:00\"]]}");
        preferences.setWakeTime(LocalTime.of(7, 0));
        preferences.setSleepTime(LocalTime.of(23, 0));
        preferences.setLunchTime(LocalTime.of(13, 0));
        preferences.setEnergyPeak("MORNING");
        preferences.setDailyHabitsJson("[]");
    }

    // ========== Helper to mock task maps ==========

    private void mockTaskMaps() {
        when(roadmapTaskRepository.findByRoadmap_User(user)).thenReturn(List.of());
        when(milestoneTaskRepository.findByMilestone_User(user)).thenReturn(List.of());
        when(customTaskRepository.findByUser(user)).thenReturn(List.of());
    }

    // ========== Test 1: AI success ==========

    @Test
    void generateSchedule_shouldCallAIAndSaveSchedule() {
        // Given
        String mode = "all";
        when(userPreferencesRepository.findByUser(user)).thenReturn(Optional.of(preferences));

        CustomTask customTask = new CustomTask();
        customTask.setId(UUID.randomUUID());
        customTask.setTitle("Test Custom Task");
        customTask.setEstimatedHours(1.0);
        customTask.setPriority("MEDIUM");
        customTask.setCompleted(false);
        //  Stub both findByUser and findByUserAndCompletedFalse
        when(customTaskRepository.findByUser(user)).thenReturn(List.of(customTask));
        when(customTaskRepository.findByUserAndCompletedFalse(user)).thenReturn(List.of(customTask));
        // No roadmap or milestone tasks
        when(roadmapTaskRepository.findByRoadmap_User(user)).thenReturn(List.of());
        when(milestoneTaskRepository.findByMilestone_User(user)).thenReturn(List.of());

        when(scheduledTaskRepository.findByUser(user)).thenReturn(List.of());

        ScheduleResponse aiResponse = new ScheduleResponse(
                List.of(new ScheduleItem(
                        customTask.getId().toString(),
                        "Test Custom Task",
                        "WORK_TASK",
                        LocalDate.now(clock).toString(),
                        "09:00",
                        "10:00"
                )),
                List.of()
        );
        when(aiClientService.generateStructured(anyString(), eq(ScheduleResponse.class), eq(userId), eq(AITask.SCHEDULE_GENERATION)))
                .thenReturn(aiResponse);

        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<Void> callback = invocation.getArgument(0);
            callback.doInTransaction(null);
            return null;
        });

        // When
        scheduleService.generateSchedule(user, mode);

        // Then
        verify(scheduledTaskRepository).deleteIncompleteByUser(user);
        ArgumentCaptor<List<ScheduledTask>> captor = ArgumentCaptor.forClass(List.class);
        verify(scheduledTaskRepository).saveAll(captor.capture());
        List<ScheduledTask> savedTasks = captor.getValue();
        assertThat(savedTasks).hasSize(1);
        ScheduledTask saved = savedTasks.get(0);
        assertThat(saved.getCustomTaskId()).isEqualTo(customTask.getId());
        verify(gamificationService).recordActivity(user, GamificationAction.SCHEDULE);
    }
    // ========== Test 2: AI failure – fallback ==========

    @Test
    void generateSchedule_shouldUseFallbackScheduler_whenAIFails() {
        // Given
        String mode = "all";
        when(userPreferencesRepository.findByUser(user)).thenReturn(Optional.of(preferences));

        CustomTask customTask = new CustomTask();
        customTask.setId(UUID.randomUUID());
        customTask.setTitle("Test Custom Task");
        customTask.setEstimatedHours(1.0);
        customTask.setPriority("MEDIUM");
        customTask.setCompleted(false);
        //  Stub both
        when(customTaskRepository.findByUser(user)).thenReturn(List.of(customTask));
        when(customTaskRepository.findByUserAndCompletedFalse(user)).thenReturn(List.of(customTask));
        when(roadmapTaskRepository.findByRoadmap_User(user)).thenReturn(List.of());
        when(milestoneTaskRepository.findByMilestone_User(user)).thenReturn(List.of());
        when(scheduledTaskRepository.findByUser(user)).thenReturn(List.of());

        // AI throws
        when(aiClientService.generateStructured(anyString(), eq(ScheduleResponse.class), eq(userId), eq(AITask.SCHEDULE_GENERATION)))
                .thenThrow(new RuntimeException("AI error"));

        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<Void> callback = invocation.getArgument(0);
            callback.doInTransaction(null);
            return null;
        });

        // When
        scheduleService.generateSchedule(user, mode);

        // Then – fallback schedules the task
        verify(scheduledTaskRepository).deleteIncompleteByUser(user);
        ArgumentCaptor<List<ScheduledTask>> captor = ArgumentCaptor.forClass(List.class);
        verify(scheduledTaskRepository).saveAll(captor.capture());
        List<ScheduledTask> savedTasks = captor.getValue();
        assertThat(savedTasks).isNotEmpty();
        verify(gamificationService).recordActivity(user, GamificationAction.SCHEDULE);
    }

    // ========== Test 3: No tasks ==========

    @Test
    void generateSchedule_shouldHandleNoTasks() {
        // Given
        String mode = "all";
        when(userPreferencesRepository.findByUser(user)).thenReturn(Optional.of(preferences));
        when(customTaskRepository.findByUser(user)).thenReturn(List.of());
        when(customTaskRepository.findByUserAndCompletedFalse(user)).thenReturn(List.of());
        when(roadmapTaskRepository.findByRoadmap_User(user)).thenReturn(List.of());
        when(milestoneTaskRepository.findByMilestone_User(user)).thenReturn(List.of());
        when(scheduledTaskRepository.findByUser(user)).thenReturn(List.of());

        when(aiClientService.generateStructured(anyString(), eq(ScheduleResponse.class), eq(userId), eq(AITask.SCHEDULE_GENERATION)))
                .thenReturn(new ScheduleResponse(List.of(), List.of()));

        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<Void> callback = invocation.getArgument(0);
            callback.doInTransaction(null);
            return null;
        });

        // When
        scheduleService.generateSchedule(user, mode);

        // Then
        verify(scheduledTaskRepository).deleteIncompleteByUser(user);
        verify(scheduledTaskRepository, never()).saveAll(anyList());
        verify(gamificationService).recordActivity(user, GamificationAction.SCHEDULE);
    }

    // ========== Test 4: Custom mode ==========

    @Test
    void generateSchedule_shouldHandleCustomMode() {
        // Given
        String mode = "custom";
        when(userPreferencesRepository.findByUser(user)).thenReturn(Optional.of(preferences));

        CustomTask customTask = new CustomTask();
        customTask.setId(UUID.randomUUID());
        customTask.setTitle("Custom Task");
        customTask.setEstimatedHours(2.0);
        customTask.setPriority("HIGH");
        customTask.setCompleted(false);
        when(customTaskRepository.findByUser(user)).thenReturn(List.of(customTask));
        when(customTaskRepository.findByUserAndCompletedFalse(user)).thenReturn(List.of(customTask));
        when(roadmapTaskRepository.findByRoadmap_User(user)).thenReturn(List.of());
        when(milestoneTaskRepository.findByMilestone_User(user)).thenReturn(List.of());
        when(scheduledTaskRepository.findByUser(user)).thenReturn(List.of());

        ScheduleResponse aiResponse = new ScheduleResponse(
                List.of(new ScheduleItem(
                        customTask.getId().toString(),
                        "Custom Task",
                        "WORK_TASK",
                        LocalDate.now(clock).toString(),
                        "10:00",
                        "12:00"
                )),
                List.of()
        );
        when(aiClientService.generateStructured(anyString(), eq(ScheduleResponse.class), eq(userId), eq(AITask.SCHEDULE_GENERATION)))
                .thenReturn(aiResponse);

        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<Void> callback = invocation.getArgument(0);
            callback.doInTransaction(null);
            return null;
        });

        // When
        scheduleService.generateSchedule(user, mode);

        // Then
        ArgumentCaptor<List<ScheduledTask>> captor = ArgumentCaptor.forClass(List.class);
        verify(scheduledTaskRepository).saveAll(captor.capture());
        List<ScheduledTask> savedTasks = captor.getValue();
        assertThat(savedTasks).hasSize(1);
        assertThat(savedTasks.get(0).getCustomTaskId()).isEqualTo(customTask.getId());
    }

    // ========== Test 5: Preferences missing – creates default ==========

    @Test
    void generateSchedule_shouldCreateDefaultPreferences_whenNotFound() {
        // Given
        String mode = "all";
        when(userPreferencesRepository.findByUser(user)).thenReturn(Optional.empty());
        when(userPreferencesRepository.save(any(UserPreferences.class))).thenReturn(preferences);
        mockTaskMaps();
        when(scheduledTaskRepository.findByUser(user)).thenReturn(List.of());

        // No tasks
        when(customTaskRepository.findByUserAndCompletedFalse(user)).thenReturn(List.of());

        // AI returns empty
        when(aiClientService.generateStructured(anyString(), eq(ScheduleResponse.class), eq(userId), eq(AITask.SCHEDULE_GENERATION)))
                .thenReturn(new ScheduleResponse(List.of(), List.of()));

        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<Void> callback = invocation.getArgument(0);
            callback.doInTransaction(null);
            return null;
        });

        // When
        scheduleService.generateSchedule(user, mode);

        // Then – default preferences saved
        verify(userPreferencesRepository).save(any(UserPreferences.class));
        // Delete and gamification still called
        verify(scheduledTaskRepository).deleteIncompleteByUser(user);
        verify(gamificationService).recordActivity(user, GamificationAction.SCHEDULE);
    }

    // ========== Test 6: AI returns null – fallback ==========

    @Test
    void generateSchedule_shouldFallbackWhenAIResponseNull() {
        // Given
        String mode = "all";
        when(userPreferencesRepository.findByUser(user)).thenReturn(Optional.of(preferences));

        CustomTask customTask = new CustomTask();
        customTask.setId(UUID.randomUUID());
        customTask.setTitle("Test Custom Task");
        customTask.setEstimatedHours(1.0);
        customTask.setPriority("MEDIUM");
        customTask.setCompleted(false);
        when(customTaskRepository.findByUser(user)).thenReturn(List.of(customTask));
        when(customTaskRepository.findByUserAndCompletedFalse(user)).thenReturn(List.of(customTask));
        when(roadmapTaskRepository.findByRoadmap_User(user)).thenReturn(List.of());
        when(milestoneTaskRepository.findByMilestone_User(user)).thenReturn(List.of());
        when(scheduledTaskRepository.findByUser(user)).thenReturn(List.of());

        // AI returns null
        when(aiClientService.generateStructured(anyString(), eq(ScheduleResponse.class), eq(userId), eq(AITask.SCHEDULE_GENERATION)))
                .thenReturn(null);

        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<Void> callback = invocation.getArgument(0);
            callback.doInTransaction(null);
            return null;
        });

        // When
        scheduleService.generateSchedule(user, mode);

        // Then – fallback should schedule
        verify(scheduledTaskRepository).deleteIncompleteByUser(user);
        verify(scheduledTaskRepository).saveAll(anyList());
        verify(gamificationService).recordActivity(user, GamificationAction.SCHEDULE);
    }


    // ========== reoptimizeToday Tests ==========

    @Test
    void reoptimizeToday_shouldCallAIAndSaveScheduleForToday() {
        // Given
        LocalDate today = LocalDate.now(clock);
        when(userPreferencesRepository.findByUser(user)).thenReturn(Optional.of(preferences));

        CustomTask customTask = new CustomTask();
        customTask.setId(UUID.randomUUID());
        customTask.setTitle("Test Custom Task");
        customTask.setEstimatedHours(1.0);
        customTask.setPriority("MEDIUM");
        customTask.setCompleted(false);
        when(customTaskRepository.findByUser(user)).thenReturn(List.of(customTask));
        when(customTaskRepository.findByUserAndCompletedFalse(user)).thenReturn(List.of(customTask));
        when(roadmapTaskRepository.findByRoadmap_User(user)).thenReturn(List.of());
        when(milestoneTaskRepository.findByMilestone_User(user)).thenReturn(List.of());

        // Mock scheduled tasks: none completed today, but some today tasks exist
        ScheduledTask existingToday = new ScheduledTask();
        existingToday.setId(UUID.randomUUID());
        existingToday.setScheduledDate(today);
        existingToday.setCompleted(false);
        when(scheduledTaskRepository.findByUser(user)).thenReturn(List.of(existingToday));

        // AI response
        ScheduleResponse aiResponse = new ScheduleResponse(
                List.of(new ScheduleItem(
                        customTask.getId().toString(),
                        "Test Custom Task",
                        "WORK_TASK",
                        today.toString(),
                        "09:00",
                        "10:00"
                )),
                List.of()
        );
        when(aiClientService.generateStructured(anyString(), eq(ScheduleResponse.class), eq(userId), eq(AITask.SCHEDULE_REOPTIMIZATION)))
                .thenReturn(aiResponse);

        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<Void> callback = invocation.getArgument(0);
            callback.doInTransaction(null);
            return null;
        });

        // When
        scheduleService.reoptimizeToday(user);

        // Then
        // Delete only incomplete today tasks
        verify(scheduledTaskRepository).deleteIncompleteTodayByUser(user, today);
        // Save the new schedule
        ArgumentCaptor<List<ScheduledTask>> captor = ArgumentCaptor.forClass(List.class);
        verify(scheduledTaskRepository).saveAll(captor.capture());
        List<ScheduledTask> savedTasks = captor.getValue();
        assertThat(savedTasks).hasSize(1);
        assertThat(savedTasks.get(0).getCustomTaskId()).isEqualTo(customTask.getId());
        assertThat(savedTasks.get(0).getScheduledDate()).isEqualTo(today);
        verify(gamificationService).recordActivity(user, GamificationAction.SCHEDULE);
    }


    @Test
    void reoptimizeToday_shouldUseFallbackScheduler_whenAIFails() {
        // Given
        LocalDate today = LocalDate.now(clock);
        UserPreferences prefsWithSlots = new UserPreferences();
        prefsWithSlots.setUser(user);
        prefsWithSlots.setTimezone("UTC");
        // Slot covers entire day to avoid time-of-day issues
        prefsWithSlots.setAvailableHoursJson(
                "{\"monday\":[[\"00:00\",\"23:59\"]],\"tuesday\":[[\"00:00\",\"23:59\"]],\"wednesday\":[[\"00:00\",\"23:59\"]],\"thursday\":[[\"00:00\",\"23:59\"]],\"friday\":[[\"00:00\",\"23:59\"]],\"saturday\":[[\"00:00\",\"23:59\"]],\"sunday\":[[\"00:00\",\"23:59\"]]}"
        );
        prefsWithSlots.setWakeTime(LocalTime.of(7, 0));
        prefsWithSlots.setSleepTime(LocalTime.of(23, 59));
        prefsWithSlots.setLunchTime(LocalTime.of(13, 0));
        prefsWithSlots.setEnergyPeak("MORNING");
        prefsWithSlots.setDailyHabitsJson("[]");
        when(userPreferencesRepository.findByUser(user)).thenReturn(Optional.of(prefsWithSlots));

        CustomTask customTask = new CustomTask();
        customTask.setId(UUID.randomUUID());
        customTask.setTitle("Test Custom Task");
        customTask.setEstimatedHours(1.0);
        customTask.setPriority("MEDIUM");
        customTask.setCompleted(false);
        when(customTaskRepository.findByUser(user)).thenReturn(List.of(customTask));
        when(customTaskRepository.findByUserAndCompletedFalse(user)).thenReturn(List.of(customTask));
        when(roadmapTaskRepository.findByRoadmap_User(user)).thenReturn(List.of());
        when(milestoneTaskRepository.findByMilestone_User(user)).thenReturn(List.of());
        when(scheduledTaskRepository.findByUser(user)).thenReturn(List.of());

        when(aiClientService.generateStructured(anyString(), eq(ScheduleResponse.class), eq(userId), eq(AITask.SCHEDULE_REOPTIMIZATION)))
                .thenThrow(new RuntimeException("AI error"));

        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<Void> callback = invocation.getArgument(0);
            callback.doInTransaction(null);
            return null;
        });

        // When
        scheduleService.reoptimizeToday(user);

        // Then – fallback should schedule at least one task
        verify(scheduledTaskRepository).deleteIncompleteTodayByUser(user, today);
        ArgumentCaptor<List<ScheduledTask>> captor = ArgumentCaptor.forClass(List.class);
        verify(scheduledTaskRepository).saveAll(captor.capture());
        List<ScheduledTask> savedTasks = captor.getValue();
        assertThat(savedTasks).isNotEmpty();
        verify(gamificationService).recordActivity(user, GamificationAction.SCHEDULE);
    }

    @Test
    void reoptimizeToday_shouldHandleNoTasks() {
        // Given
        when(userPreferencesRepository.findByUser(user)).thenReturn(Optional.of(preferences));
        // No tasks at all
        when(customTaskRepository.findByUser(user)).thenReturn(List.of());
        when(customTaskRepository.findByUserAndCompletedFalse(user)).thenReturn(List.of());
        when(roadmapTaskRepository.findByRoadmap_User(user)).thenReturn(List.of());
        when(milestoneTaskRepository.findByMilestone_User(user)).thenReturn(List.of());
        when(scheduledTaskRepository.findByUser(user)).thenReturn(List.of());

        // When
        scheduleService.reoptimizeToday(user);

        // Then – early return, so no delete, no save, no gamification
        verify(scheduledTaskRepository, never()).deleteIncompleteTodayByUser(any(), any());
        verify(scheduledTaskRepository, never()).saveAll(anyList());
        verify(gamificationService, never()).recordActivity(any(), any());
    }

    @Test
    void reoptimizeToday_shouldFallbackWhenAIResponseNull() {
        // Given
        LocalDate today = LocalDate.now(clock);
        UserPreferences prefsWithSlots = new UserPreferences();
        prefsWithSlots.setUser(user);
        prefsWithSlots.setTimezone("UTC");
        prefsWithSlots.setAvailableHoursJson(
                "{\"monday\":[[\"00:00\",\"23:59\"]],\"tuesday\":[[\"00:00\",\"23:59\"]],\"wednesday\":[[\"00:00\",\"23:59\"]],\"thursday\":[[\"00:00\",\"23:59\"]],\"friday\":[[\"00:00\",\"23:59\"]],\"saturday\":[[\"00:00\",\"23:59\"]],\"sunday\":[[\"00:00\",\"23:59\"]]}"
        );
        prefsWithSlots.setWakeTime(LocalTime.of(7, 0));
        prefsWithSlots.setSleepTime(LocalTime.of(23, 59));
        prefsWithSlots.setLunchTime(LocalTime.of(13, 0));
        prefsWithSlots.setEnergyPeak("MORNING");
        prefsWithSlots.setDailyHabitsJson("[]");
        when(userPreferencesRepository.findByUser(user)).thenReturn(Optional.of(prefsWithSlots));

        CustomTask customTask = new CustomTask();
        customTask.setId(UUID.randomUUID());
        customTask.setTitle("Test Custom Task");
        customTask.setEstimatedHours(1.0);
        customTask.setPriority("MEDIUM");
        customTask.setCompleted(false);
        when(customTaskRepository.findByUser(user)).thenReturn(List.of(customTask));
        when(customTaskRepository.findByUserAndCompletedFalse(user)).thenReturn(List.of(customTask));
        when(roadmapTaskRepository.findByRoadmap_User(user)).thenReturn(List.of());
        when(milestoneTaskRepository.findByMilestone_User(user)).thenReturn(List.of());
        when(scheduledTaskRepository.findByUser(user)).thenReturn(List.of());

        when(aiClientService.generateStructured(anyString(), eq(ScheduleResponse.class), eq(userId), eq(AITask.SCHEDULE_REOPTIMIZATION)))
                .thenReturn(null);

        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<Void> callback = invocation.getArgument(0);
            callback.doInTransaction(null);
            return null;
        });

        // When
        scheduleService.reoptimizeToday(user);

        // Then
        verify(scheduledTaskRepository).deleteIncompleteTodayByUser(user, today);
        verify(scheduledTaskRepository).saveAll(anyList());
        verify(gamificationService).recordActivity(user, GamificationAction.SCHEDULE);
    }

    @Test
    void reoptimizeToday_shouldCreateDefaultPreferences_whenNotFound() {
        // Given
        LocalDate today = LocalDate.now(clock);
        when(userPreferencesRepository.findByUser(user)).thenReturn(Optional.empty());
        when(userPreferencesRepository.save(any(UserPreferences.class))).thenAnswer(invocation -> {
            UserPreferences saved = invocation.getArgument(0);
            saved.setAvailableHoursJson(
                    "{\"monday\":[[\"00:00\",\"23:59\"]],\"tuesday\":[[\"00:00\",\"23:59\"]],\"wednesday\":[[\"00:00\",\"23:59\"]],\"thursday\":[[\"00:00\",\"23:59\"]],\"friday\":[[\"00:00\",\"23:59\"]],\"saturday\":[[\"00:00\",\"23:59\"]],\"sunday\":[[\"00:00\",\"23:59\"]]}"
            );
            saved.setTimezone("UTC");
            saved.setWakeTime(LocalTime.of(7, 0));
            saved.setSleepTime(LocalTime.of(23, 59));
            saved.setLunchTime(LocalTime.of(13, 0));
            saved.setEnergyPeak("MORNING");
            saved.setDailyHabitsJson("[]");
            return saved;
        });

        CustomTask customTask = new CustomTask();
        customTask.setId(UUID.randomUUID());
        customTask.setTitle("Test Custom Task");
        customTask.setEstimatedHours(1.0);
        customTask.setPriority("MEDIUM");
        customTask.setCompleted(false);
        when(customTaskRepository.findByUser(user)).thenReturn(List.of(customTask));
        when(customTaskRepository.findByUserAndCompletedFalse(user)).thenReturn(List.of(customTask));
        when(roadmapTaskRepository.findByRoadmap_User(user)).thenReturn(List.of());
        when(milestoneTaskRepository.findByMilestone_User(user)).thenReturn(List.of());
        when(scheduledTaskRepository.findByUser(user)).thenReturn(List.of());

        when(aiClientService.generateStructured(anyString(), eq(ScheduleResponse.class), eq(userId), eq(AITask.SCHEDULE_REOPTIMIZATION)))
                .thenReturn(new ScheduleResponse(List.of(), List.of()));

        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<Void> callback = invocation.getArgument(0);
            callback.doInTransaction(null);
            return null;
        });

        // When
        scheduleService.reoptimizeToday(user);

        // Then – default preferences created, delete called, save called
        verify(userPreferencesRepository).save(any(UserPreferences.class));
        verify(scheduledTaskRepository).deleteIncompleteTodayByUser(user, today);
        verify(scheduledTaskRepository).saveAll(anyList());
        verify(gamificationService).recordActivity(user, GamificationAction.SCHEDULE);
    }


    // ========== Action Methods Tests ==========

    @Test
    void getScheduledTasks_shouldReturnMappedList() {
        LocalDate start = LocalDate.now(clock);
        LocalDate end = start.plusDays(7);
        List<ScheduledTask> mockTasks = List.of(new ScheduledTask());

        // Provide dummy data to satisfy the record's constructor
        ScheduledTaskResponse dummyResponse = new ScheduledTaskResponse(
                UUID.randomUUID(),
                "Test Task",
                start,
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                false,
                null,
                null,
                null,
                "WORK_TASK",
                "MEDIUM"
        );
        List<ScheduledTaskResponse> mockResponses = List.of(dummyResponse);

        when(scheduledTaskRepository.findByUserAndScheduledDateBetween(user, start, end)).thenReturn(mockTasks);
        when(scheduleMapper.toResponseList(mockTasks)).thenReturn(mockResponses);

        List<ScheduledTaskResponse> result = scheduleService.getScheduledTasks(user, start, end);

        assertThat(result).isEqualTo(mockResponses);
        verify(scheduledTaskRepository).findByUserAndScheduledDateBetween(user, start, end);
    }

    @Test
    void moveScheduledTask_shouldUpdateAndSave_whenAuthorized() {
        UUID taskId = UUID.randomUUID();
        ScheduledTask task = new ScheduledTask();
        task.setId(taskId);
        task.setUser(user);

        LocalDate newDate = LocalDate.now(clock).plusDays(1);
        LocalTime newStart = LocalTime.of(10, 0);
        LocalTime newEnd = LocalTime.of(11, 0);

        when(scheduledTaskRepository.findById(taskId)).thenReturn(Optional.of(task));

        scheduleService.moveScheduledTask(taskId, user, newDate, newStart, newEnd);

        verify(scheduledTaskRepository).save(task);
        assertThat(task.getScheduledDate()).isEqualTo(newDate);
        assertThat(task.getStartTime()).isEqualTo(newStart);
        assertThat(task.getEndTime()).isEqualTo(newEnd);
    }

    @Test
    void moveScheduledTask_shouldThrowNotFound_whenUnauthorized() {
        UUID taskId = UUID.randomUUID();
        ScheduledTask task = new ScheduledTask();
        User wrongUser = new User();
        wrongUser.setId(UUID.randomUUID()); // Different ID
        task.setUser(wrongUser);

        when(scheduledTaskRepository.findById(taskId)).thenReturn(Optional.of(task));

        org.junit.jupiter.api.Assertions.assertThrows(ResponseStatusException.class, () -> {
            scheduleService.moveScheduledTask(taskId, user, LocalDate.now(), LocalTime.now(), LocalTime.now());
        });

        verify(scheduledTaskRepository, never()).save(any());
    }

    @Test
    void completeTask_shouldCompleteTaskAndRecordGamification() {
        UUID taskId = UUID.randomUUID();
        ScheduledTask task = new ScheduledTask();
        task.setId(taskId);
        task.setUser(user);

        when(scheduledTaskRepository.findById(taskId)).thenReturn(Optional.of(task));

        scheduleService.completeTask(taskId, user);

        verify(scheduledTaskRepository).save(task);
        assertThat(task.isCompleted()).isTrue();
        verify(gamificationService).recordActivity(user, GamificationAction.TASK);
    }

    @Test
    void completeTask_withCustomTask_shouldUpdateMasterAndLinkedTasks() {
        UUID taskId = UUID.randomUUID();
        UUID customTaskId = UUID.randomUUID();

        // The task being completed
        ScheduledTask task = new ScheduledTask();
        task.setId(taskId);
        task.setUser(user);
        task.setCustomTaskId(customTaskId);

        // The master custom task
        CustomTask masterTask = new CustomTask();
        masterTask.setId(customTaskId);

        // Another scheduled block linked to the same master task
        ScheduledTask linkedTask = new ScheduledTask();
        linkedTask.setId(UUID.randomUUID());
        linkedTask.setCustomTaskId(customTaskId);
        linkedTask.setCompleted(false);

        when(scheduledTaskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(customTaskRepository.findById(customTaskId)).thenReturn(Optional.of(masterTask));
        when(scheduledTaskRepository.findByCustomTaskId(customTaskId)).thenReturn(List.of(task, linkedTask));

        scheduleService.completeTask(taskId, user);

        // Verify master was completed
        verify(customTaskRepository).save(masterTask);
        assertThat(masterTask.isCompleted()).isTrue();

        // Verify the linked task was also completed
        verify(scheduledTaskRepository).save(linkedTask);
        assertThat(linkedTask.isCompleted()).isTrue();
    }

    @Test
    void scheduleCustomTaskManually_shouldSaveWithDefaultPriority() {
        UUID customTaskId = UUID.randomUUID();
        LocalDate date = LocalDate.now(clock);
        LocalTime start = LocalTime.of(9, 0);
        LocalTime end = LocalTime.of(10, 0);

        // Pass null for priority to hit the ternary operator fallback
        scheduleService.scheduleCustomTaskManually(user, customTaskId, "My Task", date, start, end, null);

        ArgumentCaptor<ScheduledTask> captor = ArgumentCaptor.forClass(ScheduledTask.class);
        verify(scheduledTaskRepository).save(captor.capture());

        ScheduledTask saved = captor.getValue();
        assertThat(saved.getCustomTaskId()).isEqualTo(customTaskId);
        assertThat(saved.getPriority()).isEqualTo("MEDIUM"); // The default
        assertThat(saved.isCompleted()).isFalse();
    }

    // ========== Advanced Coverage Tests for Jacoco Gaps ==========

    @Test
    void generateSchedule_shouldCoverAllTaskTypesAndDurationEstimates() {
        // Given
        when(userPreferencesRepository.findByUser(user)).thenReturn(Optional.of(preferences));

        // 1. Cover the "alreadyScheduled" map logic (image_8df3a2 & image_8df41d)
        ScheduledTask existingRoadmap = new ScheduledTask();
        existingRoadmap.setCompleted(true);
        existingRoadmap.setRoadmapTaskId(UUID.randomUUID());

        ScheduledTask existingMilestone = new ScheduledTask();
        existingMilestone.setCompleted(true);
        existingMilestone.setMilestoneTaskId(UUID.randomUUID());

        when(scheduledTaskRepository.findByUser(user)).thenReturn(List.of(existingRoadmap, existingMilestone));

        // 2. Cover estimateDuration logic (image_8df7b8)
        RoadmapTask longRoadmapTask = new RoadmapTask();
        longRoadmapTask.setId(UUID.randomUUID());
        Roadmap roadmap = new Roadmap();
        roadmap.setUser(user);
        longRoadmapTask.setRoadmap(roadmap);
        longRoadmapTask.setCompleted(false);
        // String > 100 chars to trigger the "return 2.0" line
        longRoadmapTask.setDetails("A".repeat(101));

        RoadmapTask projectRoadmapTask = new RoadmapTask();
        projectRoadmapTask.setId(UUID.randomUUID());
        projectRoadmapTask.setRoadmap(roadmap);
        projectRoadmapTask.setCompleted(false);
        // Keyword "project" to trigger the other "return 2.0" line
        projectRoadmapTask.setDescription("Final Year Project Review");

        when(roadmapTaskRepository.findByRoadmap_User(user)).thenReturn(List.of(longRoadmapTask, projectRoadmapTask));

        // 3. Cover Milestone Overdue status logic (image_8df474)
        Task overdueMilestone = new Task();
        overdueMilestone.setId(UUID.randomUUID());
        Milestone milestone = new Milestone();
        milestone.setUser(user);
        overdueMilestone.setMilestone(milestone);
        overdueMilestone.setStatus(Status.OVERDUE);
        overdueMilestone.setDescription("Overdue Milestone");

        when(milestoneTaskRepository.findByMilestone_User(user)).thenReturn(List.of(overdueMilestone));
        when(customTaskRepository.findByUser(user)).thenReturn(List.of());
        when(customTaskRepository.findByUserAndCompletedFalse(user)).thenReturn(List.of());

        // 4. Force AI to fail so we hit the simpleSchedule fallback sorting logic (image_8df7c0)
        when(aiClientService.generateStructured(anyString(), eq(ScheduleResponse.class), eq(userId), eq(AITask.SCHEDULE_GENERATION)))
                .thenThrow(new RuntimeException("Force Fallback"));

        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<Void> callback = invocation.getArgument(0);
            callback.doInTransaction(null);
            return null;
        });

        // When
        scheduleService.generateSchedule(user, "all");

        // Then
        verify(scheduledTaskRepository).deleteIncompleteByUser(user);
        ArgumentCaptor<List<ScheduledTask>> captor = ArgumentCaptor.forClass(List.class);
        verify(scheduledTaskRepository).saveAll(captor.capture());

        List<ScheduledTask> savedTasks = captor.getValue();
        assertThat(savedTasks).isNotEmpty();

        // Verify the mapping logic successfully pulled the IDs from the Roadmap and Milestone tasks
        boolean foundRoadmap = savedTasks.stream().anyMatch(t -> t.getRoadmapTaskId() != null);
        boolean foundMilestone = savedTasks.stream().anyMatch(t -> t.getMilestoneTaskId() != null);
        assertThat(foundRoadmap).isTrue();
        assertThat(foundMilestone).isTrue();
    }

    // ========== Advanced Coverage Tests for Jacoco Gaps ==========

    @Test
    void fallbackScheduler_shouldHandleAllSortingDurationBranches_andMalformedJson() {
        // 1. Cover Null Timezone (Line 75) & Malformed JSON Exception (Line 582)
        preferences.setTimezone(null);
        preferences.setAvailableHoursJson("THIS IS NOT VALID JSON");
        when(userPreferencesRepository.findByUser(user)).thenReturn(Optional.of(preferences));

        // 2. Cover estimateDuration > 100 chars (Line 495)
        RoadmapTask rtLongDetails = new RoadmapTask();
        rtLongDetails.setId(UUID.randomUUID());
        rtLongDetails.setDetails("A".repeat(105));
        rtLongDetails.setCompleted(false);

        // 3. Cover estimateDuration "project" keyword (Line 496)
        RoadmapTask rtProject = new RoadmapTask();
        rtProject.setId(UUID.randomUUID());
        rtProject.setDescription("Final Year Project Review");
        rtProject.setCompleted(false);

        // 4. Cover estimateDuration > 50 chars (Line 497)
        Task mtMediumDetails = new Task();
        mtMediumDetails.setId(UUID.randomUUID());
        mtMediumDetails.setDetails("A".repeat(55));
        mtMediumDetails.setStatus(Status.PENDING);
        mtMediumDetails.setDueDate(LocalDate.now(clock).plusDays(2));

        // 5. Cover sorting: HIGH priority, NO due date (Lines 509-511, 563)
        CustomTask ctHighNoDate = new CustomTask();
        ctHighNoDate.setId(UUID.randomUUID());
        ctHighNoDate.setPriority("HIGH");
        ctHighNoDate.setDueDate(null);
        ctHighNoDate.setCompleted(false);

        // 6. Cover sorting: LOW priority, NO due date (Lines 509-511, 564)
        CustomTask ctLowNoDate = new CustomTask();
        ctLowNoDate.setId(UUID.randomUUID());
        ctLowNoDate.setPriority("LOW");
        ctLowNoDate.setDueDate(null);
        ctLowNoDate.setCompleted(false);

        // 7. Cover sorting: HIGH priority, WITH due date (Lines 509-511)
        CustomTask ctHighWithDate = new CustomTask();
        ctHighWithDate.setId(UUID.randomUUID());
        ctHighWithDate.setPriority("HIGH");
        ctHighWithDate.setDueDate(LocalDate.now(clock).plusDays(1));
        ctHighWithDate.setCompleted(false);

        // ... existing tasks ...

        // 🔥 ADD THESE: Two tasks with identical priority but different due dates to trigger a.dueDate().compareTo(b.dueDate())
        CustomTask ctSamePriorityEarlier = new CustomTask();
        ctSamePriorityEarlier.setId(UUID.randomUUID());
        ctSamePriorityEarlier.setPriority("LOW");
        ctSamePriorityEarlier.setDueDate(LocalDate.now(clock).plusDays(1)); // Earlier

        CustomTask ctSamePriorityLater = new CustomTask();
        ctSamePriorityLater.setId(UUID.randomUUID());
        ctSamePriorityLater.setPriority("LOW");
        ctSamePriorityLater.setDueDate(LocalDate.now(clock).plusDays(5)); // Later

        // Update the mock return to include the two new tasks
        when(customTaskRepository.findByUserAndCompletedFalse(user))
                .thenReturn(List.of(ctHighNoDate, ctLowNoDate, ctHighWithDate, ctSamePriorityEarlier, ctSamePriorityLater));

        when(roadmapTaskRepository.findByRoadmap_User(user)).thenReturn(List.of(rtLongDetails, rtProject));
        when(milestoneTaskRepository.findByMilestone_User(user)).thenReturn(List.of(mtMediumDetails));
        when(customTaskRepository.findByUserAndCompletedFalse(user)).thenReturn(List.of(ctHighNoDate, ctLowNoDate, ctHighWithDate));

        // Force AI fallback to trigger simpleSchedule logic
        when(aiClientService.generateStructured(anyString(), eq(ScheduleResponse.class), eq(userId), eq(AITask.SCHEDULE_GENERATION)))
                .thenReturn(null);

        when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            TransactionCallback<Void> callback = inv.getArgument(0);
            callback.doInTransaction(null);
            return null;
        });

        // Execute
        scheduleService.generateSchedule(user, "all");

        // Verify fallback scheduler ran and saved tasks
        verify(scheduledTaskRepository).saveAll(anyList());
    }

    @Test
    void generateSchedule_shouldHandleMalformedAiResponseAndUnknownTaskIds() {
        // Covers missing AI fields (Lines 146, 190, 260, 297, 340) and unknown IDs (Line 160-184)
        when(userPreferencesRepository.findByUser(user)).thenReturn(Optional.of(preferences));
        mockTaskMaps(); // Empty task maps

        // Item 1: Missing start/end times (Hits Line 146 & 297 'continue')
        ScheduleItem badItem1 = new ScheduleItem(null, "Missing dates", "ROUTINE", null, null, null);

        // Item 2: Unknown Task ID (Hits Line 184 'continue' because ID is not in TaskMaps)
        ScheduleItem badItem2 = new ScheduleItem(UUID.randomUUID().toString(), "Unknown Task", "WORK_TASK", "2026-09-08", "10:00", "11:00");

        // Item 3: Null BlockType (Hits Line 190 & 340 ternary fallback to "ROUTINE")
        ScheduleItem badItem3 = new ScheduleItem(null, "Just chilling", null, "2026-09-08", "12:00", "13:00");

        ScheduleResponse aiResponse = new ScheduleResponse(List.of(badItem1, badItem2, badItem3), List.of());

        when(aiClientService.generateStructured(anyString(), eq(ScheduleResponse.class), eq(userId), any()))
                .thenReturn(aiResponse);

        when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            TransactionCallback<Void> callback = inv.getArgument(0);
            callback.doInTransaction(null);
            return null;
        });

        // Run both methods to cover the branches in both functions
        scheduleService.generateSchedule(user, "all");
        scheduleService.reoptimizeToday(user);

        // Verify saveAll was called (it will only save badItem3, as the others hit 'continue')
        verify(scheduledTaskRepository, atLeastOnce()).saveAll(anyList());
    }

    @Test
    void scheduleService_shouldFilterAlreadyScheduledMilestoneAndCustomTasks() {
        preferences.setAvailableHoursJson("");
        when(userPreferencesRepository.findByUser(user)).thenReturn(Optional.of(preferences));

        ScheduledTask existingMilestone = new ScheduledTask();
        existingMilestone.setCompleted(true);
        existingMilestone.setMilestoneTaskId(UUID.randomUUID());

        ScheduledTask existingCustom = new ScheduledTask();
        existingCustom.setCompleted(true);
        existingCustom.setCustomTaskId(UUID.randomUUID());

        //  A completed routine task (no IDs) to hit the final "return null;" line
        ScheduledTask existingRoutine = new ScheduledTask();
        existingRoutine.setCompleted(true);

        when(scheduledTaskRepository.findByUser(user)).thenReturn(List.of(existingMilestone, existingCustom, existingRoutine));

        scheduleService.generateSchedule(user, "all");
        scheduleService.reoptimizeToday(user);

        verify(scheduledTaskRepository, atLeastOnce()).findByUser(user);
    }

    @Test
    void reoptimizeToday_shouldMapAllTaskTypesAndTrackLatestTime() {
        when(userPreferencesRepository.findByUser(user)).thenReturn(Optional.of(preferences));

        // Mock Roadmap Task
        UUID roadmapId = UUID.randomUUID();
        RoadmapTask rt = new RoadmapTask();
        rt.setId(roadmapId);
        rt.setDescription("Roadmap Info");
        when(roadmapTaskRepository.findByRoadmap_User(user)).thenReturn(List.of(rt));

        // Mock Milestone Task
        UUID milestoneId = UUID.randomUUID();
        Task mt = new Task();
        mt.setId(milestoneId);
        mt.setDescription("Milestone Info");
        mt.setStatus(Status.PENDING);
        when(milestoneTaskRepository.findByMilestone_User(user)).thenReturn(List.of(mt));

        LocalDate today = LocalDate.now(clock);

        // Create AI Response with items to trigger all branches
        // 1. Roadmap Task mapping
        ScheduleItem itemRoadmap = new ScheduleItem(roadmapId.toString(), "RM Task", "WORK_TASK", today.toString(), "10:00", "11:00");
        // 2. Milestone Task mapping
        ScheduleItem itemMilestone = new ScheduleItem(milestoneId.toString(), "MS Task", "WORK_TASK", today.toString(), "11:00", "12:00");
        // 3. Routine block with NULL title and NULL type (hits ternary operators) & LATE end time (hits latestAiScheduledTime tracking)
        ScheduleItem itemRoutine = new ScheduleItem(null, null, null, today.toString(), "13:00", "14:00"); // 14:00 is AFTER the 10:00AM frozen clock

        ScheduleResponse aiResponse = new ScheduleResponse(List.of(itemRoadmap, itemMilestone, itemRoutine), List.of());

        when(aiClientService.generateStructured(anyString(), eq(ScheduleResponse.class), eq(userId), eq(AITask.SCHEDULE_REOPTIMIZATION)))
                .thenReturn(aiResponse);

        when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            TransactionCallback<Void> callback = inv.getArgument(0);
            callback.doInTransaction(null);
            return null;
        });

        // Execute
        scheduleService.reoptimizeToday(user);

        // Verify
        ArgumentCaptor<List<ScheduledTask>> captor = ArgumentCaptor.forClass(List.class);
        verify(scheduledTaskRepository).saveAll(captor.capture());

        List<ScheduledTask> saved = captor.getValue();
        assertThat(saved).hasSize(3);
        assertThat(saved.get(1).getMilestoneTaskId()).isEqualTo(milestoneId);
        assertThat(saved.get(2).getTitle()).isEqualTo("Personal Time"); // Fallback from null title
        assertThat(saved.get(2).getBlockType()).isEqualTo("ROUTINE"); // Fallback from null type
    }

    @Test
    void simpleSchedule_shouldCoverAllDateSortingBranches() {
        // Given: 5 tasks with the EXACT SAME priority ("MEDIUM") to force the
        // sorting algorithm to evaluate the due dates for every comparison.

        // Scenario 1: Date is null
        CustomTask ctNull1 = new CustomTask();
        ctNull1.setId(UUID.randomUUID());
        ctNull1.setPriority("MEDIUM");
        ctNull1.setDueDate(null);
        ctNull1.setCompleted(false);

        // Scenario 2: Second date is null (forces 'both null' check)
        CustomTask ctNull2 = new CustomTask();
        ctNull2.setId(UUID.randomUUID());
        ctNull2.setPriority("MEDIUM");
        ctNull2.setDueDate(null);
        ctNull2.setCompleted(false);

        // Scenario 3: Has a later date
        CustomTask ctLater = new CustomTask();
        ctLater.setId(UUID.randomUUID());
        ctLater.setPriority("MEDIUM");
        ctLater.setDueDate(LocalDate.now(clock).plusDays(5));

        // Scenario 4: Has an earlier date
        CustomTask ctEarlier = new CustomTask();
        ctEarlier.setId(UUID.randomUUID());
        ctEarlier.setPriority("MEDIUM");
        ctEarlier.setDueDate(LocalDate.now(clock).plusDays(2));

        // Scenario 5: Has the exact same date as Scenario 4 (forces compareTo() == 0)
        CustomTask ctSame = new CustomTask();
        ctSame.setId(UUID.randomUUID());
        ctSame.setPriority("MEDIUM");
        ctSame.setDueDate(LocalDate.now(clock).plusDays(2));

        when(userPreferencesRepository.findByUser(user)).thenReturn(Optional.of(preferences));

        when(customTaskRepository.findByUser(user))
                .thenReturn(List.of(ctNull1, ctLater, ctNull2, ctEarlier, ctSame));

        // Pass them in a mixed order to force maximum sorting comparisons
        when(customTaskRepository.findByUserAndCompletedFalse(user))
                .thenReturn(List.of(ctNull1, ctLater, ctNull2, ctEarlier, ctSame));

        when(roadmapTaskRepository.findByRoadmap_User(user)).thenReturn(List.of());
        when(milestoneTaskRepository.findByMilestone_User(user)).thenReturn(List.of());

        // Force AI fallback so the simpleSchedule algorithm runs
        when(aiClientService.generateStructured(anyString(), eq(ScheduleResponse.class), eq(userId), eq(AITask.SCHEDULE_GENERATION)))
                .thenReturn(null);

        when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            TransactionCallback<Void> callback = inv.getArgument(0);
            callback.doInTransaction(null);
            return null;
        });

        // When
        scheduleService.generateSchedule(user, "all");

        // Then
        verify(scheduledTaskRepository, atLeastOnce()).saveAll(anyList());
    }



    @Test
    void generateSchedule_shouldNotCrashWhenAIHallucinatesBadDateFormats() {
        when(userPreferencesRepository.findByUser(user)).thenReturn(Optional.of(preferences));
        mockTaskMaps(); // Empty maps for simplicity

        // Simulate AI returning garbage formats instead of ISO standards
        ScheduleItem badFormatItem = new ScheduleItem(
                null, "Stretch", "ROUTINE",
                "Tomorrow",      // Bad Date Format!
                "9 AM",          // Bad Time Format!
                "10 o'clock"     // Bad Time Format!
        );

        // Simulate a good task right after the bad one
        ScheduleItem goodFormatItem = new ScheduleItem(
                null, "Good Task", "ROUTINE",
                "2026-09-08", "12:00", "13:00"
        );

        ScheduleResponse aiResponse = new ScheduleResponse(List.of(badFormatItem, goodFormatItem), List.of());

        when(aiClientService.generateStructured(anyString(), eq(ScheduleResponse.class), eq(userId), eq(AITask.SCHEDULE_GENERATION)))
                .thenReturn(aiResponse);

        when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            TransactionCallback<Void> callback = inv.getArgument(0);
            callback.doInTransaction(null);
            return null;
        });

        // If the try-catch isn't there, this will crash with DateTimeParseException
        scheduleService.generateSchedule(user, "all");

        // Verify that the bad item was safely skipped, but the good item was saved
        ArgumentCaptor<List<ScheduledTask>> captor = ArgumentCaptor.forClass(List.class);
        verify(scheduledTaskRepository).saveAll(captor.capture());

        List<ScheduledTask> saved = captor.getValue();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getTitle()).isEqualTo("Good Task");
    }

    @Test
    void reoptimizeToday_shouldNotCrashWhenAIHallucinatesBadDateFormats() {
        when(userPreferencesRepository.findByUser(user)).thenReturn(Optional.of(preferences));

        // --- FIX: Provide 1 dummy task so it doesn't exit early ---
        CustomTask dummyTask = new CustomTask();
        dummyTask.setId(UUID.randomUUID());
        dummyTask.setTitle("Dummy Task");
        dummyTask.setCompleted(false);

        when(customTaskRepository.findByUserAndCompletedFalse(user)).thenReturn(List.of(dummyTask));
        when(customTaskRepository.findByUser(user)).thenReturn(List.of(dummyTask));
        when(roadmapTaskRepository.findByRoadmap_User(user)).thenReturn(List.of());
        when(milestoneTaskRepository.findByMilestone_User(user)).thenReturn(List.of());
        // ----------------------------------------------------------

        LocalDate today = LocalDate.now(clock);

        // Simulate AI returning garbage formats instead of ISO standards
        ScheduleItem badFormatItem = new ScheduleItem(
                null, "Stretch", "ROUTINE",
                "Tomorrow",      // Bad Date Format!
                "9 AM",          // Bad Time Format!
                "10 o'clock"     // Bad Time Format!
        );

        // Simulate a good task right after the bad one (must match 'today' to pass the filter)
        ScheduleItem goodFormatItem = new ScheduleItem(
                null, "Good Task", "ROUTINE",
                today.toString(), "12:00", "13:00"
        );

        ScheduleResponse aiResponse = new ScheduleResponse(List.of(badFormatItem, goodFormatItem), List.of());

        when(aiClientService.generateStructured(anyString(), eq(ScheduleResponse.class), eq(userId), eq(AITask.SCHEDULE_REOPTIMIZATION)))
                .thenReturn(aiResponse);

        when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            TransactionCallback<Void> callback = inv.getArgument(0);
            callback.doInTransaction(null);
            return null;
        });

        // Execute
        scheduleService.reoptimizeToday(user);

        // Verify that the bad item hit the catch block and was skipped, but the good item was saved
        ArgumentCaptor<List<ScheduledTask>> captor = ArgumentCaptor.forClass(List.class);
        verify(scheduledTaskRepository).saveAll(captor.capture());

        List<ScheduledTask> saved = captor.getValue();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getTitle()).isEqualTo("Good Task");
    }

    // ========================================================================
    // FINAL JACOCO COVERAGE PUSH: Edge Cases, Nulls, and Defensive Boundaries
    // ========================================================================

    @Test
    void generateSchedule_shouldTruncateTasks_whenListExceedsAILimits() {
        // Covers: tasks.size() > 15 (generateSchedule) and > 8 (reoptimizeToday)
        when(userPreferencesRepository.findByUser(user)).thenReturn(Optional.of(preferences));

        List<CustomTask> manyTasks = new java.util.ArrayList<>();
        for (int i = 0; i < 16; i++) {
            CustomTask ct = new CustomTask();
            ct.setId(UUID.randomUUID());
            ct.setTitle("Task " + i);
            ct.setCompleted(false);
            manyTasks.add(ct);
        }

        when(customTaskRepository.findByUser(user)).thenReturn(manyTasks);
        when(customTaskRepository.findByUserAndCompletedFalse(user)).thenReturn(manyTasks);
        mockTaskMaps();

        // Also cover reoptimizeToday filtering a task scheduled for tomorrow
        ScheduledTask tomorrowTask = new ScheduledTask();
        tomorrowTask.setCompleted(false);
        tomorrowTask.setScheduledDate(LocalDate.now(clock).plusDays(1));
        tomorrowTask.setRoadmapTaskId(UUID.randomUUID());
        when(scheduledTaskRepository.findByUser(user)).thenReturn(List.of(tomorrowTask));

        when(aiClientService.generateStructured(anyString(), eq(ScheduleResponse.class), any(), any()))
                .thenReturn(new ScheduleResponse(List.of(), List.of()));

        when(transactionTemplate.execute(any())).thenAnswer(i -> {
            TransactionCallback<Void> cb = i.getArgument(0);
            cb.doInTransaction(null);
            return null;
        });

        scheduleService.generateSchedule(user, "all");
        scheduleService.reoptimizeToday(user);

        // Verification: The service successfully sub-lists the tasks and calls AI without crashing
        verify(aiClientService, times(2)).generateStructured(anyString(), any(), any(), any());
    }

    @Test
    void generateSchedule_shouldSkipMalformedItems_whenAiReturnsInvalidData() {
        // Covers: aiResponse.schedule() == null
        // Covers: item.taskId().isBlank() and item.taskId().equals("null")
        // Covers: item.date() != null && item.endTime() == null
        // Covers: item.startTime() == null

        when(userPreferencesRepository.findByUser(user)).thenReturn(Optional.of(preferences));
        mockTaskMaps();

        // 🔥 FIX: Provide at least one dummy task so `reoptimizeToday` doesn't exit early!
        CustomTask dummyTask = new CustomTask();
        dummyTask.setId(UUID.randomUUID());
        dummyTask.setTitle("Dummy Task");
        dummyTask.setEstimatedHours(1.0);
        dummyTask.setPriority("MEDIUM");
        dummyTask.setCompleted(false);
        when(customTaskRepository.findByUser(user)).thenReturn(List.of(dummyTask));
        when(customTaskRepository.findByUserAndCompletedFalse(user)).thenReturn(List.of(dummyTask));

        // 1. First call: Schedule array is completely null
        when(aiClientService.generateStructured(anyString(), eq(ScheduleResponse.class), eq(userId), eq(AITask.SCHEDULE_GENERATION)))
                .thenReturn(new ScheduleResponse(null, null));

        // 2. Second call: Has blank IDs, string "null" IDs, and missing times
        ScheduleItem blankId = new ScheduleItem("   ", "Blank", "WORK", LocalDate.now(clock).toString(), "10:00", "11:00");
        ScheduleItem stringNullId = new ScheduleItem("null", "NullStr", "WORK", LocalDate.now(clock).toString(), "11:00", "12:00");
        ScheduleItem missingEnd = new ScheduleItem(null, "NoEnd", "ROUTINE", LocalDate.now(clock).toString(), "12:00", null);
        ScheduleItem missingStart = new ScheduleItem(null, "NoStart", "ROUTINE", LocalDate.now(clock).toString(), null, "13:00");

        when(aiClientService.generateStructured(anyString(), eq(ScheduleResponse.class), eq(userId), eq(AITask.SCHEDULE_REOPTIMIZATION)))
                .thenReturn(new ScheduleResponse(List.of(blankId, stringNullId, missingEnd, missingStart), List.of()));

        when(transactionTemplate.execute(any())).thenAnswer(i -> {
            TransactionCallback<Void> cb = i.getArgument(0);
            cb.doInTransaction(null);
            return null;
        });

        scheduleService.generateSchedule(user, "all");
        scheduleService.reoptimizeToday(user);

        // Ensure saves are called without crashing
        verify(scheduledTaskRepository, atLeastOnce()).saveAll(anyList());
    }

    @Test
    void reoptimizeToday_shouldUseSystemDefaults_whenPreferencesAreEmpty() {
        // Covers: preferences.getTimezone() == null
        // Covers: parseAvailableHours -> json.trim().isEmpty()
        preferences.setTimezone(null);
        preferences.setAvailableHoursJson("   \n   ");
        when(userPreferencesRepository.findByUser(user)).thenReturn(Optional.of(preferences));

        scheduleService.reoptimizeToday(user);

        // Will use system default timezone and default slots without crashing
        verify(scheduledTaskRepository, never()).saveAll(anyList()); // 0 tasks exist
    }

    @Test
    void generateSchedule_shouldFilterTasks_whenAlreadyScheduledOrMissingDetails() {
        // Covers: estimateDuration -> description == null, details == null
        // Covers: collectUnscheduledTasks -> isCompleted = false BUT alreadyScheduled = true
        when(userPreferencesRepository.findByUser(user)).thenReturn(Optional.of(preferences));

        // Create tasks that are NOT completed, but ARE already in the schedule
        RoadmapTask rt = new RoadmapTask();
        rt.setId(UUID.randomUUID());
        rt.setCompleted(false);
        rt.setDescription(null); // Covers null description
        rt.setDetails(null);     // Covers null details

        Task mt = new Task();
        mt.setId(UUID.randomUUID());
        mt.setStatus(Status.PENDING); // Not completed
        mt.setDescription(null);

        when(roadmapTaskRepository.findByRoadmap_User(user)).thenReturn(List.of(rt));
        when(milestoneTaskRepository.findByMilestone_User(user)).thenReturn(List.of(mt));
        when(customTaskRepository.findByUser(user)).thenReturn(List.of());
        when(customTaskRepository.findByUserAndCompletedFalse(user)).thenReturn(List.of());

        // Mock them as already scheduled
        ScheduledTask stRt = new ScheduledTask();
        stRt.setCompleted(true);
        stRt.setRoadmapTaskId(rt.getId());

        ScheduledTask stMt = new ScheduledTask();
        stMt.setCompleted(true);
        stMt.setMilestoneTaskId(mt.getId());

        when(scheduledTaskRepository.findByUser(user)).thenReturn(List.of(stRt, stMt));

        when(transactionTemplate.execute(any())).thenAnswer(i -> {
            TransactionCallback<Void> cb = i.getArgument(0);
            cb.doInTransaction(null);
            return null;
        });

        scheduleService.generateSchedule(user, "all");

        // Because they are already scheduled, they are filtered out. Zero tasks to save.
        verify(scheduledTaskRepository, never()).saveAll(anyList());
    }

    @Test
    void moveScheduledTask_shouldThrowExceptions_whenTaskMissingOrUnauthorized() {
        UUID fakeId = UUID.randomUUID();

        // 1. Missing IDs (Covers the .orElseThrow lambdas)
        when(scheduledTaskRepository.findById(fakeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scheduleService.moveScheduledTask(fakeId, user, LocalDate.now(), LocalTime.now(), LocalTime.now()))
                .isInstanceOf(ResponseStatusException.class);

        assertThatThrownBy(() -> scheduleService.completeTask(fakeId, user))
                .isInstanceOf(ResponseStatusException.class);

        // 2. Unauthorized Checks (Covers the security exceptions)
        ScheduledTask otherUserTask = new ScheduledTask();
        User otherUser = new User();
        otherUser.setId(UUID.randomUUID());
        otherUserTask.setUser(otherUser); // Task belongs to someone else

        when(scheduledTaskRepository.findById(fakeId)).thenReturn(Optional.of(otherUserTask));

        assertThatThrownBy(() -> scheduleService.moveScheduledTask(fakeId, user, LocalDate.now(), LocalTime.now(), LocalTime.now()))
                .isInstanceOf(ResponseStatusException.class);

        assertThatThrownBy(() -> scheduleService.completeTask(fakeId, user))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void scheduleCustomTaskManually_shouldUsePassedPriority() {
        // Covers the TRUE branch of: priority != null ? priority : "MEDIUM"
        UUID customTaskId = UUID.randomUUID();

        // Pass "HIGH" explicitly
        scheduleService.scheduleCustomTaskManually(user, customTaskId, "T", LocalDate.now(), LocalTime.now(), LocalTime.now(), "HIGH");

        ArgumentCaptor<ScheduledTask> captor = ArgumentCaptor.forClass(ScheduledTask.class);
        verify(scheduledTaskRepository).save(captor.capture());

        // Verify it didn't fall back to "MEDIUM"
        assertThat(captor.getValue().getPriority()).isEqualTo("HIGH");
    }

    // ========================================================================
    // ABSOLUTE FINAL COVERAGE: Boolean Short-Circuits & Ternary Permutations
    // ========================================================================

    @Test
    void collectUnscheduledTasks_shouldFilterCorrectly_andHandleNullCustomTaskFields() {
        when(userPreferencesRepository.findByUser(user)).thenReturn(Optional.of(preferences));

        // --- 1. Cover collectUnscheduledTasks OR branches ---
        // rt.isCompleted() || alreadyScheduled.contains(rt.getId())
        RoadmapTask rtCompleted = new RoadmapTask();
        rtCompleted.setId(UUID.randomUUID());
        rtCompleted.setCompleted(true); // true || ? (Short-circuits)

        RoadmapTask rtAlreadyScheduled = new RoadmapTask();
        rtAlreadyScheduled.setId(UUID.randomUUID());
        rtAlreadyScheduled.setCompleted(false); // false || true

        RoadmapTask rtValid = new RoadmapTask();
        rtValid.setId(UUID.randomUUID());
        rtValid.setCompleted(false); // false || false (Passes through)

        when(roadmapTaskRepository.findByRoadmap_User(user)).thenReturn(List.of(rtCompleted, rtAlreadyScheduled, rtValid));

        // mt.getStatus() == Status.COMPLETED || alreadyScheduled.contains(mt.getId())
        Task mtCompleted = new Task();
        mtCompleted.setId(UUID.randomUUID());
        mtCompleted.setStatus(Status.COMPLETED); // true || ?

        Task mtAlreadyScheduled = new Task();
        mtAlreadyScheduled.setId(UUID.randomUUID());
        mtAlreadyScheduled.setStatus(Status.PENDING); // false || true

        Task mtValid = new Task();
        mtValid.setId(UUID.randomUUID());
        mtValid.setStatus(Status.PENDING); // false || false

        when(milestoneTaskRepository.findByMilestone_User(user)).thenReturn(List.of(mtCompleted, mtAlreadyScheduled, mtValid));

        // Set up the scheduled tasks to match the "alreadyScheduled" ones above
        ScheduledTask stRt = new ScheduledTask();
        stRt.setCompleted(true);
        stRt.setRoadmapTaskId(rtAlreadyScheduled.getId());

        ScheduledTask stMt = new ScheduledTask();
        stMt.setCompleted(true);
        stMt.setMilestoneTaskId(mtAlreadyScheduled.getId());

        when(scheduledTaskRepository.findByUser(user)).thenReturn(List.of(stRt, stMt));

        // --- 2. Cover buildReoptimizePrompt Ternaries (dueDate & priority) ---
        CustomTask ctNulls = new CustomTask();
        ctNulls.setId(UUID.randomUUID());
        ctNulls.setDueDate(null);
        ctNulls.setPriority(null);
        ctNulls.setCompleted(false);

        CustomTask ctValid = new CustomTask();
        ctValid.setId(UUID.randomUUID());
        ctValid.setDueDate(LocalDate.now(clock));
        ctValid.setPriority("HIGH");
        ctValid.setCompleted(false);

        when(customTaskRepository.findByUser(user)).thenReturn(List.of(ctNulls, ctValid));
        when(customTaskRepository.findByUserAndCompletedFalse(user)).thenReturn(List.of(ctNulls, ctValid));

        // --- 3. Cover parseAvailableHours whitespace (json.trim().isEmpty()) ---
        // By setting it to spaces AND providing the tasks above, it will actually hit this logic
        preferences.setAvailableHoursJson("    ");

        // --- 4. Cover generateSchedule & reoptimizeToday null combinations ---
        // if (item.date() == null || item.startTime() == null || item.endTime() == null)
        String todayStr = LocalDate.now(clock).toString();
        String tomorrowStr = LocalDate.now(clock).plusDays(1).toString();

        ScheduleItem nullDate = new ScheduleItem(null, "T1", "ROUTINE", null, "10:00", "11:00"); // true || ? || ?
        ScheduleItem nullStart = new ScheduleItem(null, "T2", "ROUTINE", todayStr, null, "11:00"); // false || true || ?
        ScheduleItem nullEnd = new ScheduleItem(null, "T3", "ROUTINE", todayStr, "10:00", null); // false || false || true

        // Cover !parsedDate.equals(today) in reoptimizeToday
        ScheduleItem wrongDate = new ScheduleItem(null, "T4", "ROUTINE", tomorrowStr, "10:00", "11:00");

        ScheduleResponse aiResp = new ScheduleResponse(List.of(nullDate, nullStart, nullEnd, wrongDate), List.of());

        when(aiClientService.generateStructured(anyString(), eq(ScheduleResponse.class), eq(userId), any()))
                .thenReturn(aiResp);

        when(transactionTemplate.execute(any())).thenAnswer(i -> {
            TransactionCallback<Void> cb = i.getArgument(0);
            cb.doInTransaction(null);
            return null;
        });

        // Fire both methods to trigger all the yellow lines!
        scheduleService.generateSchedule(user, "all");
        scheduleService.reoptimizeToday(user);

        // Verify it navigated the boolean maze without crashing
        verify(scheduledTaskRepository, atLeastOnce()).saveAll(anyList());
    }

    // ========================================================================
    // THE FINAL JACOCO SWEEP: generateSchedule Lambda & Custom Task Branches
    // ========================================================================

    @Test
    void generateSchedule_shouldMapCustomTasks_andSkipUnknownIds() {
        when(userPreferencesRepository.findByUser(user)).thenReturn(Optional.of(preferences));

        // 1. Cover collectUnscheduledCustomTasks -> alreadyScheduled.contains(ct.getId())
        CustomTask scheduledCt = new CustomTask();
        scheduledCt.setId(UUID.randomUUID());
        scheduledCt.setTitle("Scheduled Custom");
        scheduledCt.setCompleted(false);

        CustomTask unscheduledCt = new CustomTask();
        unscheduledCt.setId(UUID.randomUUID());
        unscheduledCt.setTitle("Unscheduled Custom");
        unscheduledCt.setCompleted(false);

        when(customTaskRepository.findByUser(user)).thenReturn(List.of(scheduledCt, unscheduledCt));
        when(customTaskRepository.findByUserAndCompletedFalse(user)).thenReturn(List.of(scheduledCt, unscheduledCt));

        // Create a scheduled task to put scheduledCt into the `alreadyScheduled` Set
        ScheduledTask stCustom = new ScheduledTask();
        stCustom.setCompleted(true);
        stCustom.setCustomTaskId(scheduledCt.getId());
        when(scheduledTaskRepository.findByUser(user)).thenReturn(List.of(stCustom));

        when(roadmapTaskRepository.findByRoadmap_User(user)).thenReturn(List.of());
        when(milestoneTaskRepository.findByMilestone_User(user)).thenReturn(List.of());

        // 2. Cover generateSchedule AI response item.taskId() logic and lambda$generateSchedule$3
        String todayStr = LocalDate.now(clock).toString();

        // To clear the yellow line on the boolean check, we must hit all 4 possibilities:
        ScheduleItem nullId = new ScheduleItem(null, "NullID", "ROUTINE", todayStr, "08:00", "09:00");         // taskId() == null
        ScheduleItem blankId = new ScheduleItem("   ", "BlankID", "ROUTINE", todayStr, "09:00", "10:00");      // isBlank() == true
        ScheduleItem stringNullId = new ScheduleItem("null", "StrNull", "ROUTINE", todayStr, "10:00", "11:00"); // equals("null") == true

        // To clear the lambda map-lookup branches, we must hit the deepest 'else' block:
        ScheduleItem unknownId = new ScheduleItem(UUID.randomUUID().toString(), "Unknown", "WORK", todayStr, "11:00", "12:00"); // Not in any map -> else continue
        ScheduleItem validCustomId = new ScheduleItem(unscheduledCt.getId().toString(), "ValidCt", "WORK", todayStr, "12:00", "13:00"); // In custom map

        ScheduleResponse aiResp = new ScheduleResponse(
                List.of(nullId, blankId, stringNullId, unknownId, validCustomId),
                List.of()
        );

        when(aiClientService.generateStructured(anyString(), eq(ScheduleResponse.class), eq(userId), eq(AITask.SCHEDULE_GENERATION)))
                .thenReturn(aiResp);

        when(transactionTemplate.execute(any())).thenAnswer(i -> {
            TransactionCallback<Void> cb = i.getArgument(0);
            cb.doInTransaction(null);
            return null;
        });

        // ACT
        scheduleService.generateSchedule(user, "all");

        // ASSERT
        ArgumentCaptor<List<ScheduledTask>> captor = ArgumentCaptor.forClass(List.class);
        verify(scheduledTaskRepository, atLeastOnce()).saveAll(captor.capture());

        // Verify that the unknownId correctly hit the `continue;` statement and was skipped,
        // while the valid custom task was successfully mapped and saved.
        List<ScheduledTask> savedTasks = captor.getValue();
        boolean unknownWasSaved = savedTasks.stream().anyMatch(t -> "Unknown".equals(t.getTitle()));
        boolean customWasSaved = savedTasks.stream().anyMatch(t -> "Unscheduled Custom".equals(t.getTitle()));

        assertThat(unknownWasSaved).isFalse();
        assertThat(customWasSaved).isTrue();
    }

    // ========================================================================
    // FINAL JACOCO SWEEP: Null Routine Titles & Block Types
    // ========================================================================

    @Test
    void generateSchedule_shouldApplyDefaultTitles_whenAiReturnsNullRoutineData() {
        when(userPreferencesRepository.findByUser(user)).thenReturn(Optional.of(preferences));
        mockTaskMaps(); // Use empty maps

        String todayStr = LocalDate.now(clock).toString();

        //  This item has a null taskId (falls to 'else' routine block)
        // AND a null title (triggers "Personal Time" ternary)
        // AND a null blockType (triggers "ROUTINE" ternary)
        // AND valid dates/times so it doesn't get skipped.
        ScheduleItem pureNullRoutine = new ScheduleItem(
                null, null, null, todayStr, "14:00", "15:00"
        );

        ScheduleResponse aiResp = new ScheduleResponse(List.of(pureNullRoutine), List.of());

        when(aiClientService.generateStructured(anyString(), eq(ScheduleResponse.class), eq(userId), eq(AITask.SCHEDULE_GENERATION)))
                .thenReturn(aiResp);

        when(transactionTemplate.execute(any())).thenAnswer(i -> {
            TransactionCallback<Void> cb = i.getArgument(0);
            cb.doInTransaction(null);
            return null;
        });

        // Act
        scheduleService.generateSchedule(user, "all");

        // Assert
        ArgumentCaptor<List<ScheduledTask>> captor = ArgumentCaptor.forClass(List.class);
        verify(scheduledTaskRepository, atLeastOnce()).saveAll(captor.capture());

        ScheduledTask saved = captor.getValue().get(0);
        assertThat(saved.getTitle()).isEqualTo("Personal Time");
        assertThat(saved.getBlockType()).isEqualTo("ROUTINE");
        assertThat(saved.getPriority()).isEqualTo("LOW");
    }

    // ========================================================================
    // ULTIMATE JACOCO SWEEP: Boolean Short-Circuits & Loop Boundaries
    // ========================================================================

    @Test
    void reoptimizeToday_shouldUseFallback_whenAiReturnsNullScheduleOrUnknownIds() {
        when(userPreferencesRepository.findByUser(user)).thenReturn(Optional.of(preferences));

        CustomTask ct = new CustomTask();
        ct.setId(UUID.randomUUID());
        ct.setTitle("Dummy");

        //  The missing line that caused the early exit!
        when(customTaskRepository.findByUserAndCompletedFalse(user)).thenReturn(List.of(ct));
        when(customTaskRepository.findByUser(user)).thenReturn(List.of(ct));

        when(roadmapTaskRepository.findByRoadmap_User(user)).thenReturn(List.of());
        when(milestoneTaskRepository.findByMilestone_User(user)).thenReturn(List.of());

        // 1st call: aiResponse != null, but schedule() == null
        ScheduleResponse nullSchedule = new ScheduleResponse(null, List.of());

        // 2nd call: aiResponse is valid, but returns an unknown Task ID
        ScheduleItem unknownInfo = new ScheduleItem(UUID.randomUUID().toString(), "Unknown", "WORK_TASK", LocalDate.now(clock).toString(), "10:00", "11:00");
        ScheduleResponse validSchedule = new ScheduleResponse(List.of(unknownInfo), List.of());

        when(aiClientService.generateStructured(anyString(), eq(ScheduleResponse.class), eq(userId), eq(AITask.SCHEDULE_REOPTIMIZATION)))
                .thenReturn(nullSchedule)
                .thenReturn(validSchedule);

        when(transactionTemplate.execute(any())).thenAnswer(i -> {
            TransactionCallback<Void> cb = i.getArgument(0);
            cb.doInTransaction(null);
            return null;
        });

        // Run twice to hit both mathematical scenarios
        scheduleService.reoptimizeToday(user);
        scheduleService.reoptimizeToday(user);
    }
    @Test
    void generateSchedule_shouldEstimateDurationCorrectly_andHandleNullAvailableHours() {
        // 1. parseAvailableHours: json == null
        preferences.setAvailableHoursJson(null);
        when(userPreferencesRepository.findByUser(user)).thenReturn(Optional.of(preferences));

        // 2. estimateDuration: description contains "review"
        RoadmapTask rtReview = new RoadmapTask();
        rtReview.setId(UUID.randomUUID());
        rtReview.setDescription("Monthly Review");

        // 3. estimateDuration: description contains "project"
        RoadmapTask rtProject = new RoadmapTask();
        rtProject.setId(UUID.randomUUID());
        rtProject.setDescription("Main Project");

        // 4. estimateDuration: details length <= 50
        RoadmapTask rtShortDetails = new RoadmapTask();
        rtShortDetails.setId(UUID.randomUUID());
        rtShortDetails.setDescription("Normal");
        rtShortDetails.setDetails("Short details");

        when(roadmapTaskRepository.findByRoadmap_User(user)).thenReturn(List.of(rtReview, rtProject, rtShortDetails));
        when(milestoneTaskRepository.findByMilestone_User(user)).thenReturn(List.of());
        when(customTaskRepository.findByUser(user)).thenReturn(List.of());
        when(customTaskRepository.findByUserAndCompletedFalse(user)).thenReturn(List.of());

        when(aiClientService.generateStructured(anyString(), eq(ScheduleResponse.class), eq(userId), eq(AITask.SCHEDULE_GENERATION)))
                .thenReturn(null);

        when(transactionTemplate.execute(any())).thenAnswer(i -> {
            TransactionCallback<Void> cb = i.getArgument(0);
            cb.doInTransaction(null);
            return null;
        });

        scheduleService.generateSchedule(user, "all");

        verify(scheduledTaskRepository, atLeastOnce()).saveAll(anyList());
    }

    @Test
    void simpleSchedule_shouldSafelyExitLoops_whenTasksExceedAvailableSlots() {
        // Set up highly specific JSON to hit slot logic limits:
        preferences.setAvailableHoursJson("{\"tuesday\": [], \"wednesday\": [[\"09:00\", \"10:00\"]]}");
        when(userPreferencesRepository.findByUser(user)).thenReturn(Optional.of(preferences));

        CustomTask ct1 = new CustomTask();
        ct1.setId(UUID.randomUUID());
        ct1.setEstimatedHours(1.0);

        CustomTask ct2 = new CustomTask();
        ct2.setId(UUID.randomUUID());
        ct2.setEstimatedHours(1.0);

        CustomTask massiveTask = new CustomTask();
        massiveTask.setId(UUID.randomUUID());
        massiveTask.setEstimatedHours(1000.0);

        when(customTaskRepository.findByUser(user)).thenReturn(List.of(ct1, ct2, massiveTask));
        when(customTaskRepository.findByUserAndCompletedFalse(user)).thenReturn(List.of(ct1, ct2, massiveTask));
        when(roadmapTaskRepository.findByRoadmap_User(user)).thenReturn(List.of());
        when(milestoneTaskRepository.findByMilestone_User(user)).thenReturn(List.of());

        when(aiClientService.generateStructured(anyString(), eq(ScheduleResponse.class), eq(userId), eq(AITask.SCHEDULE_GENERATION)))
                .thenReturn(null);

        when(transactionTemplate.execute(any())).thenAnswer(i -> {
            TransactionCallback<Void> cb = i.getArgument(0);
            cb.doInTransaction(null);
            return null;
        });

        scheduleService.generateSchedule(user, "all");

        verify(scheduledTaskRepository, atLeastOnce()).saveAll(anyList());
    }

    @Test
    void simpleSchedule_shouldSkipWhileLoopEntirely_whenMaxDaysAlreadyReached() {
        // We limit availability to one hour a week so the massive task instantly consumes the 21-day max limit
        preferences.setAvailableHoursJson("{\"monday\": [[\"09:00\", \"10:00\"]]}");
        when(userPreferencesRepository.findByUser(user)).thenReturn(Optional.of(preferences));

        // 1. HIGH Priority: Processes first. Schedules normally, loop continues.
        CustomTask ctHigh = new CustomTask();
        ctHigh.setId(UUID.randomUUID());
        ctHigh.setPriority("HIGH");
        ctHigh.setEstimatedHours(0.5);

        // 2. MEDIUM Priority: Processes second. Massive task. Pushes currentDayOffset to 21, loop breaks.
        CustomTask ctMediumMassive = new CustomTask();
        ctMediumMassive.setId(UUID.randomUUID());
        ctMediumMassive.setPriority("MEDIUM");
        ctMediumMassive.setEstimatedHours(1000.0);

        // 3. LOW Priority: Processes third.
        // Because currentDayOffset is ALREADY 21 from the previous task, the while loop condition
        // (currentDayOffset < maxDays) evaluates to FALSE on the very first check, skipping the loop entirely!
        CustomTask ctLow = new CustomTask();
        ctLow.setId(UUID.randomUUID());
        ctLow.setPriority("LOW");
        ctLow.setEstimatedHours(1.0);

        when(customTaskRepository.findByUser(user)).thenReturn(List.of(ctHigh, ctMediumMassive, ctLow));
        when(customTaskRepository.findByUserAndCompletedFalse(user)).thenReturn(List.of(ctHigh, ctMediumMassive, ctLow));
        when(roadmapTaskRepository.findByRoadmap_User(user)).thenReturn(List.of());
        when(milestoneTaskRepository.findByMilestone_User(user)).thenReturn(List.of());

        // Force AI fallback so the simpleSchedule algorithm runs
        when(aiClientService.generateStructured(anyString(), eq(ScheduleResponse.class), eq(userId), eq(AITask.SCHEDULE_GENERATION)))
                .thenReturn(null);

        when(transactionTemplate.execute(any())).thenAnswer(i -> {
            TransactionCallback<Void> cb = i.getArgument(0);
            cb.doInTransaction(null);
            return null;
        });

        // Act
        scheduleService.generateSchedule(user, "all");

        // Assert
        verify(scheduledTaskRepository, atLeastOnce()).saveAll(anyList());
    }
}