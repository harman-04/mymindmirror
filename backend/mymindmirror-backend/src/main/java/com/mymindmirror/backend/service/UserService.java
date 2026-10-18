package com.mymindmirror.backend.service;

import com.mymindmirror.backend.constants.CacheConstants;
import com.mymindmirror.backend.model.*;
import com.mymindmirror.backend.payload.request.UpdateRoadmapPreferencesRequest;
import com.mymindmirror.backend.payload.request.UpdateUserPreferencesRequest;
import com.mymindmirror.backend.payload.request.UserProfileRequest;
import com.mymindmirror.backend.payload.response.UserFullProfileResponse;
import com.mymindmirror.backend.payload.response.UserPreferencesResponse;
import com.mymindmirror.backend.payload.response.UserRoadmapPreferencesDto;
import com.mymindmirror.backend.repository.*;
import com.mymindmirror.backend.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service class for managing User-related business logic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JournalEntryRepository journalEntryRepository;
    private final UserRoadmapPreferencesRepository userRoadmapPreferencesRepository;
    private final UserPreferencesRepository userPreferencesRepository;
    private final UserStatsRepository userStatsRepository;
    private final MilestoneRepository milestoneRepository;
    private final RoadmapRepository roadmapRepository;
    private final CustomTaskRepository customTaskRepository;
    private final ScheduledTaskRepository scheduledTaskRepository;
    private final EmbeddingGenerationService embeddingGenerationService;
    private final ChatMemoryService chatMemoryService;
    private final DailyJournalSummaryRepository dailyJournalSummaryRepository;



    public User registerNewUser(String username, String email, String password) {
        log.info("Attempting to register new user: {}", username);
        if (userRepository.existsByUsername(username)) {
            log.warn("Registration failed: Username '{}' already exists.", username);
            throw new IllegalArgumentException("Username already exists.");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            log.warn("Registration failed: Email '{}' already exists.", email);
            throw new IllegalArgumentException("Email already exists.");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));

        User savedUser = userRepository.save(user);
        log.info("User '{}' registered successfully with ID: {}", username, savedUser.getId());
        return savedUser;
    }

    public Optional<User> findByUsername(String username) {
        log.debug("Attempting to find user by username: {}", username);
        return userRepository.findByUsername(username);
    }

    public Optional<User> findById(UUID id) {
        log.debug("Attempting to find user by ID: {}", id);
        return userRepository.findById(id);
    }

    @CacheEvict(value = CacheConstants.USER_FULL_PROFILE, key = "#userId")
    public User updateUser(UUID userId, UserProfileRequest request) {
        log.info("Attempting to update user with ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User update failed: User with ID {} not found.", userId);
                    return new IllegalArgumentException("User not found.");
                });

        boolean changed = false;

        //  Use record accessors .username() and .email()
        if (request.username() != null && !request.username().trim().isEmpty() && !request.username().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.username())) {
                log.warn("User update failed: New username '{}' already exists.", request.username());
                throw new IllegalArgumentException("Username already taken.");
            }
            user.setUsername(request.username());
            changed = true;
            log.debug("Updated username to: {}", request.username());
        }

        if (request.email() != null && !request.email().trim().isEmpty() && !request.email().equals(user.getEmail())) {
            // Check if the new email exists for another user (not the current one)
            Optional<User> existingUserWithEmail = userRepository.findByEmail(request.email());
            if (existingUserWithEmail.isPresent() && !existingUserWithEmail.get().getId().equals(userId)) {
                log.warn("User update failed: New email '{}' already exists for another user.", request.email());
                throw new IllegalArgumentException("Email already taken by another user.");
            }
            user.setEmail(request.email());
            changed = true;
            log.debug("Updated email to: {}", request.email());
        }

        if (changed) {
            User updatedUser = userRepository.save(user);
            log.info("User with ID {} updated successfully.", userId);
            return updatedUser;
        } else {
            log.info("No changes detected for user with ID {}. Returning existing user.", userId);
            return user;
        }
    }

    @Caching(evict = {
            @CacheEvict(value = CacheConstants.USER_FULL_PROFILE, key = "#userId"),
            @CacheEvict(value = CacheConstants.USER_PREFERENCES_DTO, key = "#userId"),
            @CacheEvict(value = CacheConstants.GAMIFICATION_STATS, key = "#userId"),
            @CacheEvict(value = CacheConstants.API_KEY_STATUS, key = "#userId"),
            @CacheEvict(value = CacheConstants.KEY_PHRASE_FREQUENCIES, key = "#userId")
    })
    @Transactional
    public void deleteUser(UUID userId) {
        log.info("Attempting to delete user with ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        // Delete dependent data in correct order (child to parent)
        // 1. Scheduled tasks
        scheduledTaskRepository.deleteByUser(user);
        // 2. Custom tasks
        List<CustomTask> customTasks = customTaskRepository.findByUser(user);
        customTaskRepository.deleteAll(customTasks);
        // 3. Roadmap tasks, resources, milestones are cascade-deleted when roadmap is deleted
        List<Roadmap> roadmaps = roadmapRepository.findByUserOrderByCreatedAtDesc(user);
        roadmapRepository.deleteAll(roadmaps);
        // 4. Milestones (their tasks will be cascade-deleted if `@OneToMany` has `cascade = ALL`)
        List<Milestone> milestones = milestoneRepository.findByUserOrderByCreationDateDesc(user);
        milestoneRepository.deleteAll(milestones);
        // 5. Journal entries & Vector Embeddings
        List<JournalEntry> entries = journalEntryRepository.findByUser(user);
        for (JournalEntry entry : entries) {
            embeddingGenerationService.deleteEmbedding(entry.getId());
        }
        journalEntryRepository.deleteAll(entries);

        // 6. Daily Journal Summary (explicit delete, in case triggers miss)
        dailyJournalSummaryRepository.deleteByUserId(userId); // You'll need to add this method to the repository

        // 6. Redis Chat Sessions
        chatMemoryService.clearAllUserSessions(userId);

        // 7. UserStats
        userStatsRepository.findByUser(user).ifPresent(stats -> userStatsRepository.delete(stats));
        // 8. UserPreferences
        userPreferencesRepository.findByUser(user).ifPresent(prefs -> userPreferencesRepository.delete(prefs));
        // 9. UserRoadmapPreferences
        userRoadmapPreferencesRepository.findByUser(user).ifPresent(prefs -> userRoadmapPreferencesRepository.delete(prefs));

        //10  Finally delete the user
        userRepository.delete(user);
        log.info("User with ID {} and all associated data deleted successfully.", userId);
    }
    /**
     * Changes a user's password after verifying the current password.
     * @param userId The ID of the user whose password is to be changed.
     * @param currentPassword The user's current raw password.
     * @param newPassword The user's new raw password.
     * @throws IllegalArgumentException if user not found, current password is incorrect,
     * or new password is the same as the old password.
     */

    @CacheEvict(value = CacheConstants.USER_FULL_PROFILE, key = "#userId")
    @Transactional
    public void changeUserPassword(UUID userId, String currentPassword, String newPassword) {
        log.info("Attempting to change password for user ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Incorrect current password.");
        }

        // Check if new password is same as old
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("New password cannot be the same as the old password.");
        }

        // --- Re-encrypt all journal entries with the new password hash ---
        List<JournalEntry> userEntries = journalEntryRepository.findByUser(user);
        log.info("Re-encrypting {} journal entries for user: {}", userEntries.size(), user.getUsername());

        String oldPasswordHash = user.getPasswordHash(); // current hash (before change)
        String newPasswordHash = passwordEncoder.encode(newPassword);

        for (JournalEntry entry : userEntries) {
            // Decrypt with old password hash
            String decryptedText = EncryptionUtil.decrypt(entry.getRawText(), oldPasswordHash);
            if (decryptedText == null) {
                // If decryption fails (e.g., corrupted data), log and skip
                log.warn("Failed to decrypt entry {} for user {}. Skipping re-encryption.", entry.getId(), user.getUsername());
                continue;
            }
            // Encrypt with new password hash
            String newEncryptedText = EncryptionUtil.encrypt(decryptedText, newPasswordHash);
            if (newEncryptedText == null) {
                log.error("Failed to encrypt entry {} for user {}. Aborting password change.", entry.getId(), user.getUsername());
                throw new RuntimeException("Failed to re-encrypt journal entries. Password change aborted.");
            }
            entry.setRawText(newEncryptedText);
        }

        // Save all updated entries
        journalEntryRepository.saveAll(userEntries);
        log.info("Successfully re-encrypted {} entries for user: {}", userEntries.size(), user.getUsername());

        // Update password hash
        user.setPasswordHash(newPasswordHash);
        userRepository.save(user);

        log.info("Password changed successfully for user ID: {}", userId);
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    /**
     * Returns roadmap preferences as a DTO (cached).
     */
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConstants.USER_PREFERENCES_DTO, key = "#user.id")
    public UserRoadmapPreferencesDto getRoadmapPreferencesDto(User user) {
        UserRoadmapPreferences prefs = getRoadmapPreferences(user); // no cache on entity method
        return new UserRoadmapPreferencesDto(
                prefs.getDifficulty(),
                prefs.getLanguagePreference(),
                prefs.getLearningStyle(),
                prefs.getHoursPerWeek(),
                prefs.isAvoidWeekends()
        );
    }

    /**
     * Internal method – not cached, serves as a source of truth.
     * (If you want to keep caching on entity, you can, but it's okay to remove @Cacheable here.)
     */
    @Transactional(readOnly = true)
    public UserRoadmapPreferences getRoadmapPreferences(User user) {
        return userRoadmapPreferencesRepository.findByUser(user)
                .orElseGet(() -> createDefaultRoadmapPreferences(user));
    }

    @Caching(evict = {
            @CacheEvict(value = CacheConstants.USER_PREFERENCES_DTO, key = "#user.id"),
            @CacheEvict(value = CacheConstants.USER_FULL_PROFILE, key = "#user.id")
    })
    @Transactional
    public UserRoadmapPreferences updateRoadmapPreferences(User user, String difficulty, String languagePreference,
                                                           String learningStyle, Integer hoursPerWeek, Boolean avoidWeekends) {
        UserRoadmapPreferences prefs = getRoadmapPreferences(user);
        if (difficulty != null && !difficulty.isBlank()) prefs.setDifficulty(difficulty);
        if (languagePreference != null && !languagePreference.isBlank()) prefs.setLanguagePreference(languagePreference);
        if (learningStyle != null && !learningStyle.isBlank()) prefs.setLearningStyle(learningStyle);
        if (hoursPerWeek != null && hoursPerWeek >= 1) prefs.setHoursPerWeek(hoursPerWeek);
        if (avoidWeekends != null) prefs.setAvoidWeekends(avoidWeekends);
        return userRoadmapPreferencesRepository.save(prefs);
    }

    private UserRoadmapPreferences createDefaultRoadmapPreferences(User user) {
        UserRoadmapPreferences prefs = new UserRoadmapPreferences();
        prefs.setUser(user);
        prefs.setDifficulty("BEGINNER");
        prefs.setLanguagePreference("en");
        prefs.setLearningStyle("READING");
        prefs.setHoursPerWeek(10);
        prefs.setAvoidWeekends(false);
        return userRoadmapPreferencesRepository.save(prefs);
    }

    @Caching(evict = {
            @CacheEvict(value = CacheConstants.USER_PREFERENCES_DTO, key = "#user.id"),
            @CacheEvict(value = CacheConstants.USER_FULL_PROFILE, key = "#user.id")
    })
    @Transactional
    public UserRoadmapPreferencesDto updateRoadmapPreferences(User user, UpdateRoadmapPreferencesRequest request) {
        UserRoadmapPreferences updated = updateRoadmapPreferences(
                user,
                request.difficulty(),
                request.languagePreference(),
                request.learningStyle(),
                request.hoursPerWeek(),
                request.avoidWeekends()
        );
        return new UserRoadmapPreferencesDto(
                updated.getDifficulty(),
                updated.getLanguagePreference(),
                updated.getLearningStyle(),
                updated.getHoursPerWeek(),
                updated.isAvoidWeekends()
        );
    }

    @Cacheable(value = CacheConstants.USER_FULL_PROFILE, key = "#user.id")
    public UserFullProfileResponse getFullUserProfile(User user, String decryptedApiKey) {

        // API key status
        boolean usingOwnKey = decryptedApiKey != null && !decryptedApiKey.isBlank();
        String maskedKey = null;
        if (usingOwnKey) {
            int len = decryptedApiKey.length();
            maskedKey = "••••••••" + decryptedApiKey.substring(Math.max(0, len - 4));
        }

        // Roadmap preferences – now using the DTO directly from cache
        UserRoadmapPreferencesDto roadmapPrefsDto = getRoadmapPreferencesDto(user);

        // User Entity-based preferences
        UserPreferences prefs = userPreferencesRepository.findByUser(user)
                .orElseGet(() -> createDefaultPreferences(user));

        //  Construct the immutable record using the constructor. No more .setX()!
        return new UserFullProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                usingOwnKey,
                maskedKey,
                roadmapPrefsDto,
                prefs.getAvailableHoursJson(),
                prefs.getTimezone(),
                prefs.getEnergyPeak(),
                prefs.getWakeTime() != null ? prefs.getWakeTime().toString() : null,
                prefs.getSleepTime() != null ? prefs.getSleepTime().toString() : null,
                prefs.getLunchTime() != null ? prefs.getLunchTime().toString() : null,
                prefs.getDailyHabitsJson()
        );
    }

    private UserPreferences createDefaultPreferences(User user) {
        UserPreferences prefs = new UserPreferences();
        prefs.setUser(user);
        // Default: weekdays 9-12, 13-17
        String defaultHours = "{\"monday\":[[\"09:00\",\"12:00\"],[\"13:00\",\"17:00\"]],\"tuesday\":[[\"09:00\",\"12:00\"],[\"13:00\",\"17:00\"]],\"wednesday\":[[\"09:00\",\"12:00\"],[\"13:00\",\"17:00\"]],\"thursday\":[[\"09:00\",\"12:00\"],[\"13:00\",\"17:00\"]],\"friday\":[[\"09:00\",\"12:00\"],[\"13:00\",\"17:00\"]],\"saturday\":[],\"sunday\":[]}";
        prefs.setAvailableHoursJson(defaultHours);
        prefs.setTimezone("Asia/Kolkata");
        return userPreferencesRepository.save(prefs);
    }

    public UserPreferences getUserPreferences(User user) {
        return userPreferencesRepository.findByUser(user)
                .orElseGet(() -> createDefaultPreferences(user));
    }

    @CacheEvict(value = CacheConstants.USER_FULL_PROFILE, key = "#user.id")
    @Transactional
    public UserPreferencesResponse updateUserPreferences(User user, UpdateUserPreferencesRequest request) {
        UserPreferences prefs = userPreferencesRepository.findByUser(user)
                .orElseGet(() -> createDefaultPreferences(user));

        if (request.availableHoursJson() != null) {
            prefs.setAvailableHoursJson(request.availableHoursJson());
        }
        if (request.timezone() != null) {
            prefs.setTimezone(request.timezone());
        }
        if (request.energyPeak() != null) {
            prefs.setEnergyPeak(request.energyPeak());
        }
        if (request.wakeTime() != null) {
            prefs.setWakeTime(request.wakeTime());
        }
        if (request.sleepTime() != null) {
            prefs.setSleepTime(request.sleepTime());
        }
        if (request.lunchTime() != null) {
            prefs.setLunchTime(request.lunchTime());
        }
        if (request.dailyHabitsJson() != null) {
            prefs.setDailyHabitsJson(request.dailyHabitsJson());
        }

        UserPreferences saved = userPreferencesRepository.save(prefs);
        return new UserPreferencesResponse(
                saved.getAvailableHoursJson(),
                saved.getTimezone(),
                saved.getEnergyPeak(),
                saved.getWakeTime() != null ? saved.getWakeTime().toString() : null,
                saved.getSleepTime() != null ? saved.getSleepTime().toString() : null,
                saved.getLunchTime() != null ? saved.getLunchTime().toString() : null,
                saved.getDailyHabitsJson()
        );
    }
}