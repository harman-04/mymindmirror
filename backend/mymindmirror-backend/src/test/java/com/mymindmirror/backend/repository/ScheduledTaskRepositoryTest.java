package com.mymindmirror.backend.repository;

import com.mymindmirror.backend.model.ScheduledTask;
import com.mymindmirror.backend.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Transactional
class ScheduledTaskRepositoryTest {

    @Autowired
    private ScheduledTaskRepository repository;

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

    private ScheduledTask persist(User owner, LocalDate date, boolean completed, String blockType, UUID customTaskId) {
        ScheduledTask t = new ScheduledTask();
        t.setUser(owner);
        t.setTitle("Task");
        t.setScheduledDate(date);
        t.setStartTime(LocalTime.of(9, 0));
        t.setEndTime(LocalTime.of(10, 0));
        t.setCompleted(completed);
        t.setBlockType(blockType);
        t.setCustomTaskId(customTaskId);
        t.setPriority("MEDIUM");
        return repository.save(t);
    }

    private long countAll() {
        return repository.count();
    }

    // ============================================================
    // findByUser (basic)
    // ============================================================

    @Test
    @DisplayName("findByUser returns only the user's scheduled tasks")
    void findByUser_isolatesByUser() {
        persist(user, LocalDate.now(), false, "WORK_TASK", null);
        persist(user, LocalDate.now(), true, "WORK_TASK", null);
        persist(otherUser, LocalDate.now(), false, "WORK_TASK", null);

        List<ScheduledTask> result = repository.findByUser(user);

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(t -> t.getUser().getId().equals(user.getId()));
    }

    @Test
    @DisplayName("findByUser returns empty when user has no tasks")
    void findByUser_empty() {
        assertThat(repository.findByUser(user)).isEmpty();
    }

    // ============================================================
    // findByUserAndScheduledDateBetween
    // ============================================================

