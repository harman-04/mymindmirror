package com.mymindmirror.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "custom_tasks" ,
        indexes = {
                @Index(name = "idx_custom_task_user", columnList = "user_id"),
                @Index(name = "idx_custom_task_due", columnList = "due_date")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomTask {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "estimated_hours")
    private Double estimatedHours;

    @Column(nullable = false)
    private String priority; // HIGH, MEDIUM, LOW

    private boolean completed = false;

    @Column(name = "created_at", nullable = false)
    private LocalDate createdAt;
}