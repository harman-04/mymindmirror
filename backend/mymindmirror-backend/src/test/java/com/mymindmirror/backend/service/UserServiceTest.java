package com.mymindmirror.backend.service;

import com.mymindmirror.backend.model.*;
import com.mymindmirror.backend.payload.request.UpdateRoadmapPreferencesRequest;
import com.mymindmirror.backend.payload.request.UpdateUserPreferencesRequest;
import com.mymindmirror.backend.payload.request.UserProfileRequest;
import com.mymindmirror.backend.payload.response.UserFullProfileResponse;
import com.mymindmirror.backend.payload.response.UserPreferencesResponse;
import com.mymindmirror.backend.payload.response.UserRoadmapPreferencesDto;
import com.mymindmirror.backend.repository.*;
import com.mymindmirror.backend.util.EncryptionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JournalEntryRepository journalEntryRepository;

    @Mock
    private UserRoadmapPreferencesRepository userRoadmapPreferencesRepository;

    @Mock
    private UserPreferencesRepository userPreferencesRepository;

    @Mock
    private UserStatsRepository userStatsRepository;

    @Mock
    private MilestoneRepository milestoneRepository;

    @Mock
    private RoadmapRepository roadmapRepository;

    @Mock
    private CustomTaskRepository customTaskRepository;

    @Mock
    private ScheduledTaskRepository scheduledTaskRepository;

    @Mock
    private DailyJournalSummaryRepository dailyJournalSummaryRepository;

    @Mock
    private EmbeddingGenerationService embeddingGenerationService;

    @Mock
    private ChatMemoryService chatMemoryService;

    @InjectMocks
    private UserService userService;

    private User user;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPasswordHash("hashed-password");
    }

    // ========== registerNewUser Tests ==========

    @Test
    void registerNewUser_shouldSaveUser_whenValid() {
        // Given
        String username = "newuser";
        String email = "new@example.com";
        String rawPassword = "password123";
        String encodedPassword = "encoded-password";

        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        // When
        User savedUser = userService.registerNewUser(username, email, rawPassword);

        // Then
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getUsername()).isEqualTo(username);
        assertThat(savedUser.getEmail()).isEqualTo(email);
        assertThat(savedUser.getPasswordHash()).isEqualTo(encodedPassword);
        verify(userRepository).save(any(User.class));
        verify(userRepository).existsByUsername(username);
        verify(userRepository).findByEmail(email);
    }

    @Test
    void registerNewUser_shouldThrowException_whenUsernameExists() {
        // Given
        when(userRepository.existsByUsername("existing")).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> userService.registerNewUser("existing", "email@ex.com", "pass"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Username already exists.");
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerNewUser_shouldThrowException_whenEmailExists() {
        // Given
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.findByEmail("existing@email.com")).thenReturn(Optional.of(new User()));

        // When/Then
        assertThatThrownBy(() -> userService.registerNewUser("newuser", "existing@email.com", "pass"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email already exists.");
        verify(userRepository, never()).save(any());
    }

    // ========== updateUser Tests ==========

    @Test
    void updateUser_shouldUpdateUsername_whenValid() throws Exception {
        // Given
        UserProfileRequest request = new UserProfileRequest("newUsername", "test@example.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername("newUsername")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        User updated = userService.updateUser(userId, request);

        // Then
        assertThat(updated.getUsername()).isEqualTo("newUsername");
        verify(userRepository).save(user);
        verify(userRepository).existsByUsername("newUsername");
        // Email not changed – no check for email duplication
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void updateUser_shouldUpdateEmail_whenValid() throws Exception {
        // Given
        UserProfileRequest request = new UserProfileRequest("testuser", "newemail@example.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("newemail@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        User updated = userService.updateUser(userId, request);

        // Then
        assertThat(updated.getEmail()).isEqualTo("newemail@example.com");
        verify(userRepository).save(user);
        verify(userRepository).findByEmail("newemail@example.com");
    }

    @Test
    void updateUser_shouldThrowException_whenUsernameTaken() {
        // Given
        UserProfileRequest request = new UserProfileRequest("taken", "test@example.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername("taken")).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> userService.updateUser(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Username already taken.");
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_shouldThrowException_whenEmailTakenByAnotherUser() {
        // Given
        User otherUser = new User();
        otherUser.setId(UUID.randomUUID());
        UserProfileRequest request = new UserProfileRequest("testuser", "other@example.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(otherUser));

        // When/Then
        assertThatThrownBy(() -> userService.updateUser(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email already taken by another user.");
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_shouldNotUpdate_whenNoChanges() {
        // Given – request has same username and email
        UserProfileRequest request = new UserProfileRequest("testuser", "test@example.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        User updated = userService.updateUser(userId, request);

        // Then
        assertThat(updated).isSameAs(user); // returns the same instance, no save
        verify(userRepository, never()).save(any());
        verify(userRepository, never()).existsByUsername(any());
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void updateUser_shouldThrowException_whenUserNotFound() {
        // Given
        UserProfileRequest request = new UserProfileRequest("any", "any@ex.com");
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> userService.updateUser(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User not found.");
    }

    // ========== deleteUser Tests ==========

    @Test
    void deleteUser_shouldDeleteUserAndAllDependentData() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(customTaskRepository.findByUser(user)).thenReturn(List.of());
        when(roadmapRepository.findByUserOrderByCreatedAtDesc(user)).thenReturn(List.of());
        when(milestoneRepository.findByUserOrderByCreationDateDesc(user)).thenReturn(List.of());

        // Add a mock journal entry to verify the embedding deletion
        JournalEntry entry = new JournalEntry();
        entry.setId(UUID.randomUUID());
        when(journalEntryRepository.findByUser(user)).thenReturn(List.of(entry));
        when(userStatsRepository.findByUser(user)).thenReturn(Optional.empty());
        when(userPreferencesRepository.findByUser(user)).thenReturn(Optional.empty());
        when(userRoadmapPreferencesRepository.findByUser(user)).thenReturn(Optional.empty());

        // When
        userService.deleteUser(userId);

        // Then
        verify(scheduledTaskRepository).deleteByUser(user);
        verify(customTaskRepository).deleteAll(anyList());
        verify(roadmapRepository).deleteAll(anyList());
        verify(milestoneRepository).deleteAll(anyList());

        // --- NEW VERIFICATIONS ---
        verify(embeddingGenerationService).deleteEmbedding(entry.getId());
        verify(chatMemoryService).clearAllUserSessions(userId);

        verify(journalEntryRepository).deleteAll(anyList());
        verify(dailyJournalSummaryRepository).deleteByUserId(userId);
        verify(userStatsRepository).findByUser(user);
        verify(userPreferencesRepository).findByUser(user);
        verify(userRoadmapPreferencesRepository).findByUser(user);
        verify(userRepository).delete(user);
    }

    @Test
    void deleteUser_shouldThrowException_whenUserNotFound() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> userService.deleteUser(userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User not found.");
        verify(userRepository, never()).delete(any());
    }

    // ========== changeUserPassword Tests ==========

    @Test
    void changeUserPassword_shouldUpdatePasswordAndReEncryptEntries() throws Exception {
        // Given
        String currentPassword = "oldPass";
        String newPassword = "newPass";
        String oldHash = "oldHash";
        String newHash = "newHash";

        user.setPasswordHash(oldHash);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(currentPassword, oldHash)).thenReturn(true);
        when(passwordEncoder.matches(newPassword, oldHash)).thenReturn(false);
        when(passwordEncoder.encode(newPassword)).thenReturn(newHash);

        // Mock journal entries
        JournalEntry entry1 = new JournalEntry();
        entry1.setRawText("encrypted1");
        JournalEntry entry2 = new JournalEntry();
        entry2.setRawText("encrypted2");
        List<JournalEntry> entries = List.of(entry1, entry2);
        when(journalEntryRepository.findByUser(user)).thenReturn(entries);

        // Mock static EncryptionUtil
        try (MockedStatic<EncryptionUtil> mockedEncryption = mockStatic(EncryptionUtil.class)) {
            mockedEncryption.when(() -> EncryptionUtil.decrypt("encrypted1", oldHash)).thenReturn("plain1");
            mockedEncryption.when(() -> EncryptionUtil.decrypt("encrypted2", oldHash)).thenReturn("plain2");
            mockedEncryption.when(() -> EncryptionUtil.encrypt("plain1", newHash)).thenReturn("newEnc1");
            mockedEncryption.when(() -> EncryptionUtil.encrypt("plain2", newHash)).thenReturn("newEnc2");

            // When
            userService.changeUserPassword(userId, currentPassword, newPassword);

            // Then
            assertThat(user.getPasswordHash()).isEqualTo(newHash);
            verify(journalEntryRepository).saveAll(entries);
            assertThat(entry1.getRawText()).isEqualTo("newEnc1");
            assertThat(entry2.getRawText()).isEqualTo("newEnc2");
            verify(userRepository).save(user);
        }
    }

    @Test
    void changeUserPassword_shouldThrowException_whenCurrentPasswordIncorrect() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", user.getPasswordHash())).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> userService.changeUserPassword(userId, "wrong", "new"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Incorrect current password.");
        verify(userRepository, never()).save(any());
    }

    @Test
    void changeUserPassword_shouldThrowException_whenNewPasswordSameAsOld() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPass", user.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.matches("oldPass", user.getPasswordHash())).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> userService.changeUserPassword(userId, "oldPass", "oldPass"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("New password cannot be the same as the old password.");
        verify(userRepository, never()).save(any());
    }

    @Test
    void changeUserPassword_shouldThrowException_whenUserNotFound() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> userService.changeUserPassword(userId, "any", "any"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User not found.");
    }

    // ========== getRoadmapPreferencesDto Tests ==========

    @Test
    void getRoadmapPreferencesDto_shouldReturnDtoFromPreferences() {
        // Given
        UserRoadmapPreferences prefs = new UserRoadmapPreferences();
        prefs.setDifficulty("INTERMEDIATE");
        prefs.setLanguagePreference("fr");
        prefs.setLearningStyle("VISUAL");
        prefs.setHoursPerWeek(15);
        prefs.setAvoidWeekends(true);

        when(userRoadmapPreferencesRepository.findByUser(user)).thenReturn(Optional.of(prefs));

        // When
        UserRoadmapPreferencesDto result = userService.getRoadmapPreferencesDto(user);

        // Then
        assertThat(result.difficulty()).isEqualTo("INTERMEDIATE");
        assertThat(result.languagePreference()).isEqualTo("fr");
        assertThat(result.learningStyle()).isEqualTo("VISUAL");
        assertThat(result.hoursPerWeek()).isEqualTo(15);
        assertThat(result.avoidWeekends()).isTrue();
    }

    @Test
    void getRoadmapPreferencesDto_shouldCreateDefaultWhenNotFound() {
        // Given – repository returns empty
        when(userRoadmapPreferencesRepository.findByUser(user)).thenReturn(Optional.empty());
        // We need to mock the default creation – this will call createDefaultRoadmapPreferences, which saves a new entity
        UserRoadmapPreferences defaultPrefs = new UserRoadmapPreferences();
        defaultPrefs.setDifficulty("BEGINNER");
        defaultPrefs.setLanguagePreference("en");
        defaultPrefs.setLearningStyle("READING");
        defaultPrefs.setHoursPerWeek(10);
        defaultPrefs.setAvoidWeekends(false);
        when(userRoadmapPreferencesRepository.save(any(UserRoadmapPreferences.class))).thenReturn(defaultPrefs);

        // When
        UserRoadmapPreferencesDto result = userService.getRoadmapPreferencesDto(user);

        // Then
        assertThat(result.difficulty()).isEqualTo("BEGINNER");
        assertThat(result.languagePreference()).isEqualTo("en");
        assertThat(result.learningStyle()).isEqualTo("READING");
        assertThat(result.hoursPerWeek()).isEqualTo(10);
        assertThat(result.avoidWeekends()).isFalse();
        verify(userRoadmapPreferencesRepository).save(any(UserRoadmapPreferences.class));
    }

// ========== updateRoadmapPreferences (User, DTO) Tests ==========

    @Test
    void updateRoadmapPreferences_withDto_shouldUpdateAndReturnDto() {
        // Given
        UpdateRoadmapPreferencesRequest request = new UpdateRoadmapPreferencesRequest(
                "ADVANCED", "es", "HANDS_ON", 20, true
        );
        UserRoadmapPreferences existingPrefs = new UserRoadmapPreferences();
        existingPrefs.setDifficulty("BEGINNER");
        existingPrefs.setLanguagePreference("en");
        existingPrefs.setLearningStyle("READING");
        existingPrefs.setHoursPerWeek(10);
        existingPrefs.setAvoidWeekends(false);

        when(userRoadmapPreferencesRepository.findByUser(user)).thenReturn(Optional.of(existingPrefs));
        when(userRoadmapPreferencesRepository.save(any(UserRoadmapPreferences.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        UserRoadmapPreferencesDto result = userService.updateRoadmapPreferences(user, request);

        // Then
        assertThat(result.difficulty()).isEqualTo("ADVANCED");
        assertThat(result.languagePreference()).isEqualTo("es");
        assertThat(result.learningStyle()).isEqualTo("HANDS_ON");
        assertThat(result.hoursPerWeek()).isEqualTo(20);
        assertThat(result.avoidWeekends()).isTrue();
        verify(userRoadmapPreferencesRepository).save(existingPrefs);
        // Verify that the cache evictions are called? We can't test AOP here; it's fine.
    }

// ========== getFullUserProfile Tests ==========

    @Test
    void getFullUserProfile_shouldBuildResponse_withOwnKey() {
        // Given
        String decryptedApiKey = "abcdefghijklmnopqrstuvwxyz";
        UserRoadmapPreferencesDto roadmapPrefs = new UserRoadmapPreferencesDto("BEGINNER", "en", "READING", 10, false);
        UserPreferences userPrefs = new UserPreferences();
        userPrefs.setAvailableHoursJson("{}");
        userPrefs.setTimezone("UTC");
        userPrefs.setEnergyPeak("MORNING");
        userPrefs.setWakeTime(LocalTime.of(7, 0));
        userPrefs.setSleepTime(LocalTime.of(23, 0));
        userPrefs.setLunchTime(LocalTime.of(13, 0));
        userPrefs.setDailyHabitsJson("[]");

        // Stub the dependencies
        // We need to mock getRoadmapPreferencesDto, which we can do with a spy, or we can mock the repository.
        // Since we want to test the method in isolation, we can let getRoadmapPreferencesDto be called and we mock its dependencies.
        // But to avoid complexity, we can use the actual method with mocks.
        UserRoadmapPreferences prefsEntity = new UserRoadmapPreferences();
        prefsEntity.setDifficulty("BEGINNER");
        prefsEntity.setLanguagePreference("en");
        prefsEntity.setLearningStyle("READING");
        prefsEntity.setHoursPerWeek(10);
        prefsEntity.setAvoidWeekends(false);
        when(userRoadmapPreferencesRepository.findByUser(user)).thenReturn(Optional.of(prefsEntity));

        when(userPreferencesRepository.findByUser(user)).thenReturn(Optional.of(userPrefs));

        // When
        UserFullProfileResponse response = userService.getFullUserProfile(user, decryptedApiKey);

        // Then
        assertThat(response.id()).isEqualTo(userId);
        assertThat(response.username()).isEqualTo("testuser");
        assertThat(response.email()).isEqualTo("test@example.com");
        assertThat(response.usingOwnKey()).isTrue();
        assertThat(response.maskedKey()).isEqualTo("••••••••wxyz");
        assertThat(response.roadmapPreferences()).isNotNull();
        assertThat(response.availableHoursJson()).isEqualTo("{}");
        assertThat(response.timezone()).isEqualTo("UTC");
        assertThat(response.energyPeak()).isEqualTo("MORNING");
        assertThat(response.wakeTime()).isEqualTo("07:00");
        assertThat(response.sleepTime()).isEqualTo("23:00");
        assertThat(response.lunchTime()).isEqualTo("13:00");
        assertThat(response.dailyHabitsJson()).isEqualTo("[]");
    }

    @Test
    void getFullUserProfile_shouldBuildResponse_withoutOwnKey() {
        // Given
        String decryptedApiKey = null; // no custom key
        UserRoadmapPreferences prefsEntity = new UserRoadmapPreferences();
        prefsEntity.setDifficulty("BEGINNER");
        prefsEntity.setLanguagePreference("en");
        prefsEntity.setLearningStyle("READING");
        prefsEntity.setHoursPerWeek(10);
        prefsEntity.setAvoidWeekends(false);
        when(userRoadmapPreferencesRepository.findByUser(user)).thenReturn(Optional.of(prefsEntity));

        UserPreferences userPrefs = new UserPreferences();
        userPrefs.setAvailableHoursJson("{}");
        userPrefs.setTimezone("UTC");
        userPrefs.setEnergyPeak("MORNING");
        userPrefs.setWakeTime(LocalTime.of(7, 0));
        userPrefs.setSleepTime(LocalTime.of(23, 0));
        userPrefs.setLunchTime(LocalTime.of(13, 0));
        userPrefs.setDailyHabitsJson("[]");
        when(userPreferencesRepository.findByUser(user)).thenReturn(Optional.of(userPrefs));

        // When
        UserFullProfileResponse response = userService.getFullUserProfile(user, decryptedApiKey);

        // Then
        assertThat(response.usingOwnKey()).isFalse();
        assertThat(response.maskedKey()).isNull();
    }

// ========== getUserPreferences Tests ==========

    @Test
    void getUserPreferences_shouldReturnExistingPreferences() {
        // Given
        UserPreferences existingPrefs = new UserPreferences();
        existingPrefs.setTimezone("Asia/Kolkata");
        when(userPreferencesRepository.findByUser(user)).thenReturn(Optional.of(existingPrefs));

        // When
        UserPreferences result = userService.getUserPreferences(user);

        // Then
        assertThat(result).isSameAs(existingPrefs);
        verify(userPreferencesRepository).findByUser(user);
        verify(userPreferencesRepository, never()).save(any());
    }

    @Test
    void getUserPreferences_shouldCreateDefaultWhenNotFound() {
        // Given
        when(userPreferencesRepository.findByUser(user)).thenReturn(Optional.empty());
        // Mock the save so it returns a new object with defaults
        UserPreferences defaultPrefs = new UserPreferences();
        defaultPrefs.setTimezone("Asia/Kolkata");
        when(userPreferencesRepository.save(any(UserPreferences.class))).thenReturn(defaultPrefs);

        // When
        UserPreferences result = userService.getUserPreferences(user);

        // Then
        assertThat(result.getTimezone()).isEqualTo("Asia/Kolkata");
        verify(userPreferencesRepository).save(any(UserPreferences.class));
    }

// ========== updateUserPreferences (User, DTO) Tests ==========

    @Test
    void updateUserPreferences_withDto_shouldUpdateFieldsAndReturnResponse() {
        // Given
        UpdateUserPreferencesRequest request = new UpdateUserPreferencesRequest(
                "{\"monday\":[]}", "UTC", "EVENING", LocalTime.of(8, 0),
                LocalTime.of(22, 0), LocalTime.of(12, 0), "[]"
        );
        UserPreferences existingPrefs = new UserPreferences();
        existingPrefs.setTimezone("Asia/Kolkata");
        existingPrefs.setEnergyPeak("MORNING");
        existingPrefs.setWakeTime(LocalTime.of(7, 0));

        when(userPreferencesRepository.findByUser(user)).thenReturn(Optional.of(existingPrefs));
        when(userPreferencesRepository.save(any(UserPreferences.class))).thenAnswer(i -> i.getArgument(0));

        // When
        UserPreferencesResponse result = userService.updateUserPreferences(user, request);

        // Then
        assertThat(result.availableHoursJson()).isEqualTo("{\"monday\":[]}");
        assertThat(result.timezone()).isEqualTo("UTC");
        assertThat(result.energyPeak()).isEqualTo("EVENING");
        assertThat(result.wakeTime()).isEqualTo("08:00");
        assertThat(result.sleepTime()).isEqualTo("22:00");
        assertThat(result.lunchTime()).isEqualTo("12:00");
        assertThat(result.dailyHabitsJson()).isEqualTo("[]");
        verify(userPreferencesRepository).save(existingPrefs);
    }

    // ========== save Tests ==========

    @Test
    void save_shouldDelegateToRepositoryAndReturnUser() {
        // Given
        when(userRepository.save(user)).thenReturn(user);

        // When
        User result = userService.save(user);

        // Then
        assertThat(result).isSameAs(user);
        verify(userRepository).save(user);
    }

// ========== findByUsername Tests ==========

    @Test
    void findByUsername_shouldReturnUser_whenFound() {
        // Given
        String username = "testuser";
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        // When
        Optional<User> result = userService.findByUsername(username);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isSameAs(user);
    }

    @Test
    void findByUsername_shouldReturnEmpty_whenNotFound() {
        // Given
        String username = "unknown";
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // When
        Optional<User> result = userService.findByUsername(username);

        // Then
        assertThat(result).isEmpty();
    }

    // ========== changeUserPassword – Decryption Failure ==========

    @Test
    void changeUserPassword_shouldSkipDecryptionFailureAndContinue() {
        // Given
        String currentPassword = "oldPass";
        String newPassword = "newPass";
        String oldHash = "oldHash";
        String newHash = "newHash";

        user.setPasswordHash(oldHash);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(currentPassword, oldHash)).thenReturn(true);
        when(passwordEncoder.matches(newPassword, oldHash)).thenReturn(false);
        when(passwordEncoder.encode(newPassword)).thenReturn(newHash);

        JournalEntry entry = new JournalEntry();
        entry.setId(UUID.randomUUID());
        entry.setRawText("encrypted");
        List<JournalEntry> entries = List.of(entry);
        when(journalEntryRepository.findByUser(user)).thenReturn(entries);

        try (MockedStatic<EncryptionUtil> mockedEncryption = mockStatic(EncryptionUtil.class)) {
            mockedEncryption.when(() -> EncryptionUtil.decrypt("encrypted", oldHash)).thenReturn(null);
            mockedEncryption.when(() -> EncryptionUtil.encrypt(anyString(), eq(newHash))).thenReturn("newEncrypted");

            // When
            userService.changeUserPassword(userId, currentPassword, newPassword);

            // Then – decryption failed, so entry should remain unchanged
            assertThat(entry.getRawText()).isEqualTo("encrypted");
            verify(journalEntryRepository).saveAll(entries);
            verify(userRepository).save(user);
        }
    }

// ========== changeUserPassword – Encryption Failure ==========

    @Test
    void changeUserPassword_shouldThrowException_whenEncryptionFails() {
        // Given
        String currentPassword = "oldPass";
        String newPassword = "newPass";
        String oldHash = "oldHash";
        String newHash = "newHash";

        user.setPasswordHash(oldHash);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(currentPassword, oldHash)).thenReturn(true);
        when(passwordEncoder.matches(newPassword, oldHash)).thenReturn(false);
        when(passwordEncoder.encode(newPassword)).thenReturn(newHash);

        JournalEntry entry = new JournalEntry();
        entry.setId(UUID.randomUUID());
        entry.setRawText("encrypted");
        List<JournalEntry> entries = List.of(entry);
        when(journalEntryRepository.findByUser(user)).thenReturn(entries);

        try (MockedStatic<EncryptionUtil> mockedEncryption = mockStatic(EncryptionUtil.class)) {
            mockedEncryption.when(() -> EncryptionUtil.decrypt("encrypted", oldHash)).thenReturn("plain");
            mockedEncryption.when(() -> EncryptionUtil.encrypt("plain", newHash)).thenReturn(null);

            // When/Then
            assertThatThrownBy(() -> userService.changeUserPassword(userId, currentPassword, newPassword))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to re-encrypt journal entries");
            verify(userRepository, never()).save(user);
        }
    }

// ========== deleteUser – with Stats, Preferences, RoadmapPreferences present ==========

    @Test
    void deleteUser_shouldDeleteAllDependencies_whenPresent() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(customTaskRepository.findByUser(user)).thenReturn(List.of());
        when(roadmapRepository.findByUserOrderByCreatedAtDesc(user)).thenReturn(List.of());
        when(milestoneRepository.findByUserOrderByCreationDateDesc(user)).thenReturn(List.of());
        when(journalEntryRepository.findByUser(user)).thenReturn(List.of());

        UserStats stats = new UserStats();
        UserPreferences prefs = new UserPreferences();
        UserRoadmapPreferences roadmapPrefs = new UserRoadmapPreferences();
        when(userStatsRepository.findByUser(user)).thenReturn(Optional.of(stats));
        when(userPreferencesRepository.findByUser(user)).thenReturn(Optional.of(prefs));
        when(userRoadmapPreferencesRepository.findByUser(user)).thenReturn(Optional.of(roadmapPrefs));

        // When
        userService.deleteUser(userId);

        // Then
        verify(userStatsRepository).delete(stats);
        verify(userPreferencesRepository).delete(prefs);
        verify(userRoadmapPreferencesRepository).delete(roadmapPrefs);
        verify(userRepository).delete(user);
    }

// ========== updateRoadmapPreferences – Null checks (cover skipped updates) ==========

    @Test
    void updateRoadmapPreferences_shouldSkipNullFields() {
        // Given
        UserRoadmapPreferences prefs = new UserRoadmapPreferences();
        prefs.setDifficulty("BEGINNER");
        prefs.setLanguagePreference("en");
        prefs.setLearningStyle("READING");
        prefs.setHoursPerWeek(10);
        prefs.setAvoidWeekends(false);
        when(userRoadmapPreferencesRepository.findByUser(user)).thenReturn(Optional.of(prefs));
        when(userRoadmapPreferencesRepository.save(any())).thenReturn(prefs);

        // When – pass null for some fields
        UserRoadmapPreferences result = userService.updateRoadmapPreferences(
                user, null, null, null, null, null
        );

        // Then – all fields should remain unchanged
        assertThat(result.getDifficulty()).isEqualTo("BEGINNER");
        assertThat(result.getLanguagePreference()).isEqualTo("en");
        assertThat(result.getLearningStyle()).isEqualTo("READING");
        assertThat(result.getHoursPerWeek()).isEqualTo(10);
        assertThat(result.isAvoidWeekends()).isFalse();
    }

// ========== getFullUserProfile – Null Times ==========

    @Test
    void getFullUserProfile_shouldHandleNullTimes() {
        // Given
        String decryptedApiKey = "apiKey";
        UserRoadmapPreferences roadmapPrefs = new UserRoadmapPreferences();
        roadmapPrefs.setDifficulty("BEGINNER");
        roadmapPrefs.setLanguagePreference("en");
        roadmapPrefs.setLearningStyle("READING");
        roadmapPrefs.setHoursPerWeek(10);
        roadmapPrefs.setAvoidWeekends(false);
        when(userRoadmapPreferencesRepository.findByUser(user)).thenReturn(Optional.of(roadmapPrefs));

        UserPreferences userPrefs = new UserPreferences();
        userPrefs.setAvailableHoursJson("{}");
        userPrefs.setTimezone("UTC");
        userPrefs.setEnergyPeak("MORNING");
        userPrefs.setWakeTime(null);
        userPrefs.setSleepTime(null);
        userPrefs.setLunchTime(null);
        userPrefs.setDailyHabitsJson("[]");
        when(userPreferencesRepository.findByUser(user)).thenReturn(Optional.of(userPrefs));

        // When
        UserFullProfileResponse response = userService.getFullUserProfile(user, decryptedApiKey);

        // Then
        assertThat(response.wakeTime()).isNull();
        assertThat(response.sleepTime()).isNull();
        assertThat(response.lunchTime()).isNull();
    }

// ========== updateUserPreferences – Null Times in Response ==========

    @Test
    void updateUserPreferences_shouldHandleNullTimesInResponse() {
        // Given
        UpdateUserPreferencesRequest request = new UpdateUserPreferencesRequest(
                "{}", "UTC", "MORNING", null, null, null, "[]"
        );
        UserPreferences existingPrefs = new UserPreferences();
        existingPrefs.setTimezone("UTC");
        existingPrefs.setEnergyPeak("MORNING");
        existingPrefs.setWakeTime(null);
        existingPrefs.setSleepTime(null);
        existingPrefs.setLunchTime(null);
        existingPrefs.setAvailableHoursJson("{}");
        existingPrefs.setDailyHabitsJson("[]");
        when(userPreferencesRepository.findByUser(user)).thenReturn(Optional.of(existingPrefs));
        when(userPreferencesRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // When
        UserPreferencesResponse response = userService.updateUserPreferences(user, request);

        // Then
        assertThat(response.wakeTime()).isNull();
        assertThat(response.sleepTime()).isNull();
        assertThat(response.lunchTime()).isNull();
    }

    @Test
    void findById_shouldReturnUser_whenFound() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        Optional<User> result = userService.findById(userId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isSameAs(user);
    }

    @Test
    void findById_shouldReturnEmpty_whenNotFound() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When
        Optional<User> result = userService.findById(userId);

        // Then
        assertThat(result).isEmpty();
    }


    // ========== Lambda Coverage Tests (Default Preferences Creation) ==========

    @Test
    void getFullUserProfile_shouldCreateDefaultPreferences_whenNotFound() {
        // Given
        String decryptedApiKey = "apiKey";

        // Mock roadmap prefs
        UserRoadmapPreferences roadmapPrefs = new UserRoadmapPreferences();
        roadmapPrefs.setDifficulty("BEGINNER");
        when(userRoadmapPreferencesRepository.findByUser(user)).thenReturn(Optional.of(roadmapPrefs));

        //  Force empty to trigger lambda$getFullUserProfile$0
        when(userPreferencesRepository.findByUser(user)).thenReturn(Optional.empty());

        UserPreferences defaultPrefs = new UserPreferences();
        defaultPrefs.setTimezone("Asia/Kolkata");
        when(userPreferencesRepository.save(any(UserPreferences.class))).thenReturn(defaultPrefs);

        // When
        UserFullProfileResponse response = userService.getFullUserProfile(user, decryptedApiKey);

        // Then
        assertThat(response.timezone()).isEqualTo("Asia/Kolkata");
        verify(userPreferencesRepository).save(any(UserPreferences.class)); // Verifies createDefaultPreferences ran
    }

    @Test
    void updateUserPreferences_shouldCreateDefaultPreferences_whenNotFound() {
        // Given
        UpdateUserPreferencesRequest request = new UpdateUserPreferencesRequest(
                null, "Europe/London", null, null, null, null, null
        );

        //  Force empty to trigger lambda$updateUserPreferences$0
        when(userPreferencesRepository.findByUser(user)).thenReturn(Optional.empty());

        // Mock the save method (it will be called twice: once for defaults, once for the update)
        when(userPreferencesRepository.save(any(UserPreferences.class))).thenAnswer(i -> i.getArgument(0));

        // When
        UserPreferencesResponse response = userService.updateUserPreferences(user, request);

        // Then
        assertThat(response.timezone()).isEqualTo("Europe/London");
        verify(userPreferencesRepository, atLeastOnce()).save(any(UserPreferences.class));
    }

    @Test
    void updateUser_shouldSkipNullAndBlankFields() {
        UserProfileRequest request = new UserProfileRequest(null, null);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        User result = userService.updateUser(userId, request);

        assertThat(result).isSameAs(user);
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_shouldSkipEmailCheck_whenEmailUnchanged() {
        UserProfileRequest request = new UserProfileRequest("testuser", "test@example.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.updateUser(userId, request);

        verify(userRepository, never()).findByEmail(any());
    }


    @Test
    void updateRoadmapPreferences_shouldSkipAllNullFields() {
        UserRoadmapPreferences prefs = new UserRoadmapPreferences();
        prefs.setDifficulty("BEGINNER");
        prefs.setLanguagePreference("en");
        prefs.setLearningStyle("READING");
        prefs.setHoursPerWeek(10);

        when(userRoadmapPreferencesRepository.findByUser(user)).thenReturn(Optional.of(prefs));
        when(userRoadmapPreferencesRepository.save(any())).thenReturn(prefs);

        userService.updateRoadmapPreferences(user, null, null, null, null, null);

        assertThat(prefs.getDifficulty()).isEqualTo("BEGINNER");
        assertThat(prefs.getHoursPerWeek()).isEqualTo(10);
    }

    @Test
    void updateUserPreferences_shouldSkipNullTimezone() {
        UpdateUserPreferencesRequest request = new UpdateUserPreferencesRequest(
                null, null, null, null, null, null, null);
        UserPreferences prefs = new UserPreferences();
        prefs.setTimezone("Asia/Kolkata");

        when(userPreferencesRepository.findByUser(user)).thenReturn(Optional.of(prefs));
        when(userPreferencesRepository.save(any())).thenReturn(prefs);

        userService.updateUserPreferences(user, request);

        assertThat(prefs.getTimezone()).isEqualTo("Asia/Kolkata");
    }

    @Test
    @DisplayName("updateUser: whitespace-only username is ignored (no overwrite)")
    void updateUser_shouldSkipWhitespaceUsername() {
        UserProfileRequest request = new UserProfileRequest("   ", "test@example.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        User result = userService.updateUser(userId, request);

        // Username unchanged — whitespace was rejected
        assertThat(result.getUsername()).isEqualTo("testuser");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateUser: whitespace-only email is ignored (no overwrite)")
    void updateUser_shouldSkipWhitespaceEmail() {
        UserProfileRequest request = new UserProfileRequest("testuser", "   ");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        User result = userService.updateUser(userId, request);

        // Email unchanged — whitespace was rejected
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateUser: email lookup returning self is allowed (defensive)")
    void updateUser_shouldAllowSameUserEmailLookup() {
        // Simulates the defensive case where findByEmail returns the same user
        UserProfileRequest request = new UserProfileRequest("testuser", "newemail@example.com");
        User selfByEmail = new User();
        selfByEmail.setId(userId);   // same ID

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("newemail@example.com")).thenReturn(Optional.of(selfByEmail));
        when(userRepository.save(any(User.class))).thenReturn(user);

        User result = userService.updateUser(userId, request);

        // Same user → no conflict → email updated
        assertThat(result.getEmail()).isEqualTo("newemail@example.com");
    }

    @Test
    @DisplayName("updateRoadmapPreferences: blank strings and hours < 1 are ignored")
    void updateRoadmapPreferences_shouldSkipBlankStringsAndLowHours() {
        // Existing prefs with known values
        UserRoadmapPreferences prefs = new UserRoadmapPreferences();
        prefs.setDifficulty("BEGINNER");
        prefs.setLanguagePreference("en");
        prefs.setLearningStyle("READING");
        prefs.setHoursPerWeek(10);
        prefs.setAvoidWeekends(false);

        when(userRoadmapPreferencesRepository.findByUser(user)).thenReturn(Optional.of(prefs));
        when(userRoadmapPreferencesRepository.save(any())).thenReturn(prefs);

        // Pass whitespace strings and 0 hours — all should be rejected
        UserRoadmapPreferences result = userService.updateRoadmapPreferences(
                user,
                "   ",      // blank difficulty
                "   ",      // blank language
                "   ",      // blank learning style
                0,          // < 1
                null
        );

        // Nothing changed
        assertThat(result.getDifficulty()).isEqualTo("BEGINNER");
        assertThat(result.getLanguagePreference()).isEqualTo("en");
        assertThat(result.getLearningStyle()).isEqualTo("READING");
        assertThat(result.getHoursPerWeek()).isEqualTo(10);
    }

    @Test
    @DisplayName("getFullUserProfile: blank decrypted API key is treated as no key")
    void getFullUserProfile_shouldTreatBlankKeyAsNoKey() {
        // Roadmap prefs + preferences stubs
        UserRoadmapPreferences prefs = new UserRoadmapPreferences();
        prefs.setDifficulty("BEGINNER");
        prefs.setLanguagePreference("en");
        prefs.setLearningStyle("READING");
        prefs.setHoursPerWeek(10);
        prefs.setAvoidWeekends(false);
        when(userRoadmapPreferencesRepository.findByUser(user)).thenReturn(Optional.of(prefs));

        UserPreferences userPrefs = new UserPreferences();
        userPrefs.setAvailableHoursJson("{}");
        userPrefs.setTimezone("UTC");
        when(userPreferencesRepository.findByUser(user)).thenReturn(Optional.of(userPrefs));

        // Blank key — non-null but all whitespace
        String blankKey = "   ";

        UserFullProfileResponse response = userService.getFullUserProfile(user, blankKey);

        // Blank treated as "no key"
        assertThat(response.usingOwnKey()).isFalse();
        assertThat(response.maskedKey()).isNull();
    }

}