// src/main/java/com/mymindmirror/backend/repository/TaskRepository.java
package com.mymindmirror.backend.repository;

import com.mymindmirror.backend.model.Milestone;
import com.mymindmirror.backend.model.Task;
import com.mymindmirror.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {
    List<Task> findByMilestoneOrderByCreationTimestampAsc(Milestone milestone);
    List<Task> findByIdAndMilestone(UUID id, Milestone milestone);

    // Correct nested property method
    List<Task> findByMilestone_User(User user);
}
