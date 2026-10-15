package com.mymindmirror.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mymindmirror.backend.config.properties.MySqlDataSourceProperties;
import com.mymindmirror.backend.config.properties.VectorDataSourceProperties;
import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;

import java.time.Clock;

@SpringBootApplication(exclude = PgVectorStoreAutoConfiguration.class)
@EnableAsync
@EnableConfigurationProperties({MySqlDataSourceProperties.class, VectorDataSourceProperties.class})
public class BackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Registering this module because you use Java 8+ Dates (LocalDate, etc.)
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone(); // Uses real time in production
    }
}