    @Test
    @DisplayName("findByUserAndScheduledDateBetween returns only tasks in the range")
    void findByUserAndScheduledDateBetween_respectsRange() {
        LocalDate today = LocalDate.now();
        persist(user, today.minusDays(10), false, "WORK_TASK", null);   // out
        persist(user, today.minusDays(2), false, "WORK_TASK", null);    // in
        persist(user, today, false, "WORK_TASK", null);                  // in
        persist(user, today.plusDays(5), false, "WORK_TASK", null);     // out

        List<ScheduledTask> result = repository.findByUserAndScheduledDateBetween(
                user, today.minusDays(7), today);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("findByUserAndScheduledDateBetween includes both range boundaries")
    void findByUserAndScheduledDateBetween_inclusiveBounds() {
        LocalDate start = LocalDate.now().minusDays(5);
        LocalDate end = LocalDate.now();
        persist(user, start, false, "WORK_TASK", null);
        persist(user, end, false, "WORK_TASK", null);

        List<ScheduledTask> result = repository.findByUserAndScheduledDateBetween(user, start, end);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("findByUserAndScheduledDateBetween excludes other users")
    void findByUserAndScheduledDateBetween_isolatesByUser() {
        LocalDate today = LocalDate.now();
        persist(user, today, false, "WORK_TASK", null);
        persist(otherUser, today, false, "WORK_TASK", null);

        List<ScheduledTask> result = repository.findByUserAndScheduledDateBetween(
                user, today.minusDays(1), today.plusDays(1));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUser().getId()).isEqualTo(user.getId());
    }



    // ============================================================
    // findByCustomTaskId
    // ============================================================

    @Test
    @DisplayName("findByCustomTaskId returns all linked tasks regardless of user")
    void findByCustomTaskId_returnsAllLinked() {
        UUID customTaskId = UUID.randomUUID();
        UUID otherCustomTaskId = UUID.randomUUID();
        persist(user, LocalDate.now(), false, "WORK_TASK", customTaskId);
        persist(user, LocalDate.now(), true, "WORK_TASK", customTaskId);
        persist(otherUser, LocalDate.now(), false, "WORK_TASK", customTaskId);  // Linked to same custom id
        persist(user, LocalDate.now(), false, "WORK_TASK", otherCustomTaskId); // Different custom id

        List<ScheduledTask> result = repository.findByCustomTaskId(customTaskId);

        assertThat(result).hasSize(3);
        assertThat(result).allMatch(t -> customTaskId.equals(t.getCustomTaskId()));
    }

    @Test
    @DisplayName("findByCustomTaskId returns empty for unknown custom task ID")
    void findByCustomTaskId_empty() {
        assertThat(repository.findByCustomTaskId(UUID.randomUUID())).isEmpty();
    }

    // ============================================================
    // deleteByUser (Spring Data derived delete)
    // ============================================================

    @Test
    @DisplayName("deleteByUser deletes only the user's tasks, leaves other users' intact")
    void deleteByUser_onlyTargetUser() {
        persist(user, LocalDate.now(), false, "WORK_TASK", null);
        persist(user, LocalDate.now(), true, "WORK_TASK", null);
        persist(otherUser, LocalDate.now(), false, "WORK_TASK", null);

        repository.deleteByUser(user);
        em.flush();
        em.clear();

        assertThat(repository.findByUser(user)).isEmpty();
        assertThat(repository.findByUser(otherUser)).hasSize(1);
    }




    // ============================================================
    // deleteIncompleteByUser
    // ============================================================

    @Test
    @DisplayName("deleteIncompleteByUser deletes all incomplete tasks, preserves completed")
    void deleteIncompleteByUser_preservesCompleted() {
        persist(user, LocalDate.now(), false, "WORK_TASK", null);
        persist(user, LocalDate.now().minusDays(5), false, "ROUTINE", null);
        persist(user, LocalDate.now(), true, "WORK_TASK", null);
        persist(user, LocalDate.now(), true, "ROUTINE", null);
        persist(otherUser, LocalDate.now(), false, "WORK_TASK", null);

        repository.deleteIncompleteByUser(user);
        em.flush();
        em.clear();

        List<ScheduledTask> remaining = repository.findByUser(user);
        assertThat(remaining).hasSize(2);
        assertThat(remaining).allMatch(ScheduledTask::isCompleted);

        assertThat(repository.findByUser(otherUser)).hasSize(1);
    }


    // ============================================================
    // deleteIncompleteTodayByUser
    // ============================================================

    @Test
    @DisplayName("deleteIncompleteTodayByUser deletes only incomplete tasks for today")
    void deleteIncompleteTodayByUser_onlyTodayIncomplete() {
        LocalDate today = LocalDate.now();

        persist(user, today, false, "WORK_TASK", null);            // DELETE
        persist(user, today, false, "ROUTINE", null);              // DELETE
        persist(user, today, true, "WORK_TASK", null);             // KEEP (completed)
        persist(user, today.minusDays(1), false, "WORK_TASK", null); // KEEP (yesterday)
        persist(user, today.plusDays(1), false, "WORK_TASK", null); // KEEP (tomorrow)
        persist(otherUser, today, false, "WORK_TASK", null);       // KEEP (other user)

        repository.deleteIncompleteTodayByUser(user, today);
        em.flush();
        em.clear();

        List<ScheduledTask> remaining = repository.findByUser(user);
        assertThat(remaining).hasSize(3);
        // No incomplete tasks for today remain
        assertThat(remaining).noneMatch(t -> t.getScheduledDate().equals(today) && !t.isCompleted());

        assertThat(repository.findByUser(otherUser)).hasSize(1);
    }

    @Test
    @DisplayName("deleteIncompleteTodayByUser is a no-op when user has no tasks for today")
    void deleteIncompleteTodayByUser_noTasksToday() {
        persist(user, LocalDate.now().minusDays(3), false, "WORK_TASK", null);

        repository.deleteIncompleteTodayByUser(user, LocalDate.now());
        em.flush();
        em.clear();

        assertThat(repository.findByUser(user)).hasSize(1);
    }

    // ============================================================
    // Combined sanity check
    // ============================================================

    @Test
    @DisplayName("All delete methods combined leave other users untouched")
    void allDeletes_doNotAffectOtherUsers() {
        UUID customId = UUID.randomUUID();

        // User tasks (mix)
        persist(user, LocalDate.now(), false, "WORK_TASK", null);
        persist(user, LocalDate.now(), false, "ROUTINE", null);
        persist(user, LocalDate.now(), true, "WORK_TASK", customId);

        // Other user's tasks
        persist(otherUser, LocalDate.now(), false, "WORK_TASK", null);
        persist(otherUser, LocalDate.now(), false, "ROUTINE", null);
        persist(otherUser, LocalDate.now(), true, "WORK_TASK", customId);

        long otherUserBefore = repository.findByUser(otherUser).size();

        repository.deleteIncompleteByUser(user);
        em.flush();
        em.clear();

        assertThat(repository.findByUser(otherUser)).hasSize((int) otherUserBefore);
    }
}