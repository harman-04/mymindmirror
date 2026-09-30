// src/main/java/com/mymindmirror.backend/service/MilestoneService.java
package com.mymindmirror.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mymindmirror.backend.enums.AITask;
import com.mymindmirror.backend.enums.GamificationAction;
import com.mymindmirror.backend.enums.Status;
import com.mymindmirror.backend.mapper.MilestoneMapper;
import com.mymindmirror.backend.model.Milestone;
import com.mymindmirror.backend.model.Task;
import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.payload.response.GrowthTipParseResponse;
import com.mymindmirror.backend.payload.response.GrowthTipTask;
import com.mymindmirror.backend.payload.response.MilestoneResponse;
import com.mymindmirror.backend.repository.MilestoneRepository;
import com.mymindmirror.backend.repository.TaskRepository;
import com.mymindmirror.backend.service.ai.DynamicAiClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
/**
 * Service class for managing Milestone-related business logic.
 * Handles creating, retrieving, updating, and deleting milestones,
 * ensuring proper user ownership and data integrity.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MilestoneService {


    private final MilestoneRepository milestoneRepository;
    private final TaskRepository taskRepository;
    private final ObjectMapper objectMapper;
    private final DynamicAiClientService aiClientService;
    private final GamificationService gamificationService;
    private final MilestoneMapper milestoneMapper;
    private final TransactionTemplate transactionTemplate;



    /**
     * Creates a new milestone for a specific user.
     * @param user The authenticated user.
     * @param title The title of the milestone.
     * @param description The description of the milestone.
     * @param dueDate The optional due date for the milestone.
     * @return The created Milestone entity.
     */
    @Transactional
    public Milestone createMilestone(User user, String title, String description, LocalDate dueDate) {
        log.info("Creating new milestone for user {}: {}", user.getUsername(), title);
        Milestone milestone = new Milestone(user, title, description, dueDate);
        milestone.setStatus(Status.PENDING); // New milestones start as PENDING
        gamificationService.recordActivity(user, GamificationAction.MILESTONE_CREATE);
        return milestoneRepository.save(milestone);
    }

    /**
     * Retrieves a specific milestone by its ID for a given user.
     * Ensures that the milestone belongs to the authenticated user.
     * @param milestoneId The ID of the milestone.
     * @param user The authenticated user.
     * @return An Optional containing the Milestone if found and owned by the user.
     */
    @Transactional(readOnly = true)
    public Optional<Milestone> getMilestoneByIdForUser(UUID milestoneId, User user) {
        log.info("Fetching milestone {} for user {}", milestoneId, user.getUsername());
        List<Milestone> milestones = milestoneRepository.findByIdAndUser(milestoneId, user);
        if (milestones.isEmpty()) {
            log.warn("Milestone {} not found or not owned by user {}", milestoneId, user.getUsername());
            return Optional.empty();
        }
        Milestone milestone = milestones.get(0);
        // Force initialization of the User proxy while session is open
        Hibernate.initialize(milestone.getUser());
        // Also initialize tasks if they are lazy (but they are already EAGER in your model)
        Hibernate.initialize(milestone.getTasks());
        return Optional.of(milestone);
    }
    /**
     * Updates an existing milestone.
     * Ensures that the milestone belongs to the authenticated user.
     * @param milestoneId The ID of the milestone to update.
     * @param user The authenticated user.
     * @param newTitle The new title (optional).
     * @param newDescription The new description (optional).
     * @param newDueDate The new due date (optional).
     * @param newStatus The new status (optional).
     * @return The updated Milestone entity.
     * @throws ResponseStatusException with 404 if the milestone is not found or not owned by the user.
     */
    @Transactional
    public Milestone updateMilestone(UUID milestoneId, User user,
                                     String newTitle, String newDescription,
                                     LocalDate newDueDate, Status newStatus) {
        log.info("Updating milestone {} for user {}", milestoneId, user.getUsername());

        // Fetch the milestone directly inside the transaction
        List<Milestone> milestones = milestoneRepository.findByIdAndUser(milestoneId, user);
        if (milestones.isEmpty()) {
            log.warn("Milestone {} not found or not owned by user {}", milestoneId, user.getUsername());
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Milestone not found or not owned by user.");
        }
        Milestone existingMilestone = milestones.get(0);

        // Apply updates – we do NOT need to initialize lazy collections here
        if (newTitle != null && !newTitle.trim().isEmpty()) {
            existingMilestone.setTitle(newTitle);
        }
        if (newDescription != null) {
            existingMilestone.setDescription(newDescription);
        }
        if (newDueDate != null) {
            existingMilestone.setDueDate(newDueDate);
        }
        if (newStatus != null) {
            existingMilestone.setStatus(newStatus);
        }

        return milestoneRepository.save(existingMilestone);
    }

    /**
     * Deletes a milestone.
     * Ensures that the milestone belongs to the authenticated user.
     * @param milestoneId The ID of the milestone to delete.
     * @param user The authenticated user.
     * @throws ResponseStatusException with 404 if the milestone is not found or not owned by the user.
     */
    @Transactional
    public void deleteMilestone(UUID milestoneId, User user) {
        log.info("Deleting milestone {} for user {}", milestoneId, user.getUsername());
        Milestone existingMilestone = getMilestoneByIdForUser(milestoneId, user)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Milestone not found or not owned by user."));

        milestoneRepository.delete(existingMilestone);
        log.info("Milestone {} deleted successfully.", milestoneId);
    }

    /**
     * Updates the status of a milestone based on its tasks' completion.
     * This method can be called after a task's status changes.
     * @param milestoneId The ID of the milestone to update.
     */
    @Transactional
    public void updateMilestoneStatusBasedOnTasks(UUID milestoneId) {
        Optional<Milestone> milestoneOptional = milestoneRepository.findById(milestoneId);
        milestoneOptional.ifPresent(milestone -> {
            long totalTasks = milestone.getTasks().size();
            long completedTasks = milestone.getTasks().stream()
                    .filter(task -> task.getStatus() == Status.COMPLETED)
                    .count();

            if (totalTasks == 0) {
                milestone.setStatus(Status.PENDING); // Or PENDING_NO_TASKS
            } else if (completedTasks == totalTasks) {
                milestone.setStatus(Status.COMPLETED);
            } else if (completedTasks > 0) {
                milestone.setStatus(Status.IN_PROGRESS);
            } else {
                milestone.setStatus(Status.PENDING);
            }

            // Check for overdue status
            if (milestone.getDueDate() != null && milestone.getDueDate().isBefore(LocalDate.now()) && milestone.getStatus() != Status.COMPLETED) {
                milestone.setStatus(Status.OVERDUE);
            }
            milestoneRepository.save(milestone);
            log.info("Milestone {} status updated to {}.", milestoneId, milestone.getStatus());
        });
    }

    public Milestone getOrCreateMilestoneByTitle(User user, String title) {
        // Check if milestone with exact title already exists
        List<Milestone> existing = milestoneRepository.findByUserOrderByCreationDateDesc(user);
        for (Milestone m : existing) {
            if (m.getTitle().equals(title)) {
                return m;
            }
        }
        // Otherwise create new
        return createMilestone(user, title, "Auto-generated from roadmap", null);
    }


    /**
     * Parses a growth tip using AI and creates corresponding tasks.
     * AI call is outside the transaction to prevent holding database connections.
     */

