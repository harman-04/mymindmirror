package com.mymindmirror.backend.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoadmapTest {

    // ============================================================
    // isFullyGenerated()
    // ============================================================

    @Test
    @DisplayName("isFullyGenerated() returns false when generatedWeeks < durationWeeks")
    void isFullyGenerated_falseWhenLess() {
        Roadmap roadmap = new Roadmap();
        roadmap.setGeneratedWeeks(2);
        roadmap.setDurationWeeks(4);
        assertThat(roadmap.isFullyGenerated()).isFalse();
    }

    @Test
    @DisplayName("isFullyGenerated() returns true when generatedWeeks == durationWeeks")
    void isFullyGenerated_trueWhenEqual() {
        Roadmap roadmap = new Roadmap();
        roadmap.setGeneratedWeeks(4);
        roadmap.setDurationWeeks(4);
        assertThat(roadmap.isFullyGenerated()).isTrue();
    }

    @Test
    @DisplayName("isFullyGenerated() returns true when generatedWeeks > durationWeeks")
    void isFullyGenerated_trueWhenExceeds() {
        Roadmap roadmap = new Roadmap();
        roadmap.setGeneratedWeeks(6);
        roadmap.setDurationWeeks(4);
        assertThat(roadmap.isFullyGenerated()).isTrue();
    }

    @Test
    @DisplayName("isFullyGenerated() returns false when generatedWeeks is null (short-circuit)")
    void isFullyGenerated_falseWhenGeneratedNull() {
        Roadmap roadmap = new Roadmap();
        roadmap.setGeneratedWeeks(null);
        roadmap.setDurationWeeks(4);
        assertThat(roadmap.isFullyGenerated()).isFalse();
    }

    @Test
    @DisplayName("isFullyGenerated() returns false when durationWeeks is null (hardened check)")
    void isFullyGenerated_falseWhenDurationNull() {
        Roadmap roadmap = new Roadmap();
        roadmap.setGeneratedWeeks(4);
        roadmap.setDurationWeeks(null);
        assertThat(roadmap.isFullyGenerated()).isFalse();
    }

    // ============================================================
    // Constructor / defaults
    // ============================================================

    @Test
    @DisplayName("Default constructor initializes collections and generatedWeeks=0")
    void defaultConstructor_setsDefaults() {
        Roadmap roadmap = new Roadmap();
        assertThat(roadmap.getTasks()).isNotNull().isEmpty();
        assertThat(roadmap.getResources()).isNotNull().isEmpty();
        assertThat(roadmap.getMilestones()).isNotNull().isEmpty();
        assertThat(roadmap.getGeneratedWeeks()).isZero();
    }

    @Test
    @DisplayName("Parameterized constructor sets createdAt and status")
    void parameterizedConstructor_setsDefaults() {
        User user = new User();
        Roadmap roadmap = new Roadmap(user, "Test Goal", "Description", 4);

        assertThat(roadmap.getUser()).isSameAs(user);
        assertThat(roadmap.getTitle()).isEqualTo("Test Goal");
        assertThat(roadmap.getDescription()).isEqualTo("Description");
        assertThat(roadmap.getDurationWeeks()).isEqualTo(4);
        assertThat(roadmap.getCreatedAt()).isNotNull();
        assertThat(roadmap.getStatus()).isEqualTo("PLANNED");
    }
}