package com.mymindmirror.backend.repository;

import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.model.UserStats;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserStatsRepository extends JpaRepository<UserStats, UUID> {
    Optional<UserStats> findByUser(User user);
}