package com.mymindmirror.backend.repository;

import com.mymindmirror.backend.model.DailyJournalSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

/**
 * Integration test for the MySQL-specific upsert method in
 * {@link DailyJournalSummaryRepository#insertOrUpdateSummary}.
 *
 * Runs against a real MySQL 8 container because H2 does not support
 * the ON DUPLICATE KEY UPDATE syntax.
 *
 * Uses @ServiceConnection so Spring Boot manages the container lifecycle
 * correctly – no @DirtiesContext workaround needed.
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class DailyJournalSummaryRepositoryIT {

    @Container
    @ServiceConnection  //  Auto-wires DataSource from the container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("mymindmirror")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void overrideHibernateDdl(DynamicPropertyRegistry registry) {
        // Only override the DDL behaviour – everything else comes from @ServiceConnection.
        // "create" avoids the drop phase that produces 13 noise errors on fresh MySQL.
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");

        // ↓ ADD this line
        registry.add("spring.jpa.properties.hibernate.dialect",
                () -> "org.hibernate.dialect.MySQLDialect");
    }

    @Autowired
    private DailyJournalSummaryRepository repository;

    @Autowired
    private TestEntityManager em;

    @Autowired
    private DataSource dataSource;

    private UUID userA;
    private UUID userB;

    @BeforeEach
    void setUp() {
        userA = UUID.randomUUID();
        userB = UUID.randomUUID();
    }

    @BeforeEach
    void assertNotConnectedToProduction() throws Exception {   // ← ADD
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String url = metaData.getURL();
            if (url != null && url.contains("aivencloud.com")) {
                throw new IllegalStateException(
                        "REFUSING TO RUN: test is connected to production Aiven database.\n" +
                                "URL: " + url + "\n" +
                                "Check @ActiveProfiles and @Profile(\"!test\") gates on config classes.");
            }
        }
    }

    // ============================================================
    // INSERT path
    // ============================================================

    @Test
    @DisplayName("insertOrUpdateSummary inserts a new row on first call")
    void insertOrUpdateSummary_insertsWhenAbsent() {
        LocalDate today = LocalDate.now();

        repository.insertOrUpdateSummary(userA, today, 0.5, 100L, 2);
        em.flush();
        em.clear();

        List<Object[]> rows = repository.findByUserAndDateRange(userA, today, today);

        assertThat(rows).hasSize(1);
        assertThat((Double) rows.get(0)[1]).isCloseTo(0.5, offset(0.001));
        assertThat((Long) rows.get(0)[2]).isEqualTo(100L);
    }

    // ============================================================
    // UPDATE path (ON DUPLICATE KEY UPDATE)
    // ============================================================

    @Test
    @DisplayName("insertOrUpdateSummary updates ALL fields on second call (avg_mood, total_words, entry_count)")
    void insertOrUpdateSummary_updatesWhenPresent() {
        LocalDate today = LocalDate.now();

        repository.insertOrUpdateSummary(userA, today, 0.5, 100L, 2);
        em.flush();
        em.clear();

        repository.insertOrUpdateSummary(userA, today, 0.9, 250L, 5);
        em.flush();
        em.clear();

        // Verify avg_mood + total_words via the range query
        List<Object[]> rows = repository.findByUserAndDateRange(userA, today, today);
        assertThat(rows).hasSize(1);
        assertThat((Double) rows.get(0)[1]).isCloseTo(0.9, offset(0.001));
        assertThat((Long) rows.get(0)[2]).isEqualTo(250L);

        //  Verify entry_count separately (not returned by findByUserAndDateRange)
        DailyJournalSummary row = em.getEntityManager()
                .createQuery("SELECT d FROM DailyJournalSummary d WHERE d.userId = :uid AND d.date = :date",
                        DailyJournalSummary.class)
                .setParameter("uid", userA)
                .setParameter("date", today)
                .getSingleResult();
        assertThat(row.getEntryCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("insertOrUpdateSummary does not create duplicates for same user+date")
    void insertOrUpdateSummary_noDuplicates() {
        LocalDate today = LocalDate.now();

        for (int i = 1; i <= 5; i++) {
            repository.insertOrUpdateSummary(userA, today, 0.5, 100L, i);
            em.flush();
        }
        em.clear();

        assertThat(repository.count()).isEqualTo(1);
    }

    // ============================================================
    // Isolation
    // ============================================================

    @Test
    @DisplayName("insertOrUpdateSummary keeps user rows isolated")
    void insertOrUpdateSummary_isolatesByUser() {
        LocalDate today = LocalDate.now();

        repository.insertOrUpdateSummary(userA, today, 0.5, 100L, 2);
        repository.insertOrUpdateSummary(userB, today, 0.9, 999L, 5);
        em.flush();
        em.clear();

        List<Object[]> a = repository.findByUserAndDateRange(userA, today, today);
        List<Object[]> b = repository.findByUserAndDateRange(userB, today, today);

        assertThat(a).hasSize(1);
        assertThat(b).hasSize(1);
        assertThat((Double) a.get(0)[1]).isCloseTo(0.5, offset(0.001));
        assertThat((Double) b.get(0)[1]).isCloseTo(0.9, offset(0.001));
    }

    @Test
    @DisplayName("insertOrUpdateSummary keeps date rows isolated per user")
    void insertOrUpdateSummary_isolatesByDate() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        repository.insertOrUpdateSummary(userA, today, 0.5, 100L, 2);
        repository.insertOrUpdateSummary(userA, yesterday, 0.9, 999L, 5);
        em.flush();
        em.clear();

        List<Object[]> result = repository.findByUserAndDateRange(userA, yesterday, today);
        assertThat(result).hasSize(2);
    }

    // ============================================================
    // Null avgMood
    // ============================================================

    @Test
    @DisplayName("insertOrUpdateSummary accepts null avgMood (all entries unscored)")
    void insertOrUpdateSummary_handlesNullAvgMood() {
        LocalDate today = LocalDate.now();

        repository.insertOrUpdateSummary(userA, today, null, 100L, 2);
        em.flush();
        em.clear();

        List<Object[]> rows = repository.findByUserAndDateRange(userA, today, today);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0)[1]).isNull();
        assertThat((Long) rows.get(0)[2]).isEqualTo(100L);
    }
}