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
class TaskRepositoryTest {

    @Autowired
    private TaskRepository repository;

    @Autowired
    private MilestoneRepository milestoneRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager em;

    private User user;
    private User otherUser;
    private Milestone milestone;
    private Milestone otherMilestone;

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

        milestone = new Milestone();
        milestone.setUser(user);
        milestone.setTitle("My Milestone");
        milestone.setCreationDate(LocalDate.now());
        milestone.setStatus(Status.PENDING);
        milestone = milestoneRepository.save(milestone);

        otherMilestone = new Milestone();
        otherMilestone.setUser(otherUser);
        otherMilestone.setTitle("Other Milestone");
        otherMilestone.setCreationDate(LocalDate.now());
        otherMilestone.setStatus(Status.PENDING);
        otherMilestone = milestoneRepository.save(otherMilestone);
    }

    // ============================================================
    // Helpers
    // ============================================================

    private Task persistTask(Milestone m, String description, Status status, LocalDateTime createdAt) {
        Task task = new Task();
        task.setMilestone(m);
        task.setDescription(description);
        task.setCreationTimestamp(createdAt);
        task.setStatus(status);
        task.setDueDate(LocalDate.now().plusDays(7));
        m.getTasks().add(task);   //  CRITICAL: bidirectional link so cascade works
        return repository.save(task);
    }

    // ============================================================
    // findByMilestoneOrderByCreationTimestampAsc
    // ============================================================

    @Test
    @DisplayName("findByMilestoneOrderByCreationTimestampAsc returns only tasks of the given milestone")
    void findByMilestone_onlyThatMilestone() {
        persistTask(milestone, "Mine 1", Status.PENDING, LocalDateTime.now());
        persistTask(milestone, "Mine 2", Status.COMPLETED, LocalDateTime.now());
        persistTask(otherMilestone, "Theirs", Status.PENDING, LocalDateTime.now());

        List<Task> result = repository.findByMilestoneOrderByCreationTimestampAsc(milestone);

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(t -> t.getMilestone().getId().equals(milestone.getId()));
    }

    @Test
    @DisplayName("findByMilestoneOrderByCreationTimestampAsc returns tasks in ascending timestamp order")
    void findByMilestone_orderedByAscTimestamp() {
        LocalDateTime base = LocalDateTime.now().minusHours(1);

        persistTask(milestone, "Third", Status.PENDING, base.plusMinutes(30));
        persistTask(milestone, "First", Status.PENDING, base.plusMinutes(10));
        persistTask(milestone, "Second", Status.PENDING, base.plusMinutes(20));

        List<Task> result = repository.findByMilestoneOrderByCreationTimestampAsc(milestone);

        assertThat(result)
                .extracting(Task::getDescription)
                .containsExactly("First", "Second", "Third");
    }

    @Test
    @DisplayName("findByMilestoneOrderByCreationTimestampAsc returns empty list for milestone with no tasks")
    void findByMilestone_empty() {
        List<Task> result = repository.findByMilestoneOrderByCreationTimestampAsc(milestone);
        assertThat(result).isEmpty();
    }

    // ============================================================
    // findByIdAndMilestone
    // ============================================================

    @Test
    @DisplayName("findByIdAndMilestone returns task when ID and milestone match")
    void findByIdAndMilestone_found() {
        Task saved = persistTask(milestone, "Task", Status.PENDING, LocalDateTime.now());

        List<Task> result = repository.findByIdAndMilestone(saved.getId(), milestone);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(saved.getId());
    }

    @Test
    @DisplayName("findByIdAndMilestone returns empty for wrong milestone")
    void findByIdAndMilestone_wrongMilestone() {
        Task saved = persistTask(milestone, "Task", Status.PENDING, LocalDateTime.now());

        List<Task> result = repository.findByIdAndMilestone(saved.getId(), otherMilestone);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByIdAndMilestone returns empty for unknown ID")
    void findByIdAndMilestone_unknownId() {
        List<Task> result = repository.findByIdAndMilestone(UUID.randomUUID(), milestone);
        assertThat(result).isEmpty();
    }

    // ============================================================
    // findByMilestone_User
    // ============================================================

    @Test
    @DisplayName("findByMilestone_User returns all tasks belonging to the user's milestones")
    void findByMilestone_User_onlyOwnedTasks() {
        persistTask(milestone, "Mine 1", Status.PENDING, LocalDateTime.now());
        persistTask(milestone, "Mine 2", Status.COMPLETED, LocalDateTime.now());
        persistTask(otherMilestone, "Theirs 1", Status.PENDING, LocalDateTime.now());
        persistTask(otherMilestone, "Theirs 2", Status.PENDING, LocalDateTime.now());

        List<Task> result = repository.findByMilestone_User(user);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Task::getDescription)
                .containsExactlyInAnyOrder("Mine 1", "Mine 2");
    }

    @Test
    @DisplayName("findByMilestone_User aggregates tasks across multiple milestones of the same user")
    void findByMilestone_User_multipleMilestones() {
        Milestone second = new Milestone();
        second.setUser(user);
        second.setTitle("Second Milestone");
        second.setCreationDate(LocalDate.now());
        second.setStatus(Status.PENDING);
        second = milestoneRepository.save(second);

        persistTask(milestone, "M1 Task 1", Status.PENDING, LocalDateTime.now());
        persistTask(milestone, "M1 Task 2", Status.PENDING, LocalDateTime.now());
        persistTask(second, "M2 Task 1", Status.PENDING, LocalDateTime.now());

        List<Task> result = repository.findByMilestone_User(user);

        assertThat(result).hasSize(3);
    }

    @Test
    @DisplayName("findByMilestone_User returns empty for a user with no tasks")
    void findByMilestone_User_empty() {
        List<Task> result = repository.findByMilestone_User(user);
        assertThat(result).isEmpty();
    }

    // ============================================================
    // Cascade behaviour
    // ============================================================

    @Test
    @DisplayName("Deleting a milestone cascades to its tasks")
    void deleteMilestone_cascadesToTasks() {
        persistTask(milestone, "T1", Status.PENDING, LocalDateTime.now());
        persistTask(milestone, "T2", Status.COMPLETED, LocalDateTime.now());
        em.flush();

        UUID milestoneId = milestone.getId();

        milestoneRepository.delete(milestone);
        em.flush();
        em.clear();

        Long remaining = em.getEntityManager()
                .createQuery("SELECT COUNT(t) FROM Task t WHERE t.milestone.id = :mid", Long.class)
                .setParameter("mid", milestoneId)
                .getSingleResult();

        assertThat(remaining).isZero();
    }
}