public List<Task> importGrowthTipAsTask(User user, String tipText) {
    // 1. Call AI (outside any transaction).
    //    Let DynamicAiClientService's key-aware 503 propagate directly —
    //    it already knows whether the shared key or the user's own key was exhausted.
    String prompt = buildParseGrowthTipPrompt(tipText);
    GrowthTipParseResponse response = aiClientService.generateStructured(
            prompt, GrowthTipParseResponse.class, user.getId(), AITask.PARSE_GROWTH_TIP);

    if (response == null || response.tasks() == null || response.tasks().isEmpty()) {
        throw new RuntimeException("No tasks extracted from growth tip");
    }

        // 2. Perform all DB operations in a short transaction
        return transactionTemplate.execute(status -> {
            // a) Get or create the milestone
            Milestone milestone = getOrCreateMilestoneByTitle(user, "AI Growth Tips");

            // b) Build task list
            List<Task> tasksToSave = new ArrayList<>();
            for (GrowthTipTask taskData : response.tasks()) {
                Task task = new Task();
                task.setMilestone(milestone);
                task.setDescription(taskData.title());
                task.setDetails(taskData.description() != null ? taskData.description() : "");
                task.setStatus(Status.PENDING);
                task.setCreationTimestamp(LocalDateTime.now());
                task.setDueDate(LocalDate.now().plusDays(3));

                if (taskData.subtasks() != null && !taskData.subtasks().isEmpty()) {
                    try {
                        String subtasksJson = objectMapper.writeValueAsString(taskData.subtasks());
                        task.setSubtasksJson(subtasksJson);
                    } catch (JsonProcessingException e) {
                        log.warn("Failed to serialize subtasks for task {}", taskData.title(), e);
                        task.setSubtasksJson("[]");
                    }
                } else {
                    task.setSubtasksJson(null);
                }
                tasksToSave.add(task);
            }

            // c) Batch save all tasks
            List<Task> savedTasks = taskRepository.saveAll(tasksToSave);

            // d) Update milestone status based on new tasks
            updateMilestoneStatusBasedOnTasks(milestone.getId());

            // e) Return the saved tasks
            return savedTasks;
        });
    }

    private String buildParseGrowthTipPrompt(String tipText) {
        return String.format("""
    The following text is a self‑help / growth tip. It may be written in any language or mix of languages (e.g., Hinglish, English, Hindi, etc.).
    **Language & Style Instruction:**
    - Detect the language(s) and style (casual, formal, motivational) of the tip text.
    - Generate the output tasks in the **same language(s) and style** as the tip text. Preserve any code‑switching (e.g., Hinglish).

    Extract from the tip text:
    - A list of **concrete, actionable tasks** (3-5 items).
    Each task should have:
      - A short title (max 8 words) in the same language/style
      - A detailed description (2-3 sentences explaining the purpose, in the same language/style)
      - A list of 2-4 micro‑subtasks (the actual steps to complete the task, in the same language/style)

    Return ONLY valid JSON with structure:
    {
        "tasks": [
            {
                "title": "task title",
                "description": "detailed explanation",
                "subtasks": ["step 1", "step 2", ...]
            },
            ...
        ]
    }

    Tip text:
    %s
    """, tipText);
    }

    @Transactional(readOnly = true)
    public List<MilestoneResponse> getAllMilestonesForUserAsDTO(User user) {
        List<Milestone> milestones = milestoneRepository.findByUserOrderByCreationDateDesc(user);
        return milestoneMapper.toResponseList(milestones);
    }

    public MilestoneResponse getMilestoneResponseById(UUID milestoneId, User user) {
        Milestone milestone = getMilestoneByIdForUser(milestoneId, user)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Milestone not found"));
        return milestoneMapper.toResponse(milestone);
    }

    public MilestoneResponse createMilestoneAsDTO(User user, String title, String description, LocalDate dueDate) {
        Milestone milestone = createMilestone(user, title, description, dueDate);
        return milestoneMapper.toResponse(milestone);
    }

    public MilestoneResponse updateMilestoneAsDTO(UUID milestoneId, User user,
                                                  String title, String description,
                                                  LocalDate dueDate, Status status) {
        Milestone updated = updateMilestone(milestoneId, user, title, description, dueDate, status);
        return milestoneMapper.toResponse(updated);
    }
}
