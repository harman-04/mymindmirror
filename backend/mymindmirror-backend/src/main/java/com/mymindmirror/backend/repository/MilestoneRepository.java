package com.mymindmirror.backend.repository;

import com.mymindmirror.backend.model.Milestone;
import com.mymindmirror.backend.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for Milestone entities.
 * Provides methods for CRUD operations and fetching milestones by user.
 */
public interface MilestoneRepository extends JpaRepository<Milestone, UUID> {

    @EntityGraph(attributePaths = "tasks")
    List<Milestone> findByUserOrderByCreationDateDesc(User user);

    @EntityGraph(attributePaths = "tasks")
    List<Milestone> findByIdAndUser(UUID id, User user);
}