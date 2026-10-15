package com.mymindmirror.backend.repository;

import com.mymindmirror.backend.model.Roadmap;
import com.mymindmirror.backend.model.RoadmapTask;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class RoadmapTaskRepositoryTest {

    @Autowired
    private RoadmapTaskRepository repository;

    @Autowired
    private RoadmapRepository roadmapRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager em;

    private User user;
    private User otherUser;
    private Roadmap roadmap;
    private Roadmap otherRoadmap;

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

        roadmap = new Roadmap();
        roadmap.setUser(user);
        roadmap.setTitle("My Roadmap");
        roadmap.setCreatedAt(LocalDate.now());
        roadmap.setStatus("ACTIVE");
        roadmap.setDurationWeeks(4);
        roadmap = roadmapRepository.save(roadmap);

        otherRoadmap = new Roadmap();
        otherRoadmap.setUser(otherUser);
        otherRoadmap.setTitle("Other Roadmap");
        otherRoadmap.setCreatedAt(LocalDate.now());
        otherRoadmap.setStatus("ACTIVE");
        otherRoadmap.setDurationWeeks(4);
        otherRoadmap = roadmapRepository.save(otherRoadmap);
    }

    private RoadmapTask persist(Roadmap r, String description) {
        RoadmapTask t = new RoadmapTask();
        t.setRoadmap(r);
        t.setDescription(description);
        t.setWeekNumber(1);
        t.setDayNumber(1);
        t.setTaskType("daily");
        t.setCompleted(false);
        t.setImportedToMilestone(false);
        return repository.save(t);
    }

    // ============================================================
    // findByRoadmap_User
    // ============================================================

    @Test
    @DisplayName("findByRoadmap_User returns only the user's roadmap tasks")
    void findByRoadmap_User_isolatesByUser() {
        persist(roadmap, "Mine 1");
        persist(roadmap, "Mine 2");
        persist(otherRoadmap, "Theirs 1");

        List<RoadmapTask> result = repository.findByRoadmap_User(user);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(RoadmapTask::getDescription)
                .containsExactlyInAnyOrder("Mine 1", "Mine 2");
    }

    @Test
    @DisplayName("findByRoadmap_User aggregates tasks across multiple roadmaps of the same user")
    void findByRoadmap_User_multipleRoadmaps() {
        Roadmap second = new Roadmap();
        second.setUser(user);
        second.setTitle("Second Roadmap");
        second.setCreatedAt(LocalDate.now());
        second.setStatus("ACTIVE");
        second.setDurationWeeks(4);
        second = roadmapRepository.save(second);

        persist(roadmap, "R1 Task 1");
        persist(second, "R2 Task 1");
        persist(second, "R2 Task 2");

        List<RoadmapTask> result = repository.findByRoadmap_User(user);

        assertThat(result).hasSize(3);
    }

    @Test
    @DisplayName("findByRoadmap_User returns empty for a user with no roadmap tasks")
    void findByRoadmap_User_empty() {
        assertThat(repository.findByRoadmap_User(user)).isEmpty();
    }

    // ============================================================
// findByIdWithRoadmap (JPQL with JOIN FETCH)
// ============================================================

    @Test
    @DisplayName("findByIdWithRoadmap eagerly loads the parent roadmap via JOIN FETCH")
    void findByIdWithRoadmap_loadsRoadmapEagerly() {
        RoadmapTask saved = persist(roadmap, "Elaborate Me");
        em.flush();
        em.clear();

        Optional<RoadmapTask> result = repository.findByIdWithRoadmap(saved.getId());

        assertThat(result).isPresent();
        //  Accessing roadmap.getTitle() must NOT throw LazyInitializationException
        assertThat(result.get().getRoadmap()).isNotNull();
        assertThat(result.get().getRoadmap().getTitle()).isEqualTo("My Roadmap");
        assertThat(result.get().getRoadmap().getUser().getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("findByIdWithRoadmap returns empty for unknown ID")
    void findByIdWithRoadmap_unknownId() {
        Optional<RoadmapTask> result = repository.findByIdWithRoadmap(UUID.randomUUID());
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByIdWithRoadmap returns exactly one result (no duplicates from JOIN)")
    void findByIdWithRoadmap_returnsSingleResult() {
        RoadmapTask saved = persist(roadmap, "Unique");
        // Add sibling tasks to ensure JOIN doesn't multiply rows
        persist(roadmap, "Sibling 1");
        persist(roadmap, "Sibling 2");
        em.flush();
        em.clear();

        Optional<RoadmapTask> result = repository.findByIdWithRoadmap(saved.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(saved.getId());
        assertThat(result.get().getDescription()).isEqualTo("Unique");
    }

    @Test
    @DisplayName("findByIdWithRoadmap works for tasks of any user (no user filter in query)")
    void findByIdWithRoadmap_crossesUserBoundary() {
        // NOTE: This method does NOT filter by user in the query.
        // Authorization is enforced at the SERVICE layer (checks roadmap.user).
        // The test documents this behaviour so it's explicit and intentional.
        RoadmapTask otherTask = persist(otherRoadmap, "Other's Task");
        em.flush();
        em.clear();

        Optional<RoadmapTask> result = repository.findByIdWithRoadmap(otherTask.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getRoadmap().getUser().getUsername()).isEqualTo("otheruser");
    }
}