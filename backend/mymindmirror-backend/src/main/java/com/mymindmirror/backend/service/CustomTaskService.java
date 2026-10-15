package com.mymindmirror.backend.service;

import com.mymindmirror.backend.enums.GamificationAction;
import com.mymindmirror.backend.mapper.CustomTaskMapper;
import com.mymindmirror.backend.model.CustomTask;
import com.mymindmirror.backend.model.ScheduledTask;
import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.payload.request.CustomTaskRequest;
import com.mymindmirror.backend.payload.response.CustomTaskResponse;
import com.mymindmirror.backend.repository.CustomTaskRepository;
import com.mymindmirror.backend.repository.ScheduledTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomTaskService {

    private final CustomTaskRepository customTaskRepository;
    private final ScheduledTaskRepository scheduledTaskRepository;
    private final GamificationService gamificationService;
    private final CustomTaskMapper customTaskMapper;

    @Transactional(readOnly = true)
    public List<CustomTaskResponse> getUserTasks(User user) {
        List<CustomTask> tasks = customTaskRepository.findByUserOrderByCreatedAtDesc(user);
        return customTaskMapper.toResponseList(tasks);
    }

    @Transactional
    public CustomTaskResponse createCustomTask(User user, CustomTaskRequest request) {
        CustomTask task = customTaskMapper.toEntity(request);
        task.setUser(user);
        task.setCreatedAt(LocalDate.now());

        // Set defaults if needed (Mapper can handle this too, but we keep it safe)
        if (task.getEstimatedHours() == null) task.setEstimatedHours(1.0);
        if (task.getPriority() == null) task.setPriority("MEDIUM");
        if (task.getDescription() == null) task.setDescription("");

        CustomTask saved = customTaskRepository.save(task);
        gamificationService.recordActivity(user, GamificationAction.TASK_CREATE);

        return customTaskMapper.toResponse(saved);
    }

    @Transactional
    public CustomTaskResponse updateCustomTask(UUID id, User user, CustomTaskRequest request) {
        CustomTask existing = customTaskRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Custom task not found"));

        if (!existing.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Custom task not found");
        }

        boolean wasCompleted = existing.isCompleted();

        // Update fields with mapper (ignores id, user, createdAt)
        customTaskMapper.updateEntity(existing, request);

        CustomTask saved = customTaskRepository.save(existing);

        // --- SYNC LOGIC: Update calendar tasks ---
        List<ScheduledTask> linkedTasks = scheduledTaskRepository.findByCustomTaskId(saved.getId());
        for (ScheduledTask st : linkedTasks) {
            st.setTitle(saved.getTitle());
            st.setPriority(saved.getPriority());
            st.setCompleted(saved.isCompleted());
            scheduledTaskRepository.save(st);
        }

        // --- GAMIFICATION SYNC ---
        if (!wasCompleted && saved.isCompleted()) {
            gamificationService.recordActivity(user, GamificationAction.TASK);
        }

        return customTaskMapper.toResponse(saved);
    }

    @Transactional
    public void deleteCustomTask(UUID id, User user) {
        CustomTask task = customTaskRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Custom task not found"));

        if (!task.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Custom task not found");
        }

        // Delete all corresponding tasks from the calendar!
        List<ScheduledTask> linkedTasks = scheduledTaskRepository.findByCustomTaskId(id);
        scheduledTaskRepository.deleteAll(linkedTasks);

        customTaskRepository.delete(task);
    }
}