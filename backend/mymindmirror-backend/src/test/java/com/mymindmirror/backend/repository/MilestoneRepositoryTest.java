package com.mymindmirror.backend.repository;

import com.mymindmirror.backend.enums.Status;
import com.mymindmirror.backend.model.Milestone;
import com.mymindmirror.backend.model.Task;
import com.mymindmirror.backend.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class MilestoneRepositoryTest {

    @Autowired
    private MilestoneRepository repository;

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

    private Milestone persistMilestone(User owner, String title, LocalDate creationDate) {
        Milestone milestone = new Milestone();
        milestone.setUser(owner);
        milestone.setTitle(title);
        milestone.setDescription("Description of " + title);
        milestone.setCreationDate(creationDate);
        milestone.setDueDate(creationDate.plusDays(30));
        milestone.setStatus(Status.PENDING);
        return repository.save(milestone);
    }

    private Task persistTask(Milestone milestone, String description, Status status) {
        Task task = new Task();
        task.setMilestone(milestone);
        task.setDescription(description);
        task.setCreationTimestamp(LocalDateTime.now());
        task.setStatus(status);
        milestone.getTasks().add(task);
        return task;
    }

    // ============================================================
    // findByUserOrderByCreationDateDesc
    // ============================================================

    @Test
    @DisplayName("findByUserOrderByCreationDateDesc returns only the user's milestones")
    void findByUser_onlyUserMilestones() {
        persistMilestone(user, "Mine 1", LocalDate.now());
        persistMilestone(user, "Mine 2", LocalDate.now().minusDays(1));
        persistMilestone(otherUser, "Theirs", LocalDate.now());

        List<Milestone> result = repository.findByUserOrderByCreationDateDesc(user);

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(m -> m.getUser().getId().equals(user.getId()));
    }

    @Test
    @DisplayName("findByUserOrderByCreationDateDesc returns milestones in descending creation date order")
    void findByUser_ordersByCreationDateDesc() {
        LocalDate today = LocalDate.now();
        persistMilestone(user, "Oldest", today.minusDays(20));
        persistMilestone(user, "Middle", today.minusDays(10));
        persistMilestone(user, "Newest", today);

        List<Milestone> result = repository.findByUserOrderByCreationDateDesc(user);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getTitle()).isEqualTo("Newest");
        assertThat(result.get(1).getTitle()).isEqualTo("Middle");
        assertThat(result.get(2).getTitle()).isEqualTo("Oldest");
    }

    @Test
    @DisplayName("findByUserOrderByCreationDateDesc returns empty list for a user with no milestones")
    void findByUser_emptyForNewUser() {
        List<Milestone> result = repository.findByUserOrderByCreationDateDesc(user);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByUserOrderByCreationDateDesc eagerly loads tasks via @EntityGraph")
    void findByUser_eagerlyLoadsTasks() {
        Milestone milestone = persistMilestone(user, "With Tasks", LocalDate.now());
        persistTask(milestone, "Task 1", Status.PENDING);
        persistTask(milestone, "Task 2", Status.COMPLETED);
        persistTask(milestone, "Task 3", Status.IN_PROGRESS);
        repository.save(milestone);
        em.flush();
        em.clear();

        List<Milestone> result = repository.findByUserOrderByCreationDateDesc(user);

        assertThat(result).hasSize(1);
        //  Tasks eagerly loaded – no LazyInitializationException
        assertThat(result.get(0).getTasks()).hasSize(3);
    }

    @Test
    @DisplayName("findByUserOrderByCreationDateDesc returns milestones with empty tasks")
    void findByUser_handlesEmptyTasks() {
        persistMilestone(user, "Empty", LocalDate.now());
        em.flush();
        em.clear();

        List<Milestone> result = repository.findByUserOrderByCreationDateDesc(user);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTasks()).isEmpty();
    }

    // ============================================================
    // findByIdAndUser
    // ============================================================

    @Test
    @DisplayName("findByIdAndUser returns milestone when both id and user match")
    void findByIdAndUser_found() {
        Milestone milestone = persistMilestone(user, "Findable", LocalDate.now());

        List<Milestone> result = repository.findByIdAndUser(milestone.getId(), user);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(milestone.getId());
    }

    @Test
    @DisplayName("findByIdAndUser returns empty when milestone belongs to a different user")
    void findByIdAndUser_wrongOwner() {
        Milestone milestone = persistMilestone(otherUser, "Not Mine", LocalDate.now());

        List<Milestone> result = repository.findByIdAndUser(milestone.getId(), user);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByIdAndUser returns empty for unknown ID")
    void findByIdAndUser_unknownId() {
        List<Milestone> result = repository.findByIdAndUser(UUID.randomUUID(), user);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByIdAndUser eagerly loads tasks via @EntityGraph")
    void findByIdAndUser_eagerlyLoadsTasks() {
        Milestone milestone = persistMilestone(user, "Full", LocalDate.now());
        persistTask(milestone, "T1", Status.PENDING);
        persistTask(milestone, "T2", Status.COMPLETED);
        repository.save(milestone);
        em.flush();
        em.clear();

        List<Milestone> result = repository.findByIdAndUser(milestone.getId(), user);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTasks()).hasSize(2);
    }

    @Test
    @DisplayName("findByIdAndUser returns milestone with empty tasks")
    void findByIdAndUser_emptyTasks() {
        Milestone milestone = persistMilestone(user, "No Tasks", LocalDate.now());
        em.flush();
        em.clear();

        List<Milestone> result = repository.findByIdAndUser(milestone.getId(), user);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTasks()).isEmpty();
    }

    // ============================================================
    // getCompletionPercentage (null-safe, derived from tasks)
    // ============================================================

    @Test
    @DisplayName("getCompletionPercentage returns 0 for milestone with no tasks")
    void getCompletionPercentage_noTasks() {
        Milestone milestone = persistMilestone(user, "No Tasks", LocalDate.now());

        assertThat(milestone.getCompletionPercentage()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("getCompletionPercentage returns 0 when all tasks pending")
    void getCompletionPercentage_noneCompleted() {
        Milestone milestone = persistMilestone(user, "All Pending", LocalDate.now());
        persistTask(milestone, "T1", Status.PENDING);
        persistTask(milestone, "T2", Status.PENDING);

        assertThat(milestone.getCompletionPercentage()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("getCompletionPercentage returns 100 when all tasks completed")
    void getCompletionPercentage_allCompleted() {
        Milestone milestone = persistMilestone(user, "Done", LocalDate.now());
        persistTask(milestone, "T1", Status.COMPLETED);
        persistTask(milestone, "T2", Status.COMPLETED);

        assertThat(milestone.getCompletionPercentage()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("getCompletionPercentage returns 50 for half-completed tasks")
    void getCompletionPercentage_half() {
        Milestone milestone = persistMilestone(user, "Half", LocalDate.now());
        persistTask(milestone, "T1", Status.COMPLETED);
        persistTask(milestone, "T2", Status.PENDING);

        assertThat(milestone.getCompletionPercentage()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("getCompletionPercentage returns correct fraction for mixed statuses")
    void getCompletionPercentage_fraction() {
        Milestone milestone = persistMilestone(user, "Mixed", LocalDate.now());
        persistTask(milestone, "T1", Status.COMPLETED);
        persistTask(milestone, "T2", Status.PENDING);
        persistTask(milestone, "T3", Status.IN_PROGRESS);
        persistTask(milestone, "T4", Status.COMPLETED);

        assertThat(milestone.getCompletionPercentage()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("getCompletionPercentage handles null tasks list (null-safety fix)")
    void getCompletionPercentage_nullTasks() {
        Milestone milestone = persistMilestone(user, "Null Tasks", LocalDate.now());
        milestone.setTasks(null);

        assertThat(milestone.getCompletionPercentage()).isEqualTo(0.0);
    }

    // ============================================================
    // Task ordering (verified via @OrderBy on the entity)
    // ============================================================

    @Test
    @DisplayName("Tasks are ordered by creation timestamp ascending")
    void findByUser_tasksOrderedByCreationTimestamp() {
        Milestone milestone = persistMilestone(user, "Ordered", LocalDate.now());

        Task t3 = new Task();
        t3.setMilestone(milestone);
        t3.setDescription("Third");
        t3.setCreationTimestamp(LocalDateTime.now().plusMinutes(3));
        t3.setStatus(Status.PENDING);
        milestone.getTasks().add(t3);

        Task t1 = new Task();
        t1.setMilestone(milestone);
        t1.setDescription("First");
        t1.setCreationTimestamp(LocalDateTime.now().plusMinutes(1));
        t1.setStatus(Status.PENDING);
        milestone.getTasks().add(t1);

        Task t2 = new Task();
        t2.setMilestone(milestone);
        t2.setDescription("Second");
        t2.setCreationTimestamp(LocalDateTime.now().plusMinutes(2));
        t2.setStatus(Status.PENDING);
        milestone.getTasks().add(t2);

        repository.save(milestone);
        em.flush();
        em.clear();

        Milestone reloaded = repository.findByIdAndUser(milestone.getId(), user).get(0);

        assertThat(reloaded.getTasks())
                .extracting(Task::getDescription)
                .containsExactly("First", "Second", "Third");
    }

    // ============================================================
    // Cascade + Orphan removal (structural integrity)
    // ============================================================

    @Test
    @DisplayName("Deleting a milestone cascades to delete its tasks")
    void deleteMilestone_cascadeDeletesTasks() {
        Milestone milestone = persistMilestone(user, "Cascade", LocalDate.now());
        persistTask(milestone, "T1", Status.PENDING);
        persistTask(milestone, "T2", Status.COMPLETED);
        repository.save(milestone);
        em.flush();

        UUID milestoneId = milestone.getId();

        repository.delete(milestone);
        em.flush();
        em.clear();

        // Milestone is gone
        assertThat(repository.findById(milestoneId)).isEmpty();
        // Tasks were cascade-deleted (orphanRemoval)
        Long taskCount = em.getEntityManager()
                .createQuery("SELECT COUNT(t) FROM Task t WHERE t.milestone.id = :mid", Long.class)
                .setParameter("mid", milestoneId)
                .getSingleResult();
        assertThat(taskCount).isZero();
    }
}