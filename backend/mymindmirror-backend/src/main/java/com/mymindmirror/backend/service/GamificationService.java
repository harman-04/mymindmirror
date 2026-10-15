package com.mymindmirror.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mymindmirror.backend.constants.CacheConstants;
import com.mymindmirror.backend.enums.GamificationAction;
import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.model.UserStats;
import com.mymindmirror.backend.payload.response.UserStatsResponse;
import com.mymindmirror.backend.repository.UserStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class GamificationService {

    private final UserStatsRepository userStatsRepository;
    private final ObjectMapper objectMapper;

    // Constants for XP Rewards
    private static final int XP_PER_JOURNAL = 50;
    private static final int XP_PER_SCHEDULE = 30;
    private static final int XP_PER_ROADMAP = 30;
    private static final int XP_PER_TASK_COMPLETE = 20;
    private static final int XP_PER_INSIGHT = 20;
    private static final int XP_PER_CHAT = 15;
    private static final int XP_PER_REFLECTION = 15;
    private static final int XP_PER_MILESTONE_CREATE = 10;
    private static final int XP_PER_TASK_CREATE = 5;
    private static final int XP_PER_ELABORATE = 5;

    private static final int XP_PER_LEVEL = 500;

    @Transactional
    public UserStats initializeStats(User user) {
        UserStats stats = new UserStats();
        stats.setUser(user);
        stats.setCurrentStreak(0);
        stats.setLongestStreak(0);
        stats.setLastActiveDate(null);
        stats.setBadgesJson("[]");
        stats.setTotalTasksCompleted(0);
        stats.setExperiencePoints(0);
        stats.setLevel(1);
        stats.setTotalJournalEntries(0);
        stats.setTotalChats(0);
        stats.setSchedulesGenerated(0);
        return userStatsRepository.save(stats);
    }

    @CacheEvict(value = CacheConstants.GAMIFICATION_STATS, key = "#user.id")
    @Transactional
    public UserStats recordActivity(User user, GamificationAction action) {
        UserStats stats = userStatsRepository.findByUser(user)
                .orElseGet(() -> initializeStats(user));

        LocalDate today = LocalDate.now();
        LocalDate lastActive = stats.getLastActiveDate();

        // 1. STREAK LOGIC
        if (lastActive == null) {
            stats.setCurrentStreak(1);
        } else if (!lastActive.equals(today)) {
            if (lastActive.equals(today.minusDays(1))) {
                stats.setCurrentStreak(stats.getCurrentStreak() + 1);
            } else {
                stats.setCurrentStreak(1);
            }
        }
        stats.setLastActiveDate(today);
        if (stats.getCurrentStreak() > stats.getLongestStreak()) {
            stats.setLongestStreak(stats.getCurrentStreak());
        }

        Set<String> badges = getBadgesSet(stats);
        boolean badgesChanged = false;

        // 2. INCREMENT COUNTERS, ADD XP, AND AWARD INSTANT BADGES
        switch (action) {
            case TASK:
                stats.setTotalTasksCompleted(stats.getTotalTasksCompleted() + 1);
                stats.setExperiencePoints(stats.getExperiencePoints() + XP_PER_TASK_COMPLETE);
                if (badges.add("FIRST_STEP")) badgesChanged = true;
                if (stats.getTotalTasksCompleted() >= 10 && badges.add("TASK_MASTER")) badgesChanged = true;
                if (stats.getTotalTasksCompleted() >= 50 && badges.add("PRODUCTIVITY_NINJA")) badgesChanged = true;
                break;
            case JOURNAL:
                stats.setTotalJournalEntries(stats.getTotalJournalEntries() + 1);
                stats.setExperiencePoints(stats.getExperiencePoints() + XP_PER_JOURNAL);
                if (badges.add("FIRST_THOUGHT")) badgesChanged = true;
                if (stats.getTotalJournalEntries() >= 5 && badges.add("REFLECTIVE_SOUL")) badgesChanged = true;
                break;
            case SCHEDULE:
                stats.setSchedulesGenerated(stats.getSchedulesGenerated() + 1);
                stats.setExperiencePoints(stats.getExperiencePoints() + XP_PER_SCHEDULE);
                if (badges.add("TIME_LORD")) badgesChanged = true;

                break;
            case CHAT:
                stats.setTotalChats(stats.getTotalChats() + 1);
                stats.setExperiencePoints(stats.getExperiencePoints() + XP_PER_CHAT);
                if (badges.add("FIRST_CHAT")) badgesChanged = true;
                if (stats.getTotalChats() >= 20 && badges.add("AI_WHISPERER")) badgesChanged = true;
                break;
            case TASK_CREATE:
                stats.setExperiencePoints(stats.getExperiencePoints() + XP_PER_TASK_CREATE);
                break;
            case MILESTONE_CREATE:
                stats.setExperiencePoints(stats.getExperiencePoints() + XP_PER_MILESTONE_CREATE);
                if (badges.add("VISIONARY")) badgesChanged = true;
                break;
            case ROADMAP_GENERATE:
                stats.setExperiencePoints(stats.getExperiencePoints() + XP_PER_ROADMAP);
                if (badges.add("ARCHITECT")) badgesChanged = true;
                break;
            case AI_REFLECTION:
                stats.setExperiencePoints(stats.getExperiencePoints() + XP_PER_REFLECTION);
                if (badges.add("INTROSPECTIVE")) badgesChanged = true;
                break;
            case AI_INSIGHT:
                stats.setExperiencePoints(stats.getExperiencePoints() + XP_PER_INSIGHT);
                break;
            case ELABORATE_TASK:
                stats.setExperiencePoints(stats.getExperiencePoints() + XP_PER_ELABORATE);
                if (badges.add("DEEP_DIVER")) badgesChanged = true;
                break;
        }

        // 3. CALCULATE LEVEL
        int calculatedLevel = 1 + (stats.getExperiencePoints() / XP_PER_LEVEL);
        stats.setLevel(calculatedLevel);

        // 4. STREAK BADGES
        if (stats.getCurrentStreak() >= 3 && badges.add("THREE_DAY_STREAK")) badgesChanged = true;
        if (stats.getCurrentStreak() >= 7 && badges.add("SEVEN_DAY_STREAK")) badgesChanged = true;
        if (stats.getCurrentStreak() >= 30 && badges.add("THIRTY_DAY_LEGEND")) badgesChanged = true;

        if (badgesChanged) {
            try {
                stats.setBadgesJson(objectMapper.writeValueAsString(badges));
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize badges", e);
            }
        }

        return userStatsRepository.save(stats);
    }

    @CacheEvict(value = CacheConstants.GAMIFICATION_STATS, key = "#user.id")
    @Transactional
    public void awardRoadmapCompletedBadge(User user) {
        UserStats stats = userStatsRepository.findByUser(user)
                .orElseGet(() -> initializeStats(user));
        Set<String> badges = getBadgesSet(stats);
        if (badges.add("ROADMAP_FINISHER")) {
            try {
                stats.setBadgesJson(objectMapper.writeValueAsString(badges));
                userStatsRepository.save(stats);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize badges", e);
            }
        }
    }

    private Set<String> getBadgesSet(UserStats stats) {
        try {
            if (stats.getBadgesJson() != null && !stats.getBadgesJson().isBlank()) {
                return objectMapper.readValue(stats.getBadgesJson(), new TypeReference<Set<String>>() {});
            }
        } catch (Exception e) {
            log.error("Failed to parse badges JSON", e);
        }
        return new HashSet<>();
    }

    @Cacheable(value = CacheConstants.GAMIFICATION_STATS, key = "#user.id")
    public UserStatsResponse getUserStats(User user) {
        UserStats stats = userStatsRepository.findByUser(user)
                .orElseGet(() -> initializeStats(user));
        return new UserStatsResponse(
                stats.getCurrentStreak(), stats.getLongestStreak(), stats.getLastActiveDate(),
                getBadgesSet(stats), stats.getTotalTasksCompleted(), stats.getExperiencePoints(),
                stats.getLevel(), stats.getTotalJournalEntries(), stats.getTotalChats(), stats.getSchedulesGenerated()
        );
    }
}