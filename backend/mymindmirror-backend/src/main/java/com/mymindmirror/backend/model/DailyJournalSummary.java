package com.mymindmirror.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "daily_journal_summary",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_date",
                columnNames = {"user_id", "date"}
        )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyJournalSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "avg_mood")
    private Double avgMood;

    @Column(name = "total_words")
    private Long totalWords;

    @Column(name = "entry_count")
    private Integer entryCount;
}