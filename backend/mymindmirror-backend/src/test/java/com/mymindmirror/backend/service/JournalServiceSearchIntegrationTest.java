package com.mymindmirror.backend.service;

import com.mymindmirror.backend.BackendApplication;
import com.mymindmirror.backend.config.TestVectorStoreConfig;
import com.mymindmirror.backend.model.JournalEntry;
import com.mymindmirror.backend.model.KeyPhrase;
import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.repository.JournalEntryRepository;
import com.mymindmirror.backend.repository.UserRepository;
import com.mymindmirror.backend.repository.UserStatsRepository;
import com.mymindmirror.backend.util.EncryptionUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {BackendApplication.class, TestVectorStoreConfig.class})
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
public class JournalServiceSearchIntegrationTest {

    @Autowired
    private JournalService journalService;

    @Autowired
    private JournalEntryRepository journalEntryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserStatsRepository userStatsRepository;

    private User testUser;
    private final String TEST_SECRET = "test-secret-key-1234567890123456";

    @BeforeEach
    public void setUp() {
        journalEntryRepository.deleteAll();
        userStatsRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User();
        testUser.setUsername("test_search_user");
        testUser.setEmail("test@search.com");
        testUser.setPasswordHash(TEST_SECRET);
        testUser = userRepository.save(testUser);

        JournalEntry entry1 = new JournalEntry();
        entry1.setUser(testUser);
        entry1.setEntryDate(LocalDate.now());
        entry1.setCreationTimestamp(LocalDateTime.now());
        entry1.setMoodScore(0.9);
        entry1.setRawText(EncryptionUtil.encrypt("Today I ate a massive WATERMELON and felt great.", TEST_SECRET));

        KeyPhrase kp1 = new KeyPhrase("ate watermelon", entry1);
        KeyPhrase kp2 = new KeyPhrase("felt great", entry1);
        KeyPhrase kp3 = new KeyPhrase("massive fruit", entry1);
        entry1.setKeyPhrases(List.of(kp1, kp2, kp3));

        journalEntryRepository.save(entry1);

        JournalEntry entry2 = new JournalEntry();
        entry2.setUser(testUser);
        entry2.setEntryDate(LocalDate.now().minusDays(2));
        entry2.setCreationTimestamp(LocalDateTime.now().minusDays(2));
        entry2.setMoodScore(-0.5);
        entry2.setRawText(EncryptionUtil.encrypt("I had a very stressful meeting about Java architecture.", TEST_SECRET));

        KeyPhrase kp4 = new KeyPhrase("stressful meeting", entry2);
        KeyPhrase kp5 = new KeyPhrase("java architecture", entry2);
        entry2.setKeyPhrases(List.of(kp4, kp5));

        journalEntryRepository.save(entry2);
    }

    @AfterEach
    public void tearDown() {
        journalEntryRepository.deleteAll();
        userStatsRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    public void testKeywordSearch_NoCartesianDuplicates_AndNoLazyExceptions() {
        List<JournalEntry> results = journalService.searchJournalEntriesByKeyword(testUser, "watermelon");

        assertFalse(results.isEmpty(), "Should find the entry with the keyword.");
        assertEquals(1, results.size(), "Cartesian Product detected! Hibernate duplicated the entry.");

        JournalEntry foundEntry = results.get(0);
        assertDoesNotThrow(() -> {
            int keyPhraseCount = foundEntry.getKeyPhrases().size();
            assertEquals(3, keyPhraseCount, "Should have safely loaded all 3 key phrases.");
        });

        assertTrue(foundEntry.getRawText().contains("WATERMELON"));
    }

    @Test
    public void testMoodSearch_NoCartesianDuplicates_AndNoLazyExceptions() {
        List<JournalEntry> results = journalService.searchJournalEntriesByMoodScore(testUser, 0.5, 1.0);

        assertEquals(1, results.size(), "Should only find 1 entry, no duplicates.");
        assertDoesNotThrow(() -> {
            assertEquals(3, results.get(0).getKeyPhrases().size());
            assertEquals("test_search_user", results.get(0).getUser().getUsername(), "Failed to load User eagerly.");
        });
    }

    @Test
    public void testDateSearch_NoCartesianDuplicates_AndNoLazyExceptions() {
        LocalDate start = LocalDate.now().minusDays(10);
        LocalDate end = LocalDate.now().plusDays(1);
        List<JournalEntry> results = journalService.getJournalEntriesForUser(testUser, start, end);

        assertEquals(2, results.size(), "Should find exactly 2 distinct entries. No duplicates.");

        assertDoesNotThrow(() -> {
            for (JournalEntry entry : results) {
                assertTrue(entry.getKeyPhrases().size() > 0);
                assertNotNull(entry.getUser().getUsername());
            }
        });
    }
}