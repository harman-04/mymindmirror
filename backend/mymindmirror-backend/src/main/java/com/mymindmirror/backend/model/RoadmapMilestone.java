package com.mymindmirror.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "roadmap_milestones" ,
        indexes = @Index(name = "idx_roadmap_mil_roadmap", columnList = "roadmap_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RoadmapMilestone {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roadmap_id", nullable = false)
    @JsonIgnore
    private Roadmap roadmap;

    @Column(nullable = false)
    private String name;

    @Column(name = "week_number")
    private Integer weekNumber;

    private boolean achieved = false;
}