// src/main/java/com/mymindmirror/backend/repository/JournalEntryRepository.java

package com.mymindmirror.backend.repository;

import com.mymindmirror.backend.model.JournalEntry;
import com.mymindmirror.backend.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, UUID> {


    // To this (Adding "user"):
    @EntityGraph(attributePaths = {"keyPhrases", "user"})
    List<JournalEntry> findByUserAndEntryDateBetweenOrderByCreationTimestampDesc(User user, LocalDate startDate, LocalDate endDate);

    @EntityGraph(attributePaths = "keyPhrases")
    List<JournalEntry> findByUserAndEntryDateBetween(User user, LocalDate startDate, LocalDate endDate);

    // 1. Keyword Search base query (Added "user" to EntityGraph)
    @EntityGraph(attributePaths = {"keyPhrases", "user"})
    List<JournalEntry> findByUserOrderByCreationTimestampDesc(User user);


    // 2. Mood Search (Added EntityGraph because it returns a List, not a Page)
    @EntityGraph(attributePaths = {"keyPhrases", "user"})
    List<JournalEntry> findByUserAndMoodScoreBetweenOrderByCreationTimestampDesc(User user, Double minMoodScore, Double maxMoodScore);

    @EntityGraph(attributePaths = {"keyPhrases", "user"})
    List<JournalEntry> findByUser(User user);

    @Query("SELECT je FROM JournalEntry je " +
            "LEFT JOIN FETCH je.keyPhrases " +
            "LEFT JOIN FETCH je.user " +
            "WHERE je.id = :id")
    Optional<JournalEntry> findByIdWithDetails(@Param("id") UUID id);

    @Query("SELECT je.entryDate, AVG(je.moodScore), SUM(LENGTH(je.rawText)) " +
            "FROM JournalEntry je WHERE je.user.id = :userId AND je.entryDate BETWEEN :startDate AND :endDate " +
            "GROUP BY je.entryDate ORDER BY je.entryDate")
    List<Object[]> findDailyAggregatedDataByUserAndDateRange(@Param("userId") UUID userId,
                                                             @Param("startDate") LocalDate startDate,
                                                             @Param("endDate") LocalDate endDate);

    @Modifying
    @Query("DELETE FROM KeyPhrase k WHERE k.journalEntry.id = :entryId")
    void deleteKeyPhrasesByEntryId(@Param("entryId") UUID entryId);

    Page<JournalEntry> findByUserAndEntryDateBetweenOrderByCreationTimestampDesc(User user, LocalDate startDate, LocalDate endDate, Pageable pageable);

    // Change this from Page<JournalEntry> to List<JournalEntry>
    @Query("SELECT je FROM JournalEntry je WHERE je.user = :user ORDER BY je.creationTimestamp DESC")
    List<JournalEntry> findLatestEntryByUser(@Param("user") User user, Pageable pageable);

    // Highly optimized fetch for Semantic Searches to prevent N+1 collection spam
    @EntityGraph(attributePaths = {"keyPhrases", "user"})
    List<JournalEntry> findByIdIn(List<UUID> ids);
}
