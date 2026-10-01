package com.mymindmirror.backend.repository;

import com.mymindmirror.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA Repository for User entities.
 * Provides standard CRUD operations and custom query methods for User data.
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    Boolean existsByUsername(String username);

    Optional<User> findById(UUID id);

    /**
     * Finds a User by their email address.
     * @param email The email to search for.
     * @return An Optional containing the User if found, or empty if not.
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks if a User with the given email already exists.
     * @param email The email to check.
     * @return True if a user with this email exists, false otherwise.
     */
    Boolean existsByEmail(String email);
}
