package com.mymindmirror.backend.repository;

import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.model.UserPreferences;
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
class UserPreferencesRepositoryTest {

    @Autowired
    private UserPreferencesRepository repository;

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

    private UserPreferences persist(User owner) {
        UserPreferences p = new UserPreferences();
        p.setUser(owner);
        p.setTimezone("UTC");
        return repository.save(p);
    }

    @Test
    @DisplayName("findByUser returns preferences for the matching user")
    void findByUser_found() {
        UserPreferences saved = persist(user);

        Optional<UserPreferences> result = repository.findByUser(user);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    @DisplayName("findByUser returns empty when user has no preferences")
    void findByUser_empty() {
        assertThat(repository.findByUser(user)).isEmpty();
    }

    @Test
    @DisplayName("findByUser isolates by user (other user's prefs not returned)")
    void findByUser_isolatesByUser() {
        persist(otherUser);

        Optional<UserPreferences> result = repository.findByUser(user);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Saving two preferences for the same user violates the unique constraint")
    void duplicateUserPreferences_throws() {
        persist(user);

        UserPreferences dup = new UserPreferences();
        dup.setUser(user);
        dup.setTimezone("Asia/Kolkata");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> repository.saveAndFlush(dup))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
}