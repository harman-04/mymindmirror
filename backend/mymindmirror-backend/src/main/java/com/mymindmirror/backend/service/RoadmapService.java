package com.mymindmirror.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mymindmirror.backend.enums.AITask;
import com.mymindmirror.backend.enums.GamificationAction;
import com.mymindmirror.backend.mapper.RoadmapMapper;
import com.mymindmirror.backend.model.*;
import com.mymindmirror.backend.payload.response.*;
import com.mymindmirror.backend.repository.RoadmapRepository;
import com.mymindmirror.backend.repository.RoadmapTaskRepository;
import com.mymindmirror.backend.service.ai.DynamicAiClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoadmapService {

    private final RoadmapRepository roadmapRepository;
    private final UserService userService;
    private final ObjectMapper objectMapper;
    private final MilestoneService milestoneService;
    private final TaskService taskService;
    private final RoadmapTaskRepository taskRepository;
    private final GamificationService gamificationService;
    private final DynamicAiClientService aiClientService;
    private final RoadmapMapper roadmapMapper;
    private final TransactionTemplate transactionTemplate;




public Roadmap generateRoadmap(User user, String goal, Integer timeframeWeeks,
                               Integer timeframeValue, String timeframeUnit)
       {
    // Convert to weeks
    int weeks;
    if (timeframeWeeks != null) {
        weeks = timeframeWeeks;
        timeframeValue = weeks;
        timeframeUnit = "WEEKS";
    } else if (timeframeValue != null && timeframeUnit != null) {
        weeks = convertToWeeks(timeframeValue, timeframeUnit);
    } else {
        weeks = 4;
        timeframeValue = 4;
        timeframeUnit = "WEEKS";
    }

    UserRoadmapPreferences prefs = userService.getRoadmapPreferences(user);
    int weeksToGenerate = Math.min(weeks, 12);
    String prompt = buildRoadmapPrompt(goal, weeks, weeksToGenerate,
            prefs.getDifficulty(), prefs.getLanguagePreference(),
            prefs.getLearningStyle(), prefs.getHoursPerWeek(), prefs.isAvoidWeekends());

    // Call AI
    RoadmapGenerateResponse aiResponse = callAiForRoadmap(prompt, user, weeks);
    boolean isFallback = (aiResponse == null || aiResponse.tasks() == null || aiResponse.tasks().isEmpty());
    if (isFallback) {
        aiResponse = createFallbackRoadmapResponse(goal, weeks);
    }

    // Build entity
    Roadmap roadmap = buildRoadmapEntity(user, aiResponse, goal, weeks,
            timeframeValue, timeframeUnit, isFallback, weeksToGenerate);

    // Save transactional

    return transactionTemplate.execute(status -> saveRoadmapInTransaction(roadmap, user));

}

    // 2. AI call separated (non-transactional)
    private RoadmapGenerateResponse callAiForRoadmap(String prompt, User user, int weeks) {
        try {
            return aiClientService.generateStructured(prompt, RoadmapGenerateResponse.class,
                    user.getId(), AITask.ROADMAP_INITIAL);
        } catch (Exception e) {
            log.error("AI call failed, will use fallback", e);
            return null;
        }
    }

    // 3. Build entity (no DB operations)
    private Roadmap buildRoadmapEntity(User user, RoadmapGenerateResponse aiResponse, String goal,
                                       int weeks, Integer timeframeValue, String timeframeUnit,
                                       boolean isFallback, int weeksToGenerate) {
        Roadmap roadmap = new Roadmap(user, aiResponse.title(), goal, weeks);
        roadmap.setOriginalDurationValue(timeframeValue);
        roadmap.setOriginalDurationUnit(timeframeUnit);
        roadmap.setGeneratedWeeks(isFallback ? 0 : weeksToGenerate);

        // Ensure collections
        List<RoadmapGenerateResponse.Task> tasksToMap = aiResponse.tasks() != null ? aiResponse.tasks() : new ArrayList<>();
        List<RoadmapGenerateResponse.Resource> resourcesToMap = aiResponse.resources() != null ? aiResponse.resources() : new ArrayList<>();
        List<RoadmapGenerateResponse.Milestone> milestonesToMap = aiResponse.milestones() != null ? aiResponse.milestones() : new ArrayList<>();

        tasksToMap.forEach(taskDto -> {
            RoadmapTask task = roadmapMapper.toEntity(taskDto);
            task.setRoadmap(roadmap);
            task.setCompleted(false);
            roadmap.getTasks().add(task);
        });

        resourcesToMap.forEach(resDto -> {
            RoadmapResource res = new RoadmapResource();
            res.setRoadmap(roadmap);
            res.setName(resDto.name());
            res.setUrl(resDto.url());
            res.setType(resDto.type());
            roadmap.getResources().add(res);
        });

        milestonesToMap.forEach(milDto -> {
            RoadmapMilestone mil = new RoadmapMilestone();
            mil.setRoadmap(roadmap);
            mil.setName(milDto.name());
            mil.setWeekNumber(milDto.week());
            roadmap.getMilestones().add(mil);
        });

        return roadmap;
    }


    public Roadmap saveRoadmapInTransaction(Roadmap roadmap, User user) {
        roadmap.setStatus("ACTIVE");
        gamificationService.recordActivity(user, GamificationAction.ROADMAP_GENERATE);
        return roadmapRepository.save(roadmap);
    }

    private String buildRoadmapPrompt(String goal, int weeks, int weeksToGenerate, String difficulty, String language,
                                      String learningStyle, int hoursPerWeek, boolean avoidWeekends) {
        return String.format("""
    You are an expert mentor. Create a detailed, actionable JSON roadmap for the goal: "%s" within %d weeks.
    **You only need to generate detailed tasks for the first %d weeks**.
    For each of those weeks, provide 3-5 daily tasks (Monday‑Friday, day 1-5) with descriptions, details, and subtasks.

    **User Preferences:**
    - Difficulty: %s
    - Language: %s
    - Learning style: %s
    - Hours/week: %d
    - Avoid weekends: %s

    **Output Requirements (STRICT):**
    - JSON must contain these fields at the root level: title, durationWeeks, tasks, resources, milestones.
    - "tasks" MUST be a flat array of objects. Do NOT nest tasks inside "phases" or "weeks".
    - Each task object must have:
      - week (integer, 1..%d)
      - day (integer 1-5 for daily tasks, or null for weekly review)
      - description (short action)
      - details (longer instructions)
      - subtasks (array of strings)
      - type ("daily" or "weekly")
    - "resources": array of objects with "name", "url", "type" (relevant to the goal).
    - "milestones": array of objects with "name", "week", "criteria" (descriptive, e.g. "Understand Spring AI core concepts").
    - Use real resource URLs.
    - Return ONLY valid JSON, no extra text.
    """, goal, weeks, weeksToGenerate, difficulty, language, learningStyle, hoursPerWeek, avoidWeekends, weeksToGenerate);
    }

    private int convertToWeeks(int value, String unit) {
        return switch (unit.toUpperCase()) {
            case "DAYS" -> Math.max(1, value / 7);
            case "MONTHS" -> value * 4;
            case "YEARS" -> value * 52;
            default -> value;
        };
    }

    private RoadmapGenerateResponse createFallbackRoadmapResponse(String goal, int weeks) {
        // Create tasks
        List<RoadmapGenerateResponse.Task> tasks = new ArrayList<>();
        tasks.add(createTask(1, 1, "Research the best resources for " + goal, "daily", "Explore official docs and tutorials.", List.of("Find 3 resources", "Bookmark them")));
        tasks.add(createTask(2, 1, "Set up your learning environment", "daily", "Install required software.", List.of()));
        tasks.add(createTask(3, 1, "Complete first module on " + goal, "daily", "Follow a structured course.", List.of()));
        tasks.add(createTask(4, 1, "Practice with a small exercise", "daily", "Apply what you learned.", List.of()));
        tasks.add(createTask(5, 1, "Review and plan next week", "daily", "Reflect on progress.", List.of()));

        // Create resources
        List<RoadmapGenerateResponse.Resource> resources = new ArrayList<>();
        resources.add(createResource("Google: " + goal + " tutorials", "https://www.google.com/search?q=" + goal.replace(" ", "+") + "+tutorial", "search"));
        resources.add(createResource("YouTube: " + goal + " for beginners", "https://www.youtube.com/results?search_query=" + goal.replace(" ", "+") + "+beginner", "video"));
        resources.add(createResource("Coursera: " + goal + " courses", "https://www.coursera.org/search?query=" + goal.replace(" ", "+"), "course"));

        // Create milestones
        List<RoadmapGenerateResponse.Milestone> milestones = new ArrayList<>();
        milestones.add(createMilestone("Foundation of " + goal + " completed", Math.max(1, weeks / 3)));
        milestones.add(createMilestone("First project finished", Math.max(1, 2 * weeks / 3)));
        milestones.add(createMilestone("Ready to advance in " + goal, weeks));

        //  Return record using constructor (no setters!)
        return new RoadmapGenerateResponse(
                "Your Personalized Roadmap to " + goal,
                weeks,
                tasks,
                resources,
                milestones,
                true // isFallback
        );
    }

    private RoadmapGenerateResponse.Task createTask(Integer day, Integer week, String description, String type, String details, List<String> subtasks) {
        return new RoadmapGenerateResponse.Task(day, week, description, type, details, subtasks);
    }

    private RoadmapGenerateResponse.Resource createResource(String name, String url, String type) {
        return new RoadmapGenerateResponse.Resource(name, url, type);
    }

    private RoadmapGenerateResponse.Milestone createMilestone(String name, Integer week) {
        return new RoadmapGenerateResponse.Milestone(name, week);
    }



    @Transactional(readOnly = true)
    public List<RoadmapResponse> getUserRoadmaps(User user) {
        List<Roadmap> roadmaps = roadmapRepository.findByUserOrderByCreatedAtDesc(user);
        return roadmapMapper.toResponseList(roadmaps); //  Clean one-liner replacing stream & map
    }

    @Transactional
    public void deleteRoadmap(UUID roadmapId, User user) {
        Roadmap roadmap = roadmapRepository.findById(roadmapId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,"Roadmap not found"));

        if (!roadmap.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Roadmap not found");
        }
        roadmapRepository.delete(roadmap);
    }

    // Called from RoadmapController.importTaskToMilestone — ImportTaskRequest validates non-null UUIDs.
    @Transactional
    public void importTaskToMilestone(UUID roadmapId, UUID taskId, User user) {
        Roadmap roadmap = roadmapRepository.findById(roadmapId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,"Roadmap not found"));
        if (!roadmap.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Roadmap not found");
        }
        RoadmapTask task = roadmap.getTasks().stream()
                .filter(t -> t.getId().equals(taskId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Task not found in roadmap"));

        // Mark as imported
        task.setImportedToMilestone(true);
        taskRepository.save(task);

        String milestoneTitle = "Roadmap: " + roadmap.getTitle();
        Milestone milestone = milestoneService.getOrCreateMilestoneByTitle(user, milestoneTitle);

        // Copy details and subtasks from RoadmapTask to Task
        String details = task.getDetails();
        String subtasksJson = task.getSubtasks();  // note: RoadmapTask uses "subtasks", Task uses "subtasksJson"

        //  Set a default due date: 7 days from now
        LocalDate defaultDueDate = LocalDate.now().plusDays(7);

        taskService.createTaskWithRoadmapLink(
                milestone.getId(), user,
                task.getDescription(),
                defaultDueDate,
                task.getId(),
                details,
                subtasksJson
        );
    }

    @Transactional
    public void toggleTaskCompletion(UUID taskId, User user) {
        RoadmapTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,"Task not found"));
        if (!task.getRoadmap().getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found");
        }
        boolean wasCompleted = task.isCompleted();
        task.setCompleted(!wasCompleted);
        taskRepository.save(task);

        // If task was completed (just became true), update gamification
        if (!wasCompleted) {   // task.isCompleted() is guaranteed true here

//            gamificationService.recordActivity(user, "TASK");
            gamificationService.recordActivity(user, GamificationAction.TASK);
            // Check if all tasks in this roadmap are now completed
            Roadmap roadmap = task.getRoadmap();
            boolean allCompleted = roadmap.getTasks().stream().allMatch(RoadmapTask::isCompleted);
            if (allCompleted) {
                gamificationService.awardRoadmapCompletedBadge(user);
            }
        }
    }



    public RoadmapResponse continueRoadmap(UUID roadmapId, User user) {
        // 1. Load roadmap (with eager tasks via @EntityGraph)
        Roadmap roadmap = roadmapRepository.findById(roadmapId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,"Roadmap not found"));
        if (!roadmap.getUser().getId().equals(user.getId())) {
           throw new   ResponseStatusException(HttpStatus.NOT_FOUND, "Roadmap not found");
        }


        // 2. Get completed tasks


        List<String> completedTasks = roadmap.getTasks().stream()
                .filter(RoadmapTask::isCompleted)
                .map(RoadmapTask::getDescription)
                .collect(Collectors.toList());

        if (completedTasks.isEmpty()) {
            throw new IllegalArgumentException("No completed tasks yet. Complete some tasks before continuing.");
        }

        // 3. Determine current week
        int currentWeek = roadmap.getTasks().stream()
                .mapToInt(t -> t.getWeekNumber() != null ? t.getWeekNumber() : 1)
                .max()
                .orElse(0);

        // 4. Generate 3 weeks
        int weeksToGenerate = 3;
        int startWeek = currentWeek + 1;
        int endWeek = startWeek + weeksToGenerate - 1;

        // 5. Build prompt
        String prompt = buildContinueByProgressPrompt(
                roadmap.getDescription(),
                completedTasks,
                roadmap.getTitle(),
                currentWeek,
                startWeek,
                endWeek
        );

        // 6. Call AI (non‑transactional)
        RoadmapContinuationResponse aiResponse = callAiForContinuation(prompt, user);
        if (aiResponse == null || aiResponse.tasks() == null || aiResponse.tasks().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AI couldn't generate the next roadmap steps this time. Please try again in a moment.");
        }

        // 7. Save everything in a fresh transaction
        return transactionTemplate.execute(status -> saveContinuationInTransaction(roadmapId, user, aiResponse.tasks()));


    }

    /**
     * Non‑transactional AI call for roadmap continuation.
     */
    private RoadmapContinuationResponse callAiForContinuation(String prompt, User user) {
        // Let ResponseStatusException from DynamicAiClientService propagate —
        // key-aware 503 message reaches the user.
        return aiClientService.generateStructured(prompt, RoadmapContinuationResponse.class, user.getId(), AITask.ROADMAP_NEXT_STEPS);
    }


    public RoadmapResponse saveContinuationInTransaction(UUID roadmapId, User user, List<RoadmapGenerateResponse.Task> newTaskDtos) {
        Roadmap roadmap = roadmapRepository.findById(roadmapId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,"Roadmap not found"));
        if (!roadmap.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Roadmap not found");
        }

        // Map and add new tasks
        for (RoadmapGenerateResponse.Task taskDto : newTaskDtos) {
            RoadmapTask task = roadmapMapper.toEntity(taskDto);
            task.setRoadmap(roadmap);
            task.setCompleted(false);
            roadmap.getTasks().add(task);
        }

        // Update duration weeks if new tasks extend beyond current duration
        int newMaxWeek = roadmap.getTasks().stream()
                .mapToInt(t -> t.getWeekNumber() != null ? t.getWeekNumber() : 1)
                .max()
                .orElse(roadmap.getDurationWeeks());
        if (newMaxWeek > roadmap.getDurationWeeks()) {
            roadmap.setDurationWeeks(newMaxWeek);
        }

        Roadmap saved = roadmapRepository.save(roadmap);
        return roadmapMapper.toResponse(saved);
    }


    private String buildContinueByProgressPrompt(String goal, List<String> completedTasks, String currentTitle,
                                                 int currentWeek, int startWeek, int endWeek) {
        return String.format("""
    You are an expert mentor continuing a roadmap for the goal: "%s". The roadmap title is "%s".
    The user has completed the following tasks:
    %s

    The roadmap currently has tasks planned up to week %d.
    Now generate detailed, actionable tasks for weeks %d to %d (the next %d weeks).
    Each week should have 3‑5 daily tasks (Monday‑Friday, days 1‑5).

    **IMPORTANT:** 
    - Do NOT repeat tasks that are already completed.
    - The tasks must be for weeks %d, %d, and %d respectively.
    - Continue logically from where the previous weeks ended.

    Return ONLY JSON with the following structure:
    {
        "tasks": [
            {
                "week": %d,
                "day": 1,
                "description": "...",
                "details": "...",
                "subtasks": ["step1", "step2"],
                "type": "daily"
            },
            ...
        ]
    }
    If the goal seems already achieved, return an empty tasks array.
    """, goal, currentTitle, String.join("\n", completedTasks),
                currentWeek, startWeek, endWeek, (endWeek - startWeek + 1),
                startWeek, startWeek+1, startWeek+2,
                startWeek);
    }


    public ElaborationResponseDto elaborateTask(UUID taskId, User user, boolean enhance) {
        // 1. Load task with its roadmap (eager) – outside transaction
        RoadmapTask task = taskRepository.findByIdWithRoadmap(taskId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Task not found"));
        if (!task.getRoadmap().getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found");
        }

        // 2. Build prompt
        String goal = task.getRoadmap().getDescription();
        String prompt = buildElaboratePrompt(goal, task.getDescription(), enhance);

        // 3. Call AI (non‑transactional)
        ElaborationResponse aiResponse = callAiForElaboration(prompt, user);

        // 4. Save and map to DTO inside the transaction
        RoadmapTask savedTask = transactionTemplate.execute(status ->
                saveElaborationInTransaction(taskId, user, aiResponse)
        );

        // 5. Map to DTO using MapStruct
        return roadmapMapper.toElaborationDto(savedTask);
    }



    /**
     * Non‑transactional AI call for task elaboration.
     */
    private ElaborationResponse callAiForElaboration(String prompt, User user) {
        try {
            return aiClientService.generateStructured(prompt, ElaborationResponse.class, user.getId(), AITask.ROADMAP_ELABORATION);
        } catch (Exception e) {
            log.error("AI elaboration failed", e);
            return null; // orchestrator will handle
        }
    }


    public RoadmapTask saveElaborationInTransaction(UUID taskId, User user, ElaborationResponse aiResponse) {
        RoadmapTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,"Task not found"));
        if (!task.getRoadmap().getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found");
        }

        if (aiResponse != null) {
            // Apply AI elaboration
            task.setDetails(aiResponse.details());
            if (aiResponse.subtasks() != null && !aiResponse.subtasks().isEmpty()) {
                try {
                    task.setSubtasks(objectMapper.writeValueAsString(aiResponse.subtasks()));
                } catch (JsonProcessingException e) {
                    log.error("Failed to serialize subtasks", e);
                    task.setSubtasks("[]");
                }
            } else {
                task.setSubtasks("[]");
            }
        } else {
            // Fallback: use task description as details, no subtasks
            task.setDetails(task.getDescription());
            task.setSubtasks("[]");
        }

        RoadmapTask saved = taskRepository.save(task);
        gamificationService.recordActivity(user, GamificationAction.ELABORATE_TASK);
        return saved;
    }

    private String buildElaboratePrompt(String goal, String taskDescription, boolean enhance) {
        if (enhance) {
            return String.format("""
        You are an expert mentor. The user is following a roadmap for the goal: "%s".
        One task in that roadmap is: "%s".
        The user has already seen a basic elaboration and wants an **even more detailed, comprehensive guide**.

        **Language & Style Instruction:**
        - Detect the language(s) and style of the goal and task description.
        - Generate the details and subtasks in the **same language(s) and style**.

        Provide a **very detailed** step‑by‑step explanation, including:
        - Concrete examples
        - Best practices
        - Common pitfalls to avoid
        - A list of 4‑6 actionable subtasks (as an array of strings)
        - Estimated time to complete (in hours, e.g., 3.5)

        Return ONLY valid JSON with this structure:
        {
            "details": "very detailed explanation...",
            "subtasks": ["subtask 1", "subtask 2", ...],
            "estimatedHours": 3.5
        }
        """, goal, taskDescription);
        } else {
            return String.format("""
        You are an expert mentor. The user is following a roadmap for the goal: "%s".
        One task in that roadmap is: "%s".

        **Language & Style Instruction:**
        - Detect the language(s) and style of the goal and task description.
        - Generate the details and subtasks in the **same language(s) and style**.

        Provide a detailed elaboration for this task. Include:
        - A longer, step‑by‑step explanation (details)
        - A list of 2‑4 concrete subtasks (as an array of strings)
        - Estimated time to complete (in hours, e.g., 1.5)

        Return ONLY valid JSON with this structure:
        {
            "details": "step-by-step explanation...",
            "subtasks": ["subtask 1", "subtask 2", ...],
            "estimatedHours": 1.5
        }
        """, goal, taskDescription);
        }
    }



