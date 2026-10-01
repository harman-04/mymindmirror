package com.mymindmirror.backend.repository;

import com.mymindmirror.backend.model.RoadmapTask;
import com.mymindmirror.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoadmapTaskRepository extends JpaRepository<RoadmapTask, UUID> {
    List<RoadmapTask> findByRoadmap_User(User user);

    @Query("SELECT rt FROM RoadmapTask rt JOIN FETCH rt.roadmap WHERE rt.id = :id")
    Optional<RoadmapTask> findByIdWithRoadmap(@Param("id") UUID id);
}
