package com.mymindmirror.backend.repository;

import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.model.UserStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class UserStatsRepositoryTest {

    @Autowired
    private UserStatsRepository repository;

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

    private UserStats persist(User owner) {
        UserStats stats = new UserStats();
        stats.setUser(owner);
        stats.setBadgesJson("[]");
        return repository.save(stats);
    }

    @Test
    @DisplayName("findByUser returns stats for the matching user")
    void findByUser_found() {
        UserStats saved = persist(user);

        Optional<UserStats> result = repository.findByUser(user);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    @DisplayName("findByUser returns empty when user has no stats")
    void findByUser_empty() {
        assertThat(repository.findByUser(user)).isEmpty();
    }

    @Test
    @DisplayName("findByUser isolates by user")
    void findByUser_isolatesByUser() {
        persist(otherUser);
        assertThat(repository.findByUser(user)).isEmpty();
    }

    @Test
    @DisplayName("Unique constraint prevents two stats rows for the same user")
    void duplicateUserStats_throws() {
        persist(user);

        UserStats dup = new UserStats();
        dup.setUser(user);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> repository.saveAndFlush(dup))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("onPostLoad() backfills null Integer fields to their defaults (legacy rows)")
    void onPostLoad_backfillsNulls() {
        UUID userId = UUID.randomUUID();
        UUID statsId = UUID.randomUUID();

        // 1. Insert a user (required for FK)
        em.getEntityManager().createNativeQuery(
                        "INSERT INTO users (id, username, email, password_hash) " +
                                "VALUES (:id, :u, :e, :p)")
                .setParameter("id", userId)
                .setParameter("u", "legacyuser")
                .setParameter("e", "legacy@example.com")
                .setParameter("p", "hashed")
                .executeUpdate();

        // 2. Insert a stats row with NULL Integer fields (simulating legacy data)
        em.getEntityManager().createNativeQuery(
                        "INSERT INTO user_stats " +
                                "(id, user_id, current_streak, longest_streak, total_tasks_completed, " +
                                " experience_points, level, total_journal_entries, total_chats, schedules_generated, badges) " +
                                "VALUES (:id, :uid, 0, 0, 0, NULL, NULL, NULL, NULL, NULL, '[]')")
                .setParameter("id", statsId)
                .setParameter("uid", userId)
                .executeUpdate();

        em.flush();
        em.clear();

        // 3. Load the entity → @PostLoad fires
        UserStats loaded = em.find(UserStats.class, statsId);

        assertThat(loaded).isNotNull();
        assertThat(loaded.getExperiencePoints()).isZero();
        assertThat(loaded.getLevel()).isEqualTo(1);
        assertThat(loaded.getTotalJournalEntries()).isZero();
        assertThat(loaded.getTotalChats()).isZero();
        assertThat(loaded.getSchedulesGenerated()).isZero();
    }

    @Test
    @DisplayName("onPostLoad() preserves existing non-null Integer fields")
    void onPostLoad_preservesNonNulls() {
        UUID userId = UUID.randomUUID();
        UUID statsId = UUID.randomUUID();

        // 1. Insert a user (required for FK)
        em.getEntityManager().createNativeQuery(
                        "INSERT INTO users (id, username, email, password_hash) " +
                                "VALUES (:id, :u, :e, :p)")
                .setParameter("id", userId)
                .setParameter("u", "modernuser")
                .setParameter("e", "modern@example.com")
                .setParameter("p", "hashed")
                .executeUpdate();

        // 2. Insert a stats row with NON-NULL Integer fields
        em.getEntityManager().createNativeQuery(
                        "INSERT INTO user_stats " +
                                "(id, user_id, current_streak, longest_streak, total_tasks_completed, " +
                                " experience_points, level, total_journal_entries, total_chats, schedules_generated, badges) " +
                                "VALUES (:id, :uid, 0, 0, 0, 500, 2, 10, 5, 3, '[]')")
                .setParameter("id", statsId)
                .setParameter("uid", userId)
                .executeUpdate();

        em.flush();
        em.clear();

        // 3. Load the entity → @PostLoad fires, but the 'if' checks will evaluate to false
        UserStats loaded = em.find(UserStats.class, statsId);

        assertThat(loaded).isNotNull();
        // Verify that the existing data was preserved, not reset to defaults
        assertThat(loaded.getExperiencePoints()).isEqualTo(500);
        assertThat(loaded.getLevel()).isEqualTo(2);
        assertThat(loaded.getTotalJournalEntries()).isEqualTo(10);
        assertThat(loaded.getTotalChats()).isEqualTo(5);
        assertThat(loaded.getSchedulesGenerated()).isEqualTo(3);
    }
}