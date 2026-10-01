package com.mymindmirror.backend.repository;

import com.mymindmirror.backend.model.User;
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
class UserRepositoryTest {

    @Autowired
    private UserRepository repository;

    @Autowired
    private TestEntityManager em;

    private User persisted;

    @BeforeEach
    void setUp() {
        User u = new User();
        u.setUsername("testuser");
        u.setEmail("test@example.com");
        u.setPasswordHash("hashed");
        persisted = repository.save(u);
    }

    // ============================================================
    // findByUsername
    // ============================================================

    @Test
    @DisplayName("findByUsername returns user when username exists")
    void findByUsername_found() {
        Optional<User> result = repository.findByUsername("testuser");
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(persisted.getId());
    }

    @Test
    @DisplayName("findByUsername returns empty for unknown username")
    void findByUsername_notFound() {
        assertThat(repository.findByUsername("ghost")).isEmpty();
    }

    @Test
    @DisplayName("findByUsername is case-sensitive")
    void findByUsername_caseSensitive() {
        assertThat(repository.findByUsername("TESTUSER")).isEmpty();
    }

    // ============================================================
    // existsByUsername
    // ============================================================

    @Test
    @DisplayName("existsByUsername returns true for existing user")
    void existsByUsername_true() {
        assertThat(repository.existsByUsername("testuser")).isTrue();
    }

    @Test
    @DisplayName("existsByUsername returns false for unknown user")
    void existsByUsername_false() {
        assertThat(repository.existsByUsername("ghost")).isFalse();
    }

    // ============================================================
    // findById
    // ============================================================

    @Test
    @DisplayName("findById returns user when ID exists")
    void findById_found() {
        Optional<User> result = repository.findById(persisted.getId());
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("findById returns empty for unknown ID")
    void findById_notFound() {
        assertThat(repository.findById(UUID.randomUUID())).isEmpty();
    }

    // ============================================================
    // findByEmail
    // ============================================================

    @Test
    @DisplayName("findByEmail returns user when email exists")
    void findByEmail_found() {
        Optional<User> result = repository.findByEmail("test@example.com");
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(persisted.getId());
    }

    @Test
    @DisplayName("findByEmail returns empty for unknown email")
    void findByEmail_notFound() {
        assertThat(repository.findByEmail("nobody@example.com")).isEmpty();
    }

    // ============================================================
    // existsByEmail
    // ============================================================

    @Test
    @DisplayName("existsByEmail returns true for existing email")
    void existsByEmail_true() {
        assertThat(repository.existsByEmail("test@example.com")).isTrue();
    }

    @Test
    @DisplayName("existsByEmail returns false for unknown email")
    void existsByEmail_false() {
        assertThat(repository.existsByEmail("nobody@example.com")).isFalse();
    }

    // ============================================================
    // Unique constraints
    // ============================================================

    @Test
    @DisplayName("Saving a user with duplicate username throws DataIntegrityViolationException")
    void duplicateUsername_throws() {
        User dup = new User();
        dup.setUsername("testuser");
        dup.setEmail("different@example.com");
        dup.setPasswordHash("hashed");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> {
            repository.saveAndFlush(dup);
        }).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Saving a user with duplicate email throws DataIntegrityViolationException")
    void duplicateEmail_throws() {
        User dup = new User();
        dup.setUsername("differentuser");
        dup.setEmail("test@example.com");
        dup.setPasswordHash("hashed");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> {
            repository.saveAndFlush(dup);
        }).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
}