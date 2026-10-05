package com.mymindmirror.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mymindmirror.backend.config.TestVectorStoreConfig;
import com.mymindmirror.backend.payload.request.LoginRequest;
import com.mymindmirror.backend.payload.request.RegisterRequest;
import com.mymindmirror.backend.repository.JournalEntryRepository;
import com.mymindmirror.backend.repository.UserRepository;
import com.mymindmirror.backend.repository.UserStatsRepository;
import com.mymindmirror.backend.service.ai.DynamicAiClientService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = {BackendApplication.class, TestVectorStoreConfig.class},
        properties = {
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.show-sql=false",
                "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect",
                "spring.datasource.hikari.maximum-pool-size=5",
                "spring.datasource.hikari.max-lifetime=60000"
        }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    static final MySQLContainer MYSQL = new MySQLContainer(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("mymindmirror")
            .withUsername("test")
            .withPassword("test");


    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    static {
        Startables.deepStart(MYSQL, REDIS).join();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { MYSQL.stop(); } catch (Exception ignored) {}
            try { REDIS.stop(); } catch (Exception ignored) {}
        }, "testcontainers-shutdown"));
    }

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");

        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired private DataSource dataSource;
    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactionManager;

    @Autowired(required = false)
    private CacheManager cacheManager;

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;

    @Autowired protected UserRepository userRepository;
    @Autowired protected JournalEntryRepository journalEntryRepository;
    @Autowired protected UserStatsRepository userStatsRepository;

    @MockitoBean protected DynamicAiClientService aiClientService;

    @BeforeEach
    void resetDatabase() {
        new TransactionTemplate(transactionManager).execute(status -> {
            entityManager.createQuery("DELETE FROM ScheduledTask").executeUpdate();
            entityManager.createQuery("DELETE FROM Task").executeUpdate();
            entityManager.createQuery("DELETE FROM RoadmapTask").executeUpdate();
            entityManager.createQuery("DELETE FROM RoadmapResource").executeUpdate();
            entityManager.createQuery("DELETE FROM RoadmapMilestone").executeUpdate();
            entityManager.createQuery("DELETE FROM KeyPhrase").executeUpdate();
            entityManager.createQuery("DELETE FROM JournalEntry").executeUpdate();
            entityManager.createQuery("DELETE FROM Milestone").executeUpdate();
            entityManager.createQuery("DELETE FROM Roadmap").executeUpdate();
            entityManager.createQuery("DELETE FROM CustomTask").executeUpdate();
            entityManager.createQuery("DELETE FROM DailyJournalSummary").executeUpdate();
            entityManager.createQuery("DELETE FROM UserStats").executeUpdate();
            entityManager.createQuery("DELETE FROM UserPreferences").executeUpdate();
            entityManager.createQuery("DELETE FROM UserRoadmapPreferences").executeUpdate();
            entityManager.createQuery("DELETE FROM User").executeUpdate();

            entityManager.flush();
            entityManager.clear();
            return null;
        });
    }

    @BeforeEach
    void resetCaches() {
        if (cacheManager == null) {
            return;
        }
        for (String name : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        }
    }

    @BeforeEach
    void assertNotConnectedToProduction() throws Exception {
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

    protected String registerAndLogin(String username, String password) throws Exception {
        String email = username + "@test.com";

        RegisterRequest reg = new RegisterRequest(username, email, password);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        LoginRequest login = new LoginRequest(username, password);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
    }

    protected String generateExpiredToken(String username) {
        java.security.Key key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970".getBytes());
        return io.jsonwebtoken.Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new java.util.Date(System.currentTimeMillis() - 60_000))
                .setExpiration(new java.util.Date(System.currentTimeMillis() - 1_000))
                .signWith(key, io.jsonwebtoken.SignatureAlgorithm.HS256)
                .compact();
    }
}