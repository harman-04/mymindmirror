package com.mymindmirror.backend.config;

import com.mymindmirror.backend.config.properties.VectorDataSourceProperties;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Slf4j
@Configuration
@Profile("!test")  // Exclude when test profile is active
@EnableConfigurationProperties(VectorDataSourceProperties.class)
public class VectorDatabaseConfig {

    @Value("${spring.ai.vectorstore.pgvector.dimensions:3072}")
    private int vectorDimensions;

    @Bean(name = "vectorDataSource")
    public DataSource vectorDataSource(VectorDataSourceProperties properties) {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(properties.url());
        ds.setUsername(properties.username());
        ds.setPassword(properties.password());
        ds.setDriverClassName(properties.driverClassName());
        ds.setMaximumPoolSize(5);
        // Helps prevent the "connection closed" warnings from cloud databases
        ds.setMaxLifetime(240000);
        ds.setConnectionTimeout(5000);
        ds.setValidationTimeout(3000);
        return ds;
    }

    @Bean(name = "vectorJdbcTemplate")
    public JdbcTemplate vectorJdbcTemplate(@Qualifier("vectorDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public VectorStore vectorStore(
            @Qualifier("vectorJdbcTemplate") JdbcTemplate jdbcTemplate,
            EmbeddingModel embeddingModel) {

        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .dimensions(vectorDimensions)
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                .indexType(PgVectorStore.PgIndexType.NONE) // Exact Search: 100% Accuracy
                .initializeSchema(true)
                .build();
    }

    /**
     * Automatically applies enterprise performance indexes to the Postgres database on startup.
     * By requiring 'VectorStore' as a parameter, Spring guarantees the 'vector_store' table
     * is fully created before this runner executes.
     */
    @Bean
    public ApplicationRunner optimizeVectorDatabase(
            @Qualifier("vectorJdbcTemplate") JdbcTemplate vectorJdbcTemplate,
            VectorStore vectorStore) {
        return args -> {
            log.info("Optimizing PostgreSQL Vector Store with Multi-Tenant Indexes...");
            try {
                // 1. B-Tree expression index for lightning-fast user isolation
                vectorJdbcTemplate.execute(
                        "CREATE INDEX IF NOT EXISTS idx_vector_store_user_id ON vector_store ((metadata->>'userId'))"
                );
                log.info("Successfully created multi-tenant B-Tree index on userId.");

                // 2. GIN index (Requires casting to jsonb since Spring AI defaults to json)
                vectorJdbcTemplate.execute(
                        "CREATE INDEX IF NOT EXISTS idx_vector_store_metadata_gin ON vector_store USING gin ((metadata::jsonb))"
                );
                log.info("Successfully created GIN index on metadata.");

            } catch (Exception e) {
                log.error("Failed to create vector store indexes: {}", e.getMessage());
            }
        };
    }
}