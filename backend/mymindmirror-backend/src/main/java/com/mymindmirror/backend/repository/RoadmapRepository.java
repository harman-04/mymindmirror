package com.mymindmirror.backend.repository;

import com.mymindmirror.backend.model.Roadmap;
import com.mymindmirror.backend.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoadmapRepository extends JpaRepository<Roadmap, UUID> {

    @EntityGraph(value = "Roadmap.withTasksOnly", type = EntityGraph.EntityGraphType.LOAD)
    Optional<Roadmap> findById(UUID id);

    @EntityGraph(value = "Roadmap.withTasksOnly", type = EntityGraph.EntityGraphType.LOAD)
    List<Roadmap> findByUserOrderByCreatedAtDesc(User user);
}