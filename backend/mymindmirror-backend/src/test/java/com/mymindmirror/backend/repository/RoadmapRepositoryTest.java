package com.mymindmirror.backend.repository;

import com.mymindmirror.backend.model.Roadmap;
import com.mymindmirror.backend.model.RoadmapMilestone;
import com.mymindmirror.backend.model.RoadmapResource;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class RoadmapRepositoryTest {

    @Autowired
    private RoadmapRepository repository;

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

    private Roadmap persistRoadmap(User owner, String title, LocalDate createdAt) {
        Roadmap roadmap = new Roadmap();
        roadmap.setUser(owner);
        roadmap.setTitle(title);
        roadmap.setDescription("Description for " + title);
        roadmap.setCreatedAt(createdAt);
        roadmap.setStatus("ACTIVE");
        roadmap.setDurationWeeks(4);
        roadmap.setGeneratedWeeks(4);
        roadmap.setOriginalDurationValue(4);
        roadmap.setOriginalDurationUnit("WEEKS");
        return repository.save(roadmap);
    }

    private RoadmapTask persistTask(Roadmap roadmap, String description, int week, int day) {
        RoadmapTask task = new RoadmapTask();
        task.setRoadmap(roadmap);
        task.setDescription(description);
        task.setWeekNumber(week);
        task.setDayNumber(day);
        task.setTaskType("daily");
        task.setCompleted(false);
        task.setImportedToMilestone(false);
        roadmap.getTasks().add(task);
        return task;
    }

    private RoadmapResource persistResource(Roadmap roadmap, String name, String url, String type) {
        RoadmapResource r = new RoadmapResource();
        r.setRoadmap(roadmap);
        r.setName(name);
        r.setUrl(url);
        r.setType(type);
        roadmap.getResources().add(r);
        return r;
    }

    private RoadmapMilestone persistMilestone(Roadmap roadmap, String name, int week) {
        RoadmapMilestone m = new RoadmapMilestone();
        m.setRoadmap(roadmap);
        m.setName(name);
        m.setWeekNumber(week);
        m.setAchieved(false);
        roadmap.getMilestones().add(m);
        return m;
    }

    // ============================================================
    // findByUserOrderByCreatedAtDesc
    // ============================================================

    @Test
    @DisplayName("findByUserOrderByCreatedAtDesc returns only the user's roadmaps")
    void findByUser_onlyUserRoadmaps() {
        persistRoadmap(user, "Mine 1", LocalDate.now());
        persistRoadmap(user, "Mine 2", LocalDate.now().minusDays(1));
        persistRoadmap(otherUser, "Theirs", LocalDate.now());

        List<Roadmap> result = repository.findByUserOrderByCreatedAtDesc(user);

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(r -> r.getUser().getId().equals(user.getId()));
    }

    @Test
    @DisplayName("findByUserOrderByCreatedAtDesc returns roadmaps in descending createdAt order")
    void findByUser_ordersByCreatedAtDesc() {
        LocalDate today = LocalDate.now();
        persistRoadmap(user, "Oldest", today.minusDays(10));
        persistRoadmap(user, "Middle", today.minusDays(5));
        persistRoadmap(user, "Newest", today);

        List<Roadmap> result = repository.findByUserOrderByCreatedAtDesc(user);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getTitle()).isEqualTo("Newest");
        assertThat(result.get(1).getTitle()).isEqualTo("Middle");
        assertThat(result.get(2).getTitle()).isEqualTo("Oldest");
    }

    @Test
    @DisplayName("findByUserOrderByCreatedAtDesc returns empty list for user with no roadmaps")
    void findByUser_emptyForNewUser() {
        List<Roadmap> result = repository.findByUserOrderByCreatedAtDesc(user);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByUserOrderByCreatedAtDesc eagerly loads tasks via @EntityGraph")
    void findByUser_eagerlyLoadsTasks() {
        Roadmap roadmap = persistRoadmap(user, "With Tasks", LocalDate.now());
        persistTask(roadmap, "Task 1", 1, 1);
        persistTask(roadmap, "Task 2", 1, 2);
        persistTask(roadmap, "Task 3", 2, 1);
        repository.save(roadmap);
        em.flush();
        em.clear();

        List<Roadmap> result = repository.findByUserOrderByCreatedAtDesc(user);

        assertThat(result).hasSize(1);
        //  Tasks are eagerly loaded – no LazyInitializationException
        assertThat(result.get(0).getTasks()).hasSize(3);
    }

    @Test
    @DisplayName("findByUserOrderByCreatedAtDesc does not cause Cartesian product for tasks")
    void findByUser_noDuplicateTasks() {
        Roadmap roadmap = persistRoadmap(user, "Cartesian Test", LocalDate.now());
        persistTask(roadmap, "Task A", 1, 1);
        persistTask(roadmap, "Task B", 1, 2);
        persistTask(roadmap, "Task C", 1, 3);
        // Add resources to historically trigger Cartesian issues
        persistResource(roadmap, "Res 1", "https://a.com", "article");
        persistResource(roadmap, "Res 2", "https://b.com", "video");
        persistMilestone(roadmap, "Mile 1", 2);
        repository.save(roadmap);
        em.flush();
        em.clear();

        List<Roadmap> result = repository.findByUserOrderByCreatedAtDesc(user);

        //  Only 3 tasks – not 3 × 2 × 1 = 6 (Cartesian product)
        assertThat(result.get(0).getTasks()).hasSize(3);
    }

    @Test
    @DisplayName("findByUserOrderByCreatedAtDesc returns roadmaps with no tasks")
    void findByUser_handlesEmptyTasks() {
        persistRoadmap(user, "Empty", LocalDate.now());
        em.flush();
        em.clear();

        List<Roadmap> result = repository.findByUserOrderByCreatedAtDesc(user);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTasks()).isEmpty();
    }

    @Test
    @DisplayName("findByUserOrderByCreatedAtDesc loads resources and milestones (via @BatchSize) inside a transaction")
    void findByUser_loadsResourcesAndMilestones() {
        Roadmap roadmap = persistRoadmap(user, "With Extras", LocalDate.now());
        persistTask(roadmap, "Task", 1, 1);
        persistResource(roadmap, "Article", "https://x.com", "article");
        persistResource(roadmap, "Video", "https://y.com", "video");
        persistMilestone(roadmap, "Milestone 1", 2);
        persistMilestone(roadmap, "Milestone 2", 4);
        repository.save(roadmap);
        em.flush();
        em.clear();

        List<Roadmap> result = repository.findByUserOrderByCreatedAtDesc(user);

        // We're inside @DataJpaTest's transaction, so lazy loading works fine
        Roadmap loaded = result.get(0);
        assertThat(loaded.getResources()).hasSize(2);
        assertThat(loaded.getMilestones()).hasSize(2);
    }

    // ============================================================
    // findById (with @EntityGraph override)
    // ============================================================

    @Test
    @DisplayName("findById returns roadmap with tasks eagerly loaded")
    void findById_loadsTasksEagerly() {
        Roadmap roadmap = persistRoadmap(user, "By ID", LocalDate.now());
        persistTask(roadmap, "Task 1", 1, 1);
        persistTask(roadmap, "Task 2", 1, 2);
        repository.save(roadmap);
        em.flush();
        em.clear();

        Optional<Roadmap> result = repository.findById(roadmap.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getTasks()).hasSize(2);
        assertThat(result.get().getTitle()).isEqualTo("By ID");
    }

    @Test
    @DisplayName("findById returns empty for unknown ID")
    void findById_notFound() {
        Optional<Roadmap> result = repository.findById(UUID.randomUUID());
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findById returns roadmap with empty tasks")
    void findById_noTasks() {
        Roadmap roadmap = persistRoadmap(user, "No Tasks", LocalDate.now());
        em.flush();
        em.clear();

        Optional<Roadmap> result = repository.findById(roadmap.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getTasks()).isEmpty();
    }

    @Test
    @DisplayName("findById returns roadmap with all extras (resources + milestones)")
    void findById_loadsEverything() {
        Roadmap roadmap = persistRoadmap(user, "Full", LocalDate.now());
        persistTask(roadmap, "T1", 1, 1);
        persistResource(roadmap, "R1", "https://a.com", "article");
        persistMilestone(roadmap, "M1", 1);
        repository.save(roadmap);
        em.flush();
        em.clear();

        Optional<Roadmap> result = repository.findById(roadmap.getId());

        assertThat(result).isPresent();
        Roadmap loaded = result.get();
        assertThat(loaded.getTasks()).hasSize(1);
        assertThat(loaded.getResources()).hasSize(1);
        assertThat(loaded.getMilestones()).hasSize(1);
        assertThat(loaded.getUser().getUsername()).isEqualTo("testuser");
    }

    // ============================================================
    // Task ordering inside the entity graph
    // ============================================================

    @Test
    @DisplayName("Tasks are ordered by weekNumber then dayNumber (@OrderBy on entity)")
    void findByUser_tasksOrderedByWeekThenDay() {
        Roadmap roadmap = persistRoadmap(user, "Ordered", LocalDate.now());
        // Insert out of order
        persistTask(roadmap, "Week 2 Day 1", 2, 1);
        persistTask(roadmap, "Week 1 Day 2", 1, 2);
        persistTask(roadmap, "Week 1 Day 1", 1, 1);
        persistTask(roadmap, "Week 3 Day 1", 3, 1);
        repository.save(roadmap);
        em.flush();
        em.clear();

        Roadmap loaded = repository.findById(roadmap.getId()).orElseThrow();

        List<RoadmapTask> tasks = new ArrayList<>(loaded.getTasks());
        assertThat(tasks)
                .extracting(RoadmapTask::getDescription)
                .containsExactly(
                        "Week 1 Day 1",
                        "Week 1 Day 2",
                        "Week 2 Day 1",
                        "Week 3 Day 1");
    }
}