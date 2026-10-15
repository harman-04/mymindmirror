package com.mymindmirror.backend.repository;

import com.mymindmirror.backend.model.DailyJournalSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DailyJournalSummaryRepository extends JpaRepository<DailyJournalSummary, Long> {
    @Query("SELECT d.date, d.avgMood, d.totalWords FROM DailyJournalSummary d WHERE d.userId = :userId AND d.date BETWEEN :start AND :end ORDER BY d.date")
    List<Object[]> findByUserAndDateRange(@Param("userId") UUID userId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Modifying
    @Query(value = "INSERT INTO daily_journal_summary (user_id, date, avg_mood, total_words, entry_count) " +
            "VALUES (:#{#userId}, :#{#date}, :#{#avgMood}, :#{#totalWords}, :#{#entryCount}) " +
            "ON DUPLICATE KEY UPDATE " +
            "avg_mood = VALUES(avg_mood), total_words = VALUES(total_words), entry_count = VALUES(entry_count)",
            nativeQuery = true)
    void insertOrUpdateSummary(@Param("userId") UUID userId,
                               @Param("date") LocalDate date,
                               @Param("avgMood") Double avgMood,
                               @Param("totalWords") Long totalWords,
                               @Param("entryCount") Integer entryCount);
}
