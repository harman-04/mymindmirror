package com.mymindmirror.backend.config;

import com.mymindmirror.backend.config.properties.VectorDataSourceProperties;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VectorDatabaseConfigTest {

    @Mock
    private JdbcTemplate vectorJdbcTemplate;

    @Mock
    private VectorStore vectorStore;

    private VectorDatabaseConfig config;

    // Reusable ApplicationArguments for runner.run()
    private final ApplicationArguments emptyArgs = new DefaultApplicationArguments();

    @BeforeEach
    void setUp() {
        config = new VectorDatabaseConfig();
        ReflectionTestUtils.setField(config, "vectorDimensions", 3072);
    }

    // ============================================================
    // vectorDataSource()
    // ============================================================

    @Test
    @DisplayName("vectorDataSource() configures Hikari pool with expected values")
    void vectorDataSource_configuresPoolCorrectly() {
        VectorDataSourceProperties props = new VectorDataSourceProperties(
                "jdbc:postgresql://localhost:5432/vector_db",
                "vectoruser",
                "vectorpass",
                "org.postgresql.Driver");

        DataSource ds = config.vectorDataSource(props);

        assertThat(ds).isInstanceOf(HikariDataSource.class);
        HikariDataSource hikari = (HikariDataSource) ds;
        assertThat(hikari.getJdbcUrl()).isEqualTo("jdbc:postgresql://localhost:5432/vector_db");
        assertThat(hikari.getUsername()).isEqualTo("vectoruser");
        assertThat(hikari.getPassword()).isEqualTo("vectorpass");
        assertThat(hikari.getDriverClassName()).isEqualTo("org.postgresql.Driver");
        assertThat(hikari.getMaximumPoolSize()).isEqualTo(5);
        assertThat(hikari.getMaxLifetime()).isEqualTo(240_000L);

        hikari.close();
    }

    // ============================================================
    // vectorJdbcTemplate()
    // ============================================================

    @Test
    @DisplayName("vectorJdbcTemplate() wraps the provided DataSource")
    void vectorJdbcTemplate_wrapsDataSource() {
        DataSource mockDs = mock(DataSource.class);

        JdbcTemplate template = config.vectorJdbcTemplate(mockDs);

        assertThat(template).isNotNull();
        assertThat(template.getDataSource()).isSameAs(mockDs);
    }

    // ============================================================
    // optimizeVectorDatabase() – happy path
    // ============================================================

    @Test
    @DisplayName("optimizeVectorDatabase() creates both B-Tree and GIN indexes on startup")
    void optimizeVectorDatabase_createsBothIndexes() throws Exception {
        ApplicationRunner runner = config.optimizeVectorDatabase(vectorJdbcTemplate, vectorStore);

        //  Correct signature: run(ApplicationArguments)
        runner.run(emptyArgs);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(vectorJdbcTemplate, times(2)).execute(sqlCaptor.capture());

        List<String> sqls = sqlCaptor.getAllValues();
        assertThat(sqls).anyMatch(s -> s.contains("idx_vector_store_user_id")
                && s.contains("metadata->>'userId'"));
        assertThat(sqls).anyMatch(s -> s.contains("idx_vector_store_metadata_gin")
                && s.contains("gin")
                && s.contains("metadata::jsonb"));
    }

    // ============================================================
    // optimizeVectorDatabase() – error path (previously uncovered)
    // ============================================================

    @Test
    @DisplayName("optimizeVectorDatabase() swallows exceptions so app startup is not blocked")
    void optimizeVectorDatabase_swallowsException() throws Exception {
        //  Void method → use doThrow().when() instead of when().thenThrow()
        doThrow(new RuntimeException("relation \"vector_store\" does not exist"))
                .when(vectorJdbcTemplate).execute(anyString());

        ApplicationRunner runner = config.optimizeVectorDatabase(vectorJdbcTemplate, vectorStore);

        //  run() no longer declares a checked exception, so we can pass the args directly
        assertThatCode(() -> runner.run(emptyArgs)).doesNotThrowAnyException();

        //  Only first execute() attempt happened – exception stopped the sequence
        verify(vectorJdbcTemplate, times(1)).execute(anyString());
    }

    @Test
    @DisplayName("optimizeVectorDatabase() does not rethrow when second index creation fails")
    void optimizeVectorDatabase_swallowsExceptionOnSecondIndex() throws Exception {
        // First call OK (void), second call fails (void)
        doNothing()
                .doThrow(new RuntimeException("GIN index creation failed"))
                .when(vectorJdbcTemplate).execute(anyString());

        ApplicationRunner runner = config.optimizeVectorDatabase(vectorJdbcTemplate, vectorStore);

        assertThatCode(() -> runner.run(emptyArgs)).doesNotThrowAnyException();
        verify(vectorJdbcTemplate, times(2)).execute(anyString());
    }

    @Test
    @DisplayName("vectorStore() bean builds PgVectorStore correctly")
    void vectorStore_buildsSuccessfully() {
        JdbcTemplate mockJdbc = mock(JdbcTemplate.class);
        EmbeddingModel mockModel = mock(EmbeddingModel.class);

        VectorStore store = config.vectorStore(mockJdbc, mockModel);

        assertThat(store).isNotNull();
    }
}