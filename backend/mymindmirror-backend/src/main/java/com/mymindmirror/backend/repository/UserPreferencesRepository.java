package com.mymindmirror.backend.repository;

import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.model.UserPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserPreferencesRepository extends JpaRepository<UserPreferences, UUID> {
    Optional<UserPreferences> findByUser(User user);
}