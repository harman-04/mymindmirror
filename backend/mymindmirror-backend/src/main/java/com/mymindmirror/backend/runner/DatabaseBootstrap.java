package com.mymindmirror.backend.runner;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Creates MySQL objects that JPA does not manage.
 *
 * <p>Why this exists: Hibernate's {@code ddl-auto=update} creates tables from entities,
 * but there is no way for Hibernate to create stored procedures or triggers. Without
 * this bootstrap, a fresh database (Testcontainers, a new Docker volume, a new dev
 * machine) would be missing the procedures and triggers, and analytics features that
 * rely on them would silently return nothing.
 *
 * <p>Why not Flyway: Flyway runs BEFORE Hibernate in Spring Boot's standard integration.
 * On a fresh database, the migration that creates triggers on {@code journal_entries}
 * fails because that table does not exist yet. Flyway and Hibernate can only coexist if
 * Flyway owns every table and Hibernate uses {@code ddl-auto=validate}. That is a
 * significant migration project and is not needed at the current scale.
 *
 * <p>DataSource resolution:
 * <ul>
 *   <li>Dev / Prod: {@code mysqlDataSource} is created by {@code MySqlDatabaseConfig}
 *       (which is {@code @Profile("!test")}). Used directly.</li>
 *   <li>Testcontainers IT: the test profile falls back to Spring Boot's auto-configured
 *       {@code dataSource}, wired to the Testcontainers MySQL via {@code @DynamicPropertySource}.
 *       Used as a fallback.</li>
 *   <li>H2 slice tests: the auto-configured {@code dataSource} points to H2. The
 *       runtime product check skips the bootstrap entirely.</li>
 * </ul>
 *
 * <p>Timing: {@code ApplicationRunner} beans execute after the full application context
 * is initialized, which means after Hibernate has created or updated all tables from
 * the entity model. The procedures and triggers can therefore safely reference tables
 * that Hibernate owns.
 *
 * <p>Idempotency: each procedure and trigger is created only if it does not already
 * exist. Safe to run on every startup.
 *
 * <p>Failure behavior: if any object fails to create, the exception propagates and the
 * application refuses to start. A missing trigger means silent data corruption in the
 * summary table, so a loud failure is the correct outcome.
 */
