package com.mymindmirror.backend.repository;

import com.mymindmirror.backend.model.DailyJournalSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class DailyJournalSummaryRepositoryTest {

    @Autowired
    private DailyJournalSummaryRepository repository;

    @Autowired
    private TestEntityManager em;

    private UUID userId;
    private UUID otherUserId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();
    }

    // ============================================================
    // Helper
    // ============================================================

    private DailyJournalSummary persist(UUID ownerId, LocalDate date, Double avgMood, Long totalWords, Integer entryCount) {
        DailyJournalSummary s = new DailyJournalSummary();
        s.setUserId(ownerId);
        s.setDate(date);
        s.setAvgMood(avgMood);
        s.setTotalWords(totalWords);
        s.setEntryCount(entryCount);
        return repository.save(s);
    }

    // ============================================================
    // findByUserAndDateRange – JPQL aggregation query
    // ============================================================

    @Test
    @DisplayName("findByUserAndDateRange returns rows for the user within the date range")
    void findByUserAndDateRange_returnsInRange() {
        LocalDate today = LocalDate.now();
        persist(userId, today.minusDays(10), 0.5, 100L, 2);    // out of range
        persist(userId, today.minusDays(5), 0.6, 200L, 3);     // in range
        persist(userId, today.minusDays(2), 0.7, 150L, 2);     // in range
        persist(userId, today, 0.8, 300L, 4);                  // in range
        persist(userId, today.plusDays(5), 0.9, 400L, 5);      // out of range

        List<Object[]> result = repository.findByUserAndDateRange(
                userId, today.minusDays(7), today);

        assertThat(result).hasSize(3);
    }

    @Test
    @DisplayName("findByUserAndDateRange includes both boundaries (BETWEEN is inclusive)")
    void findByUserAndDateRange_inclusiveBounds() {
        LocalDate start = LocalDate.now().minusDays(5);
        LocalDate end = LocalDate.now();
        persist(userId, start, 0.5, 100L, 2);
        persist(userId, end, 0.6, 200L, 3);

        List<Object[]> result = repository.findByUserAndDateRange(userId, start, end);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("findByUserAndDateRange returns rows in ascending date order")
    void findByUserAndDateRange_orderedAscByDate() {
        LocalDate today = LocalDate.now();
        persist(userId, today.minusDays(3), 0.5, 100L, 2);
        persist(userId, today.minusDays(1), 0.6, 200L, 3);
        persist(userId, today.minusDays(2), 0.7, 150L, 4);

        List<Object[]> result = repository.findByUserAndDateRange(
                userId, today.minusDays(7), today);

        assertThat(result).hasSize(3);
        LocalDate[] dates = result.stream()
                .map(row -> (LocalDate) row[0])
                .toArray(LocalDate[]::new);
        assertThat(dates[0]).isBefore(dates[1]);
        assertThat(dates[1]).isBefore(dates[2]);
    }

    @Test
    @DisplayName("findByUserAndDateRange returns the correct column structure [date, avgMood, totalWords]")
    void findByUserAndDateRange_columnStructure() {
        LocalDate today = LocalDate.now();
        persist(userId, today, 0.75, 500L, 3);

        List<Object[]> result = repository.findByUserAndDateRange(userId, today, today);

        assertThat(result).hasSize(1);
        Object[] row = result.get(0);
        assertThat(row).hasSize(3);
        assertThat(row[0]).isInstanceOf(LocalDate.class);
        assertThat(row[1]).isInstanceOf(Double.class);
        assertThat(row[2]).isInstanceOf(Long.class);
        assertThat(row[0]).isEqualTo(today);
        assertThat((Double) row[1]).isEqualTo(0.75);
        assertThat((Long) row[2]).isEqualTo(500L);
    }

    @Test
    @DisplayName("findByUserAndDateRange isolates data by userId")
    void findByUserAndDateRange_isolatesByUser() {
        LocalDate today = LocalDate.now();
        persist(userId, today, 0.5, 100L, 2);
        persist(userId, today.minusDays(1), 0.6, 200L, 3);
        persist(otherUserId, today, 0.9, 999L, 9);

        List<Object[]> result = repository.findByUserAndDateRange(userId, today.minusDays(7), today);

        assertThat(result).hasSize(2);
        // Ensure no rows from other user
        assertThat(result).noneMatch(row -> row[2].equals(999L));
    }

    @Test
    @DisplayName("findByUserAndDateRange returns empty list when no rows exist for the user")
    void findByUserAndDateRange_empty() {
        persist(otherUserId, LocalDate.now(), 0.5, 100L, 2);

        List<Object[]> result = repository.findByUserAndDateRange(
                userId, LocalDate.now().minusDays(30), LocalDate.now());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByUserAndDateRange returns empty list when date range has no matches")
    void findByUserAndDateRange_outOfRange() {
        persist(userId, LocalDate.now().minusDays(30), 0.5, 100L, 2);

        List<Object[]> result = repository.findByUserAndDateRange(
                userId, LocalDate.now().minusDays(7), LocalDate.now());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("deleteByUserId removes only the target user's summaries")
    void deleteByUserId_shouldDeleteOnlyTargetUser() {
        // Given
        LocalDate today = LocalDate.now();
        persist(userId, today, 0.5, 100L, 2);
        persist(userId, today.minusDays(1), 0.6, 150L, 3);
        persist(otherUserId, today, 0.9, 200L, 1); // Should NOT be deleted

        // When
        repository.deleteByUserId(userId);
        em.flush();
        em.clear();

        // Then
        List<Object[]> remaining = repository.findByUserAndDateRange(userId, today.minusDays(7), today);
        assertThat(remaining).isEmpty();

        List<Object[]> otherUserData = repository.findByUserAndDateRange(otherUserId, today.minusDays(7), today);
        assertThat(otherUserData).hasSize(1);
    }

}