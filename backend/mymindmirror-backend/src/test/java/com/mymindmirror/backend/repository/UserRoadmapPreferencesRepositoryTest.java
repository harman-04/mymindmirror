package com.mymindmirror.backend.repository;

import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.model.UserRoadmapPreferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class UserRoadmapPreferencesRepositoryTest {

    @Autowired
    private UserRoadmapPreferencesRepository repository;

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

    private UserRoadmapPreferences persist(User owner) {
        UserRoadmapPreferences p = new UserRoadmapPreferences();
        p.setUser(owner);
        return repository.save(p);
    }

    @Test
    @DisplayName("findByUser returns preferences for the matching user")
    void findByUser_found() {
        UserRoadmapPreferences saved = persist(user);

        Optional<UserRoadmapPreferences> result = repository.findByUser(user);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(saved.getId());
        // Defaults from the entity
        assertThat(result.get().getDifficulty()).isEqualTo("BEGINNER");
    }

    @Test
    @DisplayName("findByUser returns empty when user has no preferences")
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
    @DisplayName("Unique constraint prevents two preference rows for the same user")
    void duplicateUserPreferences_throws() {
        persist(user);

        UserRoadmapPreferences dup = new UserRoadmapPreferences();
        dup.setUser(user);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> repository.saveAndFlush(dup))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
}