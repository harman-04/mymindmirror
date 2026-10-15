package com.mymindmirror.backend.repository;

import com.mymindmirror.backend.model.CustomTask;
import com.mymindmirror.backend.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class CustomTaskRepositoryTest {

    @Autowired
    private CustomTaskRepository repository;

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

    private CustomTask persist(User owner, String title, boolean completed, LocalDate createdAt) {
        CustomTask t = new CustomTask();
        t.setUser(owner);
        t.setTitle(title);
        t.setPriority("MEDIUM");
        t.setCompleted(completed);
        t.setCreatedAt(createdAt);
        t.setEstimatedHours(1.0);
        return repository.save(t);
    }

    // ============================================================
    // findByUserOrderByCreatedAtDesc
    // ============================================================

    @Test
    @DisplayName("findByUserOrderByCreatedAtDesc returns only the user's tasks in DESC order")
    void findByUserOrderByCreatedAtDesc_isolatesAndOrders() {
        LocalDate today = LocalDate.now();
        persist(user, "Oldest", false, today.minusDays(10));
        persist(user, "Newest", false, today);
        persist(user, "Middle", false, today.minusDays(5));
        persist(otherUser, "Not Mine", false, today);

        List<CustomTask> result = repository.findByUserOrderByCreatedAtDesc(user);

        assertThat(result).hasSize(3);
        assertThat(result).extracting(CustomTask::getTitle)
                .containsExactly("Newest", "Middle", "Oldest");
    }

    @Test
    @DisplayName("findByUserOrderByCreatedAtDesc returns empty for new user")
    void findByUserOrderByCreatedAtDesc_empty() {
        assertThat(repository.findByUserOrderByCreatedAtDesc(user)).isEmpty();
    }

    // ============================================================
    // findByUserAndCompletedFalse
    // ============================================================

    @Test
    @DisplayName("findByUserAndCompletedFalse returns only incomplete tasks for the user")
    void findByUserAndCompletedFalse_filtersCompleted() {
        persist(user, "Todo 1", false, LocalDate.now());
        persist(user, "Todo 2", false, LocalDate.now());
        persist(user, "Done", true, LocalDate.now());
        persist(otherUser, "Not Mine", false, LocalDate.now());

        List<CustomTask> result = repository.findByUserAndCompletedFalse(user);

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(t -> !t.isCompleted());
    }

    @Test
    @DisplayName("findByUserAndCompletedFalse returns empty when all tasks completed")
    void findByUserAndCompletedFalse_allCompleted() {
        persist(user, "Done 1", true, LocalDate.now());
        persist(user, "Done 2", true, LocalDate.now());

        assertThat(repository.findByUserAndCompletedFalse(user)).isEmpty();
    }

    // ============================================================
    // findByUser
    // ============================================================

    @Test
    @DisplayName("findByUser returns all tasks for the user (completed and not)")
    void findByUser_returnsAll() {
        persist(user, "Todo", false, LocalDate.now());
        persist(user, "Done", true, LocalDate.now());
        persist(otherUser, "Not Mine", false, LocalDate.now());

        List<CustomTask> result = repository.findByUser(user);

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(t -> t.getUser().getId().equals(user.getId()));
    }

    @Test
    @DisplayName("findByUser returns empty for a fresh user")
    void findByUser_empty() {
        assertThat(repository.findByUser(user)).isEmpty();
    }
}