@Component
@Order(100)
@Slf4j
public class DatabaseBootstrap implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private final boolean skip;

    public DatabaseBootstrap(
            @Qualifier("mysqlDataSource") ObjectProvider<DataSource> mysqlDataSourceProvider,
            ObjectProvider<DataSource> defaultDataSourceProvider) {

        DataSource ds = mysqlDataSourceProvider.getIfAvailable();
        if (ds == null) {
            ds = defaultDataSourceProvider.getIfAvailable();
        }

        if (ds == null) {
            log.info("DatabaseBootstrap: no DataSource bean available in this context. " +
                    "Bootstrap will be skipped.");
            this.dataSource = null;
            this.jdbcTemplate = null;
            this.skip = true;
        } else {
            this.dataSource = ds;
            this.jdbcTemplate = new JdbcTemplate(ds);
            this.skip = false;
        }
    }

    // ------------------------------------------------------------------
    // Stored procedures
    // ------------------------------------------------------------------

    private static final String PROC_RECALC_DAILY_SUMMARY = """
            CREATE PROCEDURE recalc_daily_summary(
                IN p_user_id BINARY(16),
                IN p_date DATE
            )
            BEGIN
                DECLARE v_avg_mood DOUBLE;
                DECLARE v_total_words BIGINT;
                DECLARE v_entry_count INT;

                SELECT AVG(mood_score), SUM(word_count), COUNT(*)
                INTO v_avg_mood, v_total_words, v_entry_count
                FROM journal_entries
                WHERE user_id = p_user_id AND entry_date = p_date;

                IF v_avg_mood IS NULL OR v_entry_count = 0 THEN
                    DELETE FROM daily_journal_summary
                    WHERE user_id = p_user_id AND date = p_date;
                ELSE
                    INSERT INTO daily_journal_summary (user_id, date, avg_mood, total_words, entry_count)
                    VALUES (p_user_id, p_date, v_avg_mood, IFNULL(v_total_words, 0), v_entry_count)
                    ON DUPLICATE KEY UPDATE
                        avg_mood = v_avg_mood,
                        total_words = IFNULL(v_total_words, 0),
                        entry_count = v_entry_count;
                END IF;
            END
            """;

    private static final String PROC_INSERT_JOURNAL_ENTRY_SUMMARY = """
            CREATE PROCEDURE insert_journal_entry_summary(
                IN p_user_id BINARY(16),
                IN p_date DATE,
                IN p_mood_score DOUBLE,
                IN p_word_count INT
            )
            BEGIN
                INSERT INTO daily_journal_summary (user_id, date, avg_mood, total_words, entry_count)
                VALUES (p_user_id, p_date, p_mood_score, p_word_count, 1)
                ON DUPLICATE KEY UPDATE
                    avg_mood = (avg_mood * entry_count + p_mood_score) / (entry_count + 1),
                    total_words = total_words + p_word_count,
                    entry_count = entry_count + 1;
            END
            """;

    private static final String PROC_UPDATE_JOURNAL_ENTRY_SUMMARY = """
            CREATE PROCEDURE update_journal_entry_summary(
                IN old_user_id BINARY(16),
                IN old_date DATE,
                IN new_user_id BINARY(16),
                IN new_date DATE
            )
            BEGIN
                IF old_date != new_date THEN
                    CALL recalc_daily_summary(old_user_id, old_date);
                    CALL recalc_daily_summary(new_user_id, new_date);
                ELSE
                    CALL recalc_daily_summary(new_user_id, new_date);
                END IF;
            END
            """;

    // ------------------------------------------------------------------
    // Triggers
    // ------------------------------------------------------------------

    private static final String TRIGGER_AFTER_INSERT = """
            CREATE TRIGGER after_journal_entry_insert
            AFTER INSERT ON journal_entries
            FOR EACH ROW
            CALL insert_journal_entry_summary(NEW.user_id, NEW.entry_date, NEW.mood_score, NEW.word_count)
            """;

    private static final String TRIGGER_AFTER_UPDATE = """
            CREATE TRIGGER after_journal_entry_update
            AFTER UPDATE ON journal_entries
            FOR EACH ROW
            CALL update_journal_entry_summary(OLD.user_id, OLD.entry_date, NEW.user_id, NEW.entry_date)
            """;

    private static final String TRIGGER_AFTER_DELETE = """
            CREATE TRIGGER after_journal_entry_delete
            AFTER DELETE ON journal_entries
            FOR EACH ROW
            CALL recalc_daily_summary(OLD.user_id, OLD.entry_date)
            """;

    // ------------------------------------------------------------------
    // Bootstrap
    // ------------------------------------------------------------------

    @Override
    public void run(ApplicationArguments args) {
        if (skip) {
            return;
        }

        String product = detectDatabaseProduct();
        if (product == null) {
            throw new IllegalStateException(
                    "DatabaseBootstrap: cannot detect database product at startup. " +
                            "Check that the DataSource is reachable.");
        }

        if (!product.toLowerCase().contains("mysql")) {
            log.info("DatabaseBootstrap: database is '{}', not MySQL. Skipping schema objects.", product);
            return;
        }

        log.info("DatabaseBootstrap: ensuring MySQL procedures and triggers exist...");

        ensureProcedure("recalc_daily_summary", PROC_RECALC_DAILY_SUMMARY);
        ensureProcedure("insert_journal_entry_summary", PROC_INSERT_JOURNAL_ENTRY_SUMMARY);
        ensureProcedure("update_journal_entry_summary", PROC_UPDATE_JOURNAL_ENTRY_SUMMARY);

        ensureTrigger("after_journal_entry_insert", TRIGGER_AFTER_INSERT);
        ensureTrigger("after_journal_entry_update", TRIGGER_AFTER_UPDATE);
        ensureTrigger("after_journal_entry_delete", TRIGGER_AFTER_DELETE);

        log.info("DatabaseBootstrap: procedures and triggers ensured.");
    }

    private void ensureProcedure(String name, String createSql) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.ROUTINES " +
                        "WHERE ROUTINE_NAME = ? AND ROUTINE_SCHEMA = DATABASE()",
                Integer.class, name);

        if (count != null && count > 0) {
            log.info("  procedure {}: already exists", name);
            return;
        }

        try {
            jdbcTemplate.execute(createSql);
            log.info("  procedure {}: created", name);
        } catch (Exception e) {
            log.error("  procedure {} FAILED", name, e);
            throw new IllegalStateException("Failed to create procedure: " + name, e);
        }
    }

    private void ensureTrigger(String name, String createSql) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TRIGGERS " +
                        "WHERE TRIGGER_NAME = ? AND TRIGGER_SCHEMA = DATABASE()",
                Integer.class, name);

        if (count != null && count > 0) {
            log.info("  trigger {}: already exists", name);
            return;
        }

        try {
            jdbcTemplate.execute(createSql);
            log.info("  trigger {}: created", name);
        } catch (Exception e) {
            log.error("  trigger {} FAILED", name, e);
            throw new IllegalStateException("Failed to create trigger: " + name, e);
        }
    }

    private String detectDatabaseProduct() {
        try (Connection conn = dataSource.getConnection()) {
            return conn.getMetaData().getDatabaseProductName();
        } catch (Exception e) {
            log.error("DatabaseBootstrap: could not detect database product", e);
            return null;
        }
    }
}