package com.mymindmirror.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.mymindmirror.backend.service.JournalService;
import com.mymindmirror.backend.service.MilestoneService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Error contract — every failure returns the unified shape")
class ErrorContractIT extends AbstractIntegrationTest {

    /** Spies preserve real behavior by default; we override per-test only for specific failures. */
    @MockitoSpyBean private UserDetailsService userDetailsService;
    @MockitoSpyBean private JournalService journalService;
    @MockitoSpyBean private CacheManager cacheManager;
    @MockitoSpyBean private MilestoneService milestoneService;
    private String aliceJwt;

    @BeforeEach
    void loginAlice() throws Exception {
        aliceJwt = registerAndLogin("alice", "password123");
    }

    // =================================================================
    // Shared shape assertion
    // =================================================================

    private void assertUnifiedShape(MvcResult result, int expectedStatus, String expectedErrorCategory)
            throws Exception {
        JsonNode body = objectMapper.readTree(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8));

        assertThat(body.has("timestamp")).as("timestamp present").isTrue();
        assertThat(body.get("status").asInt()).as("status").isEqualTo(expectedStatus);
        assertThat(body.get("error").asText()).as("error category").isEqualTo(expectedErrorCategory);
        assertThat(body.has("message")).as("message present").isTrue();
        assertThat(body.get("message").asText()).as("message non-blank").isNotBlank();
        assertThat(body.has("path")).as("path present").isTrue();
        assertThat(body.get("path").asText()).as("path non-blank").isNotBlank();
        // test profile has app.error.expose-debug=false, so debug_message must be absent
        assertThat(body.has("debug_message")).as("debug_message absent").isFalse();
    }

    private String messageOf(MvcResult result) throws Exception {
        return objectMapper.readTree(
                        result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .get("message").asText();
    }

    // =================================================================
    // 400 — Validation and malformed input
    // =================================================================

    @Test
    @DisplayName("400 — bean validation on @RequestBody (invalid email)")
    void validation_returns400() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"u\",\"email\":\"not-an-email\",\"password\":\"password123\"}"))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertUnifiedShape(result, 400, "Bad Request");
        assertThat(messageOf(result)).contains("Validation failed");
        assertThat(messageOf(result)).contains("Email should be valid");
    }

    @Test
    @DisplayName("400 — malformed JSON body")
    void malformedJson_returns400() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ this is not valid json "))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertUnifiedShape(result, 400, "Bad Request");
        assertThat(messageOf(result)).isEqualTo("Malformed JSON request or empty body.");
    }

    @Test
    @DisplayName("400 — IllegalArgument from controller (mood min > max)")
    void illegalArgument_returns400() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/journal/search/mood")
                        .header("Authorization", "Bearer " + aliceJwt)
                        .param("minMood", "0.9")
                        .param("maxMood", "0.1"))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertUnifiedShape(result, 400, "Bad Request");
        assertThat(messageOf(result))
                .isEqualTo("Minimum mood cannot be greater than maximum mood.");
    }

    // =================================================================
    // 401 — Authentication
    // =================================================================

    @Test
    @DisplayName("401 — no JWT header")
    void noJwt_returns401() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/journal/history"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertUnifiedShape(result, 401, "Unauthorized");
        assertThat(messageOf(result)).contains("Full authentication is required");
    }

    @Test
    @DisplayName("401 — bad credentials on login")
    void badCredentials_returns401() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"wrongpassword\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertUnifiedShape(result, 401, "Unauthorized");
        assertThat(messageOf(result)).isEqualTo("Invalid credentials");
    }

    // =================================================================
    // 403 — Access denied (handler still exists; production path removed)
    // =================================================================

    @Test
    @DisplayName("403 — AccessDeniedException from a service method (handler wiring)")
    void accessDenied_returns403() throws Exception {
        doThrow(new AccessDeniedException("Forbidden"))
                .when(journalService).getMoodDataForChart(any(), any(), any());

        MvcResult result = mockMvc.perform(get("/api/journal/mood-data")
                        .header("Authorization", "Bearer " + aliceJwt))
                .andExpect(status().isForbidden())
                .andReturn();

        assertUnifiedShape(result, 403, "Forbidden");
        assertThat(messageOf(result))
                .isEqualTo("You do not have permission to access this resource.");
    }

    // =================================================================
    // 404 — Not found (unified cross-user and deleted-resource policy)
    // =================================================================

    @Test
    @DisplayName("404 — deleted resource (roadmap)")
    void deletedResource_returns404() throws Exception {
        MvcResult result = mockMvc.perform(delete("/api/roadmap/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + aliceJwt))
                .andExpect(status().isNotFound())
                .andReturn();

        assertUnifiedShape(result, 404, "Not Found");
        assertThat(messageOf(result)).isEqualTo("Roadmap not found");
    }

    // =================================================================
    // 500 — Unexpected errors
    // =================================================================

    @Test
    @DisplayName("500 — unexpected RuntimeException from a service")
    void genericRuntime_returns500() throws Exception {
        doThrow(new RuntimeException("Boom"))
                .when(journalService).getMoodDataForChart(any(), any(), any());

        MvcResult result = mockMvc.perform(get("/api/journal/mood-data")
                        .header("Authorization", "Bearer " + aliceJwt))
                .andExpect(status().isInternalServerError())
                .andReturn();

        assertUnifiedShape(result, 500, "Internal Server Error");
        assertThat(messageOf(result)).isEqualTo("An unexpected error occurred.");
    }

    // =================================================================
    // 503 — Infrastructure failures (Redis, DB, AI)
    // =================================================================

    @Test
    @DisplayName("503 — Redis down during a cacheable call")
    void redisDown_returns503() throws Exception {
        when(cacheManager.getCache(anyString()))
                .thenThrow(new RedisConnectionFailureException("Redis down"));

        MvcResult result = mockMvc.perform(get("/api/users/profile-full")
                        .header("Authorization", "Bearer " + aliceJwt))
                .andExpect(status().isServiceUnavailable())
                .andReturn();

        assertUnifiedShape(result, 503, "Service Unavailable");
        assertThat(messageOf(result))
                .isEqualTo("Cache service is temporarily unavailable. Please try again in a moment.");
    }

    @Test
    @DisplayName("503 — DB down during JWT filter's user lookup")
    void dbDownDuringFilter_returns503() throws Exception {
        doThrow(new DataAccessResourceFailureException("DB down"))
                .when(userDetailsService).loadUserByUsername(anyString());

        MvcResult result = mockMvc.perform(get("/api/journal/history")
                        .header("Authorization", "Bearer " + aliceJwt))
                .andExpect(status().isServiceUnavailable())
                .andReturn();

        assertUnifiedShape(result, 503, "Service Unavailable");
        assertThat(messageOf(result))
                .isEqualTo("Authentication service is temporarily unavailable. Please try again in a moment.");
    }

    @Test
    @DisplayName("503 — AI exhausted (key-aware message reaches user)")
    void aiExhausted_returns503() throws Exception {
        when(aiClientService.generateStructured(anyString(), any(), any(), any()))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "The shared AI service is currently at capacity. " +
                                "Please try again shortly, or add your own API key in " +
                                "Profile → Gemini API Key for priority access."));

        String body = objectMapper.writeValueAsString(
                new com.mymindmirror.backend.payload.request.ImportGrowthTipRequest("Be kind to yourself"));

        MvcResult result = mockMvc.perform(post("/api/milestones/import-growth-tip")
                        .header("Authorization", "Bearer " + aliceJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isServiceUnavailable())
                .andReturn();

        assertUnifiedShape(result, 503, "Service Unavailable");
        assertThat(messageOf(result)).contains("shared AI service");
        assertThat(messageOf(result)).contains("add your own API key");
    }

    @Test
    @DisplayName("400 — missing required @RequestParam (keyword)")
    void missingRequestParam_returns400() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/journal/search/keyword")
                        .header("Authorization", "Bearer " + aliceJwt))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertUnifiedShape(result, 400, "Bad Request");
        assertThat(messageOf(result)).contains("Missing required parameter");
        assertThat(messageOf(result)).contains("keyword");
    }

    @Test
    @DisplayName("400 — non-UUID path variable on /api/journal/{id}")
    void typeMismatch_returns400() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/journal/not-a-uuid")
                        .header("Authorization", "Bearer " + aliceJwt))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertUnifiedShape(result, 400, "Bad Request");
        assertThat(messageOf(result)).contains("id");
    }

    @Test
    @DisplayName("415 — wrong Content-Type on a JSON endpoint")
    void unsupportedMediaType_returns415() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("plain text instead of json"))
                .andExpect(status().isUnsupportedMediaType())
                .andReturn();

        assertUnifiedShape(result, 415, "Unsupported Media Type");
    }

    @Test
    @DisplayName("405 — wrong HTTP verb on an existing path")
    void methodNotAllowed_returns405() throws Exception {
        // /api/auth/register exists as POST; call it with PUT
        MvcResult result = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isMethodNotAllowed())
                .andReturn();

        assertUnifiedShape(result, 405, "Method Not Allowed");
    }

    @Test
    @DisplayName("406 — client Accept header cannot be satisfied (empty body by HTTP semantics)")
    void notAcceptable_returns406() throws Exception {
        mockMvc.perform(get("/api/journal/history")
                        .header("Authorization", "Bearer " + aliceJwt)
                        .accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable());
    }

    @Test
    @DisplayName("503 — Reflection: DB failure surfaces as 503, not 500")
    void reflectionDbFailure_returns503() throws Exception {
        doThrow(new DataAccessResourceFailureException("DB down"))
                .when(journalService).generateDailyReflection(any());

        MvcResult result = mockMvc.perform(post("/api/reflection/generate")
                        .header("Authorization", "Bearer " + aliceJwt))
                .andExpect(status().isServiceUnavailable())
                .andReturn();

        assertUnifiedShape(result, 503, "Service Unavailable");
        assertThat(messageOf(result))
                .isEqualTo("A backend service is temporarily unavailable. Please try again in a moment.");
    }

    @Test
    @DisplayName("503 — Milestone insights: DB failure surfaces as 503, not 500")
    void milestoneInsightsDbFailure_returns503() throws Exception {
        doThrow(new DataAccessResourceFailureException("DB down"))
                .when(milestoneService).getMilestoneByIdForUser(any(), any());

        MvcResult result = mockMvc.perform(get("/api/milestones/{id}/insights", UUID.randomUUID())
                        .header("Authorization", "Bearer " + aliceJwt))
                .andExpect(status().isServiceUnavailable())
                .andReturn();

        assertUnifiedShape(result, 503, "Service Unavailable");
    }
}