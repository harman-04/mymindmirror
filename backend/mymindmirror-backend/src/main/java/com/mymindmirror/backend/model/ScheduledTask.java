package com.mymindmirror.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "scheduled_tasks",
        indexes = {
                @Index(name = "idx_scheduled_user_date", columnList = "user_id, scheduled_date"),
                @Index(name = "idx_scheduled_reminder", columnList = "reminder_sent")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledTask {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Can be null if it's a custom task (not from roadmap/milestone)
    @Column(name = "roadmap_task_id")
    private UUID roadmapTaskId;

    @Column(name = "milestone_task_id")
    private UUID milestoneTaskId;

    @Column(name = "custom_task_id")
    private UUID customTaskId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "priority")
    private String priority = "MEDIUM";

    private boolean completed = false;

    @Column(name = "reminder_sent")
    private boolean reminderSent = false;

    // ---   Block Type for Smart Scheduling ---
    // Options: WORK_TASK, ROUTINE, BREAK, MEAL, AI_SUGGESTION
    @Column(name = "block_type", length = 30)
    private String blockType = "WORK_TASK";
}