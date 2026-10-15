package com.mymindmirror.backend.repository;

import com.mymindmirror.backend.model.JournalEntry;
import com.mymindmirror.backend.model.KeyPhrase;
import com.mymindmirror.backend.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class JournalEntryRepositoryTest {

    @Autowired
    private JournalEntryRepository repository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager em;

    private User user;
    private User otherUser;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPasswordHash("hashed");
        user = userRepository.save(user);

        otherUser = new User();
        otherUser.setUsername("otheruser");
        otherUser.setEmail("other@example.com");
        otherUser.setPasswordHash("hashed");
        otherUser = userRepository.save(otherUser);
    }

    // ============================================================
    // Helpers
    // ============================================================

    private JournalEntry persistEntry(User owner, LocalDate date, Double mood, String rawText, String... phrases) {
        JournalEntry entry = new JournalEntry();
        entry.setUser(owner);
        entry.setEntryDate(date);
        entry.setCreationTimestamp(LocalDateTime.now());
        entry.setRawText(rawText);
        entry.setMoodScore(mood);
        entry.setWordCount(rawText.split("\\s+").length);
        entry = repository.save(entry);

        for (String p : phrases) {
            KeyPhrase kp = new KeyPhrase(p, entry);
            entry.getKeyPhrases().add(kp);
        }
        return repository.save(entry);
    }

    // ============================================================
    // findByUserAndEntryDateBetweenOrderByCreationTimestampDesc
    // (with @EntityGraph keyPhrases + user)
    // ============================================================

    @Test
    @DisplayName("findByUserAndEntryDateBetween... returns only the user's entries in the range")
    void findByUserAndDateRange_onlyUserEntries() {
        LocalDate today = LocalDate.now();
        persistEntry(user, today, 0.5, "entry 1", "happy");
        persistEntry(user, today.minusDays(5), 0.7, "entry 2", "calm");
        persistEntry(otherUser, today, 0.9, "other entry", "joy");

        List<JournalEntry> result = repository.findByUserAndEntryDateBetweenOrderByCreationTimestampDesc(
                user, today.minusDays(10), today);

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(e -> e.getUser().getId().equals(user.getId()));
    }

    @Test
    @DisplayName("...excludes entries outside the date range")
    void findByUserAndDateRange_respectsRange() {
        LocalDate today = LocalDate.now();
        persistEntry(user, today, 0.5, "in range");
        persistEntry(user, today.minusDays(30), 0.5, "out of range");

        List<JournalEntry> result = repository.findByUserAndEntryDateBetweenOrderByCreationTimestampDesc(
                user, today.minusDays(7), today);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("...eagerly loads keyPhrases (no LazyInitializationException)")
    void findByUserAndDateRange_eagerlyLoadsKeyPhrases() {
        persistEntry(user, LocalDate.now(), 0.5, "text", "phrase1", "phrase2");
        em.flush();
        em.clear();

        List<JournalEntry> result = repository.findByUserAndEntryDateBetweenOrderByCreationTimestampDesc(
                user, LocalDate.now().minusDays(1), LocalDate.now());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getKeyPhrases()).hasSize(2);
    }

    @Test
    @DisplayName("...returns empty list when no entries match")
    void findByUserAndDateRange_empty() {
        List<JournalEntry> result = repository.findByUserAndEntryDateBetweenOrderByCreationTimestampDesc(
                user, LocalDate.now().minusDays(30), LocalDate.now());

        assertThat(result).isEmpty();
    }

    // ============================================================
    // findByUserAndEntryDateBetween (with @EntityGraph keyPhrases)
    // ============================================================

    @Test
    @DisplayName("findByUserAndEntryDateBetween returns entries with eager keyPhrases")
    void findByUserAndDateRange_eagerKeyPhrases() {
        persistEntry(user, LocalDate.now(), 0.5, "text", "kp1");
        em.flush();
        em.clear();

        List<JournalEntry> result = repository.findByUserAndEntryDateBetween(
                user, LocalDate.now().minusDays(1), LocalDate.now());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getKeyPhrases()).hasSize(1);
    }

    // ============================================================
    // findByUserOrderByCreationTimestampDesc (with @EntityGraph)
    // ============================================================

    @Test
    @DisplayName("findByUserOrderByCreationTimestampDesc returns user's entries only")
    void findByUser_onlyUserEntries() {
        persistEntry(user, LocalDate.now(), 0.5, "a", "kp");
        persistEntry(user, LocalDate.now().minusDays(1), 0.6, "b");
        persistEntry(otherUser, LocalDate.now(), 0.9, "other");

        List<JournalEntry> result = repository.findByUserOrderByCreationTimestampDesc(user);

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(e -> e.getUser().getId().equals(user.getId()));
    }

    // ============================================================
    // findByUserAndMoodScoreBetweenOrderByCreationTimestampDesc
    // ============================================================

    @Test
    @DisplayName("findByUserAndMoodScoreBetween returns only entries within the mood range")
    void findByUserAndMoodScoreBetween_filtersMood() {
        persistEntry(user, LocalDate.now(), 0.9, "happy");
        persistEntry(user, LocalDate.now().minusDays(1), 0.5, "neutral");
        persistEntry(user, LocalDate.now().minusDays(2), 0.1, "sad");

        List<JournalEntry> result = repository.findByUserAndMoodScoreBetweenOrderByCreationTimestampDesc(
                user, 0.4, 1.0);

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(e -> e.getMoodScore() >= 0.4 && e.getMoodScore() <= 1.0);
    }

    @Test
    @DisplayName("...excludes entries with null moodScore")
    void findByUserAndMoodScoreBetween_handlesNull() {
        persistEntry(user, LocalDate.now(), null, "unscored");
        persistEntry(user, LocalDate.now(), 0.5, "scored");

        List<JournalEntry> result = repository.findByUserAndMoodScoreBetweenOrderByCreationTimestampDesc(
                user, 0.0, 1.0);

        assertThat(result).hasSize(1);
    }

    // ============================================================
    // findByUser (with @EntityGraph)
    // ============================================================

    @Test
    @DisplayName("findByUser returns all entries for the user")
    void findByUser_allUserEntries() {
        persistEntry(user, LocalDate.now(), 0.5, "1");
        persistEntry(user, LocalDate.now().minusDays(1), 0.5, "2");
        persistEntry(user, LocalDate.now().minusDays(2), 0.5, "3");
        persistEntry(otherUser, LocalDate.now(), 0.5, "x");

        List<JournalEntry> result = repository.findByUser(user);

        assertThat(result).hasSize(3);
    }

    // ============================================================
    // findByIdWithDetails (custom JPQL with LEFT JOIN FETCH)
    // ============================================================

    @Test
    @DisplayName("findByIdWithDetails loads keyPhrases and user in one query")
    void findByIdWithDetails_returnsFullyLoadedEntity() {
        JournalEntry saved = persistEntry(user, LocalDate.now(), 0.5, "text", "kp1", "kp2");
        em.flush();
        em.clear();

        Optional<JournalEntry> result = repository.findByIdWithDetails(saved.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getKeyPhrases()).hasSize(2);
        assertThat(result.get().getUser().getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("findByIdWithDetails returns empty for missing id")
    void findByIdWithDetails_notFound() {
        Optional<JournalEntry> result = repository.findByIdWithDetails(UUID.randomUUID());
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByIdWithDetails works with entries that have no key phrases")
    void findByIdWithDetails_noKeyPhrases() {
        JournalEntry saved = persistEntry(user, LocalDate.now(), 0.5, "no phrases");
        em.flush();
        em.clear();

        Optional<JournalEntry> result = repository.findByIdWithDetails(saved.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getKeyPhrases()).isEmpty();
    }

    // ============================================================
    // findDailyAggregatedDataByUserAndDateRange
    // ============================================================

    @Test
    @DisplayName("findDailyAggregatedData groups by date and computes avg mood + sum of words")
    void findDailyAggregatedData_aggregates() {
        LocalDate today = LocalDate.now();
        persistEntry(user, today, 0.6, "one two three", "a");
        persistEntry(user, today, 0.8, "four five", "b");
        persistEntry(user, today.minusDays(1), 0.4, "six", "c");

        List<Object[]> rows = repository.findDailyAggregatedDataByUserAndDateRange(
                user.getId(), today.minusDays(2), today);

        assertThat(rows).hasSize(2);  // two distinct dates

        // Row format: [LocalDate, Double (avgMood), Long (sum of length(rawText))]
        Object[] firstRow = rows.get(0);
        assertThat(firstRow[0]).isInstanceOf(LocalDate.class);
        assertThat(firstRow[1]).isInstanceOf(Double.class);
        assertThat(firstRow[2]).isInstanceOf(Long.class);

        // The day with two entries should have avg = 0.7
        boolean foundTwoEntryDay = false;
        for (Object[] row : rows) {
            LocalDate d = (LocalDate) row[0];
            if (d.equals(today)) {
                assertThat((Double) row[1]).isCloseTo(0.7, org.assertj.core.data.Offset.offset(0.001));
                foundTwoEntryDay = true;
            }
        }
        assertThat(foundTwoEntryDay).isTrue();
    }

    @Test
    @DisplayName("findDailyAggregatedData excludes other users")
    void findDailyAggregatedData_isolatedByUser() {
        LocalDate today = LocalDate.now();
        persistEntry(user, today, 0.9, "mine");
        persistEntry(otherUser, today, 0.1, "theirs");

        List<Object[]> rows = repository.findDailyAggregatedDataByUserAndDateRange(
                user.getId(), today.minusDays(1), today);

        assertThat(rows).hasSize(1);
        // Avg mood should be 0.9 (only user's entry)
        assertThat((Double) rows.get(0)[1]).isCloseTo(0.9, org.assertj.core.data.Offset.offset(0.001));
    }

    @Test
    @DisplayName("findDailyAggregatedData returns empty list when no entries")
    void findDailyAggregatedData_empty() {
        List<Object[]> rows = repository.findDailyAggregatedDataByUserAndDateRange(
                user.getId(), LocalDate.now(), LocalDate.now());

        assertThat(rows).isEmpty();
    }

    // ============================================================
    // deleteKeyPhrasesByEntryId (modifying query)
    // ============================================================

    @Test
    @DisplayName("deleteKeyPhrasesByEntryId deletes only the target entry's phrases")
    void deleteKeyPhrasesByEntryId_deletesOnlyTarget() {
        JournalEntry entry1 = persistEntry(user, LocalDate.now(), 0.5, "a", "kp1", "kp2");
        JournalEntry entry2 = persistEntry(user, LocalDate.now(), 0.5, "b", "kp3");
        em.flush();

        repository.deleteKeyPhrasesByEntryId(entry1.getId());
        em.flush();
        em.clear();

        JournalEntry reloaded1 = em.find(JournalEntry.class, entry1.getId());
        JournalEntry reloaded2 = em.find(JournalEntry.class, entry2.getId());

        assertThat(reloaded1.getKeyPhrases()).isEmpty();
        assertThat(reloaded2.getKeyPhrases()).hasSize(1);
    }

    // ============================================================
    // findLatestEntryByUser (custom JPQL + Pageable)
    // ============================================================

    @Test
    @DisplayName("findLatestEntryByUser returns the most recent entry (limit 1)")
    void findLatestEntryByUser_returnsLatest() {
        LocalDateTime base = LocalDateTime.now().minusDays(5);
        JournalEntry older = new JournalEntry();
        older.setUser(user);
        older.setEntryDate(LocalDate.now().minusDays(5));
        older.setCreationTimestamp(base);
        older.setRawText("old");
        older.setWordCount(1);
        repository.save(older);

        JournalEntry newer = new JournalEntry();
        newer.setUser(user);
        newer.setEntryDate(LocalDate.now());
        newer.setCreationTimestamp(base.plusDays(5));
        newer.setRawText("new");
        newer.setWordCount(1);
        repository.save(newer);

        List<JournalEntry> result = repository.findLatestEntryByUser(user, PageRequest.of(0, 1));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRawText()).isEqualTo("new");
    }

    @Test
    @DisplayName("findLatestEntryByUser returns empty when user has no entries")
    void findLatestEntryByUser_empty() {
        List<JournalEntry> result = repository.findLatestEntryByUser(user, PageRequest.of(0, 1));
        assertThat(result).isEmpty();
    }

    // ============================================================
    // Paginated query with @EntityGraph
    // ============================================================

    @Test
    @DisplayName("Paginated query returns correct page metadata")
    void paginatedQuery_returnsPageMetadata() {
        for (int i = 0; i < 25; i++) {
            persistEntry(user, LocalDate.now().minusDays(i), 0.5, "entry " + i, "kp" + i);
        }

        Page<JournalEntry> page = repository.findByUserAndEntryDateBetweenOrderByCreationTimestampDesc(
                user, LocalDate.now().minusDays(30), LocalDate.now(), PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(25);
        assertThat(page.getTotalPages()).isEqualTo(3);
        assertThat(page.getContent()).hasSize(10);
        assertThat(page.isFirst()).isTrue();
        assertThat(page.isLast()).isFalse();
    }

    @Test
    @DisplayName("Paginated query returns last page correctly")
    void paginatedQuery_lastPage() {
        for (int i = 0; i < 25; i++) {
            persistEntry(user, LocalDate.now().minusDays(i), 0.5, "entry " + i);
        }

        Page<JournalEntry> page = repository.findByUserAndEntryDateBetweenOrderByCreationTimestampDesc(
                user, LocalDate.now().minusDays(30), LocalDate.now(), PageRequest.of(2, 10));

        assertThat(page.getContent()).hasSize(5);
        assertThat(page.isLast()).isTrue();
    }

    // ============================================================
    // findByIdIn (with @EntityGraph)
    // ============================================================

    @Test
    @DisplayName("findByIdIn returns entries with eager keyPhrases and user")
    void findByIdIn_returnsEagerlyLoadedEntries() {
        JournalEntry e1 = persistEntry(user, LocalDate.now(), 0.5, "a", "kp1");
        JournalEntry e2 = persistEntry(user, LocalDate.now(), 0.6, "b", "kp2");
        em.flush();
        em.clear();

        List<JournalEntry> result = repository.findByIdIn(List.of(e1.getId(), e2.getId()));

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(e -> e.getKeyPhrases() != null && !e.getKeyPhrases().isEmpty());
        assertThat(result).allMatch(e -> e.getUser() != null);
    }

    @Test
    @DisplayName("findByIdIn returns empty for empty ID list")
    void findByIdIn_emptyIds() {
        List<JournalEntry> result = repository.findByIdIn(List.of());
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByUserOrderByCreationTimestampDesc returns entries in descending creation order")
    void findByUser_ordersByCreationTimestampDesc() {
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < 3; i++) {
            JournalEntry e = new JournalEntry();
            e.setUser(user);
            e.setEntryDate(LocalDate.now().minusDays(i));
            e.setCreationTimestamp(now.minusDays(i));
            e.setRawText("entry " + i);
            e.setWordCount(2);
            repository.save(e);
        }
        em.flush();
        em.clear();

        List<JournalEntry> result = repository.findByUserOrderByCreationTimestampDesc(user);

        assertThat(result).hasSize(3);
        // Newest first
        assertThat(result.get(0).getCreationTimestamp())
                .isAfterOrEqualTo(result.get(1).getCreationTimestamp());
        assertThat(result.get(1).getCreationTimestamp())
                .isAfterOrEqualTo(result.get(2).getCreationTimestamp());
    }

    @Test
    @DisplayName("Date range boundaries are inclusive")
    void dateRange_isInclusiveBothEnds() {
        LocalDate start = LocalDate.now().minusDays(5);
        LocalDate end = LocalDate.now();
        persistEntry(user, start, 0.5, "at start");
        persistEntry(user, end, 0.5, "at end");
        persistEntry(user, start.minusDays(1), 0.5, "before start");

        List<JournalEntry> result = repository.findByUserAndEntryDateBetweenOrderByCreationTimestampDesc(
                user, start, end);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("deleteKeyPhrasesByEntryId on entry with no phrases is a no-op")
    void deleteKeyPhrases_noPhrases() {
        JournalEntry noPhrases = persistEntry(user, LocalDate.now(), 0.5, "text");
        em.flush();

        repository.deleteKeyPhrasesByEntryId(noPhrases.getId());
        em.flush();
        em.clear();

        JournalEntry reloaded = em.find(JournalEntry.class, noPhrases.getId());
        assertThat(reloaded).isNotNull();
        assertThat(reloaded.getKeyPhrases()).isEmpty();
    }
}