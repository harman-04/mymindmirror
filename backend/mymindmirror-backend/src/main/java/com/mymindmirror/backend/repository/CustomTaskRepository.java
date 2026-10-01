package com.mymindmirror.backend.repository;

import com.mymindmirror.backend.model.CustomTask;
import com.mymindmirror.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CustomTaskRepository extends JpaRepository<CustomTask, UUID> {
    List<CustomTask> findByUserOrderByCreatedAtDesc(User user);
    List<CustomTask> findByUserAndCompletedFalse(User user);

    List<CustomTask> findByUser(User user);
}