public RoadmapResponse rescheduleRoadmap(UUID roadmapId, User user) {
    // 1. Load roadmap with all tasks (eagerly)
    Roadmap roadmap = roadmapRepository.findById(roadmapId)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Roadmap not found"));
    if (!roadmap.getUser().getId().equals(user.getId())) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Roadmap not found");
    }

    List<RoadmapTask> allTasks = roadmap.getTasks();
    List<String> completedTasks = allTasks.stream()
            .filter(RoadmapTask::isCompleted)
            .map(RoadmapTask::getDescription)
            .collect(Collectors.toList());

    //  GEMINI'S CLEAN APPROACH: Create a unified Map of ID -> Description
    List<Map<String, String>> remainingTasks = allTasks.stream()
            .filter(t -> !t.isCompleted())
            .map(t -> Map.of("taskId", t.getId().toString(), "description", t.getDescription()))
            .collect(Collectors.toList());

    if (remainingTasks.isEmpty()) {
        throw new IllegalArgumentException("All tasks are already completed.");

    }

    // 2. Build prompt
    String prompt = buildReschedulePrompt(
            roadmap.getDescription(),
            roadmap.getDurationWeeks(),
            completedTasks,
            remainingTasks
    );

    // 3. Call AI (non‑transactional)
    RescheduleResponse aiResponse = callAiForReschedule(prompt, user);
    if (aiResponse == null) {
        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "AI couldn't reschedule your roadmap this time. Please try again in a moment.");
    }

    // 4. Save updates in a fresh transaction (No need to pass ID lists anymore!)
    return transactionTemplate.execute(status -> saveRescheduleInTransaction(roadmapId, user, aiResponse));
}


    private RescheduleResponse callAiForReschedule(String prompt, User user) {
        return aiClientService.generateStructured(prompt, RescheduleResponse.class, user.getId(), AITask.ROADMAP_RESCHEDULE);
    }



    /**
     * Transactional save of reschedule updates.
     */
    public RoadmapResponse saveRescheduleInTransaction(UUID roadmapId, User user, RescheduleResponse aiResponse) {
        Roadmap roadmap = roadmapRepository.findById(roadmapId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Roadmap not found"));
        if (!roadmap.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Roadmap not found");
        }

        // 1. Update duration if provided
        if (aiResponse != null && aiResponse.newDurationWeeks() != null && aiResponse.newDurationWeeks() > 0) {
            roadmap.setDurationWeeks(aiResponse.newDurationWeeks());
        }

        // 2. Apply week‑number updates using secure UUIDs
        if (aiResponse != null && aiResponse.tasks() != null && !aiResponse.tasks().isEmpty()) {

            // OPTIMIZED APPROACH: O(1) Map Lookup by String ID
            Map<String, RoadmapTask> taskMap = roadmap.getTasks().stream()
                    .collect(Collectors.toMap(t -> t.getId().toString(), t -> t));

            for (RescheduleResponse.TaskUpdate update : aiResponse.tasks()) {
                String taskIdStr = update.taskId();
                Integer newWeek = update.newWeek();

                if (taskIdStr == null || newWeek == null) {
                    continue;
                }

                RoadmapTask task = taskMap.get(taskIdStr);
                if (task == null) {
                    // Gracefully skip if AI hallucinated an invalid ID
                    log.warn("Task with ID {} not found in roadmap, skipping", taskIdStr);
                    continue;
                }

                task.setWeekNumber(newWeek);
            }
        }

        Roadmap saved = roadmapRepository.save(roadmap);
        return roadmapMapper.toResponse(saved);
    }


    private String buildReschedulePrompt(String goal, int originalDuration,
                                         List<String> completedTasks, List<Map<String, String>> remainingTasks) {

        //  Uses robust JSON serialization for the prompt
        String remainingTasksJson;
        try {
            remainingTasksJson = objectMapper.writeValueAsString(remainingTasks);
        } catch (Exception e) {
            remainingTasksJson = remainingTasks.toString();
        }

        return String.format("""
    You are an expert mentor. The user has a roadmap for the goal: "%s" originally planned for %d weeks.
    They have completed the following tasks: %s
    They still have these remaining tasks:
    %s

    **Language & Style Instruction:**
    - Detect the language(s) and style of the original goal and tasks.
    - Generate the revised schedule notes (newDurationWeeks) in the same language.

    Based on their progress, suggest a **revised weekly schedule** for the remaining tasks.
    Return ONLY JSON with the following structure:
    {
        "newDurationWeeks": integer,
        "tasks": [
            {
                "taskId": "string (You MUST copy the exact taskId string provided in the remaining tasks list)",
                "newWeek": integer (1‑based week number)
            }
        ]
    }
    Only include tasks that need a new week assignment. If no change needed, return empty tasks array.
    """, goal, originalDuration, completedTasks.toString(), remainingTasksJson);
    }



    public RoadmapResponse continueRoadmapBatch(UUID roadmapId, User user, Integer weeksToGenerate) {
        // 1. Load roadmap (with eager tasks via @EntityGraph)
        Roadmap roadmap = roadmapRepository.findById(roadmapId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Roadmap not found"));
        if (!roadmap.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Roadmap not found");
        }

        if (roadmap.getDurationWeeks() == null) {
            // Defensive: DB corruption or legacy row with missing duration.
            // Log and return a clear error instead of crashing the caller.
            log.error("Roadmap {} has null durationWeeks — data integrity issue", roadmapId);
            throw new IllegalArgumentException(
                    "This roadmap is missing duration data. Please delete and regenerate it.");
        }

        if (roadmap.isFullyGenerated()) {
            throw new IllegalArgumentException("Roadmap already fully generated");
        }

        int totalWeeks = roadmap.getDurationWeeks();
        int currentGenerated = roadmap.getGeneratedWeeks() != null ? roadmap.getGeneratedWeeks() : 0;
        int remainingWeeks = totalWeeks - currentGenerated;


        if (remainingWeeks <= 0) {
            throw new IllegalArgumentException("No weeks left to generate");
        }

        int chunkSize = (weeksToGenerate != null && weeksToGenerate > 0) ? weeksToGenerate : Math.min(remainingWeeks, 12);
        int startWeek = currentGenerated + 1;
        int endWeek = Math.min(startWeek + chunkSize - 1, totalWeeks);
        int weeksToGenerateNow = endWeek - startWeek + 1;

        // 2. Build summary of previous weeks for AI context
        StringBuilder previousWeeksSummary = new StringBuilder();
        if (currentGenerated > 0) {
            List<RoadmapTask> previousTasks = roadmap.getTasks().stream()
                    .filter(t -> t.getWeekNumber() != null && t.getWeekNumber() <= currentGenerated)
                    .sorted(Comparator.comparingInt(RoadmapTask::getWeekNumber)
                            .thenComparingInt(t -> t.getDayNumber() != null ? t.getDayNumber() : 0))
                    .collect(Collectors.toList());
            int lastWeek = 0;
            for (RoadmapTask task : previousTasks) {
                if (task.getWeekNumber() != lastWeek) {
                    lastWeek = task.getWeekNumber();
                    previousWeeksSummary.append("Week ").append(lastWeek).append(":\n");
                }
                previousWeeksSummary.append("  - ").append(task.getDescription()).append("\n");
            }
        } else {
            previousWeeksSummary.append("No previous weeks. Start from week 1.");
        }

        UserRoadmapPreferences prefs = userService.getRoadmapPreferences(user);

        // 3. Build prompt
        String prompt = buildContinuationPrompt(
                roadmap.getDescription(),
                currentGenerated,
                weeksToGenerateNow,
                totalWeeks,
                previousWeeksSummary.toString(),
                prefs.getDifficulty(),
                prefs.getLanguagePreference(),
                prefs.getLearningStyle(),
                prefs.getHoursPerWeek(),
                prefs.isAvoidWeekends()
        );


        RoadmapContinuationResponse aiResponse = callAiForBatchContinuation(prompt, user);
        if (aiResponse == null || aiResponse.tasks() == null || aiResponse.tasks().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AI couldn't generate additional roadmap weeks this time. Please try again in a moment.");
        }

        // 5. Save everything in a fresh transaction

        return transactionTemplate.execute(status -> saveContinuationBatchInTransaction(roadmapId, user, startWeek, endWeek, aiResponse.tasks()));
    }



    private RoadmapContinuationResponse callAiForBatchContinuation(String prompt, User user) {
        return aiClientService.generateStructured(prompt, RoadmapContinuationResponse.class, user.getId(), AITask.ROADMAP_EXTENSION);
    }

    /**
     * Deletes existing tasks for the target weeks and adds new ones.
     */
    public RoadmapResponse saveContinuationBatchInTransaction(
            UUID roadmapId,
            User user,
            int startWeek,
            int endWeek,
            List<RoadmapGenerateResponse.Task> newTaskDtos) {

        Roadmap roadmap = roadmapRepository.findById(roadmapId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Roadmap not found"));
        if (!roadmap.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Roadmap not found");
        }

        // 1. Delete existing tasks for the target weeks (to avoid duplicates)
        List<RoadmapTask> tasksToDelete = roadmap.getTasks().stream()
                .filter(t -> t.getWeekNumber() != null && t.getWeekNumber() >= startWeek && t.getWeekNumber() <= endWeek)
                .collect(Collectors.toList());
        roadmap.getTasks().removeAll(tasksToDelete);
        taskRepository.deleteAll(tasksToDelete);

        // 2. Map and add new tasks
        for (RoadmapGenerateResponse.Task taskDto : newTaskDtos) {
            RoadmapTask task = roadmapMapper.toEntity(taskDto);
            task.setRoadmap(roadmap);
            task.setCompleted(false);
            roadmap.getTasks().add(task);
        }

        // 3. Update generatedWeeks to the endWeek
        roadmap.setGeneratedWeeks(endWeek);

        Roadmap saved = roadmapRepository.save(roadmap);
        return roadmapMapper.toResponse(saved);
    }

    private String buildContinuationPrompt(String goal, int currentWeek, int weeksToGenerate, int totalWeeks,
                                           String previousSummary, String difficulty, String language,
                                           String learningStyle, int hoursPerWeek, boolean avoidWeekends) {
        int startWeek = currentWeek + 1;
        int endWeek = startWeek + weeksToGenerate - 1;
        return String.format("""
    You are an expert mentor continuing a roadmap for the goal: "%s".

    The roadmap spans %d weeks. Weeks 1 to %d have already been planned.

    **Topics already covered in previous weeks:**
    %s

    Now generate detailed, actionable tasks for weeks %d to %d.
    Each week should have 3‑5 daily tasks (Monday‑Friday, days 1‑5).
    **IMPORTANT:** Do NOT repeat topics that have already been covered in the previous weeks. Continue logically from where the previous weeks ended, introducing new concepts and building on what was learned.

    **User Preferences:**
    - Difficulty: %s
    - Language: %s – output ALL text in this language.
    - Learning style: %s
    - Hours per week available: %d
    - Avoid weekends: %s

    **Output Requirements:**
    Return a JSON object with a "tasks" array. Each task must have:
    - week (integer, between %d and %d)
    - day (integer 1‑7 for daily tasks; null for weekly tasks)
    - description (short action)
    - details (longer instructions, resources)
    - subtasks (array of strings)
    - type ("daily" or "weekly")

    Use the same language and style as the original goal.
    Do NOT include tasks for weeks outside the requested range.
    Return ONLY valid JSON, no extra text.
    """, goal, totalWeeks, currentWeek, previousSummary, startWeek, endWeek,
                difficulty, language, learningStyle, hoursPerWeek, avoidWeekends,
                startWeek, endWeek);
    }

    public RoadmapResponse generateRoadmapAsDTO(User user, String goal, Integer timeframeWeeks, Integer timeframeValue, String timeframeUnit) {
        Roadmap roadmap = generateRoadmap(user, goal, timeframeWeeks, timeframeValue, timeframeUnit);
        return roadmapMapper.toResponse(roadmap);
    }
}