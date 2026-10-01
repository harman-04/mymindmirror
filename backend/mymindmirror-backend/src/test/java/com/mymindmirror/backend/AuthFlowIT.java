package com.mymindmirror.backend;

import com.mymindmirror.backend.payload.request.AuthRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Auth flow — end-to-end")
class AuthFlowIT extends AbstractIntegrationTest {

    /**
     * Spy (not mock) — real behavior preserved by default.
     * Mockito resets it before each test, so stubbing in one test
     * doesn't leak into the next.
     */
    @MockitoSpyBean
    private UserDetailsService userDetailsService;

    // =================================================================
    // Happy path (already existed)
    // =================================================================

    @Test
    @DisplayName("register new user → 201 with success message")
    void register_newUser_returns201() throws Exception {
        AuthRequest request = new AuthRequest("alice", "alice@test.com", "password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("User registered successfully."))
                .andExpect(jsonPath("$.token").isEmpty());
    }

    @Test
    @DisplayName("login valid credentials → 200 with JWT")
    void login_validCredentials_returnsJwt() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthRequest("bob", "bob@test.com", "password123"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthRequest("bob", null, "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.message").value("Login successful."));
    }

    @Test
    @DisplayName("protected endpoint without JWT → 401 unified shape")
    void protectedEndpoint_withoutJwt_returns401() throws Exception {
        mockMvc.perform(get("/api/journal/history"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.path").value("/api/journal/history"));
    }

    @Test
    @DisplayName("protected endpoint with valid JWT → 200 (proves full filter chain)")
    void protectedEndpoint_withJwt_returns200() throws Exception {
        String jwt = registerAndLogin("charlie", "password123");

        mockMvc.perform(get("/api/journal/history")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());
    }

    // =================================================================
    // Registration validation
    // =================================================================

    @Test
    @DisplayName("register duplicate username → 400 with specific message")
    void register_duplicateUsername_returns400() throws Exception {
        AuthRequest request = new AuthRequest("dup", "dup@test.com", "password123");

        // First registration succeeds
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Second registration rejected by service layer
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Username already exists."));
    }

    @Test
    @DisplayName("register invalid email → 400 with field-level validation message")
    void register_invalidEmail_returns400() throws Exception {
        AuthRequest request = new AuthRequest("emailuser", "not-an-email", "password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value(org.hamcrest.Matchers.containsString("Email should be valid")));
    }

    @Test
    @DisplayName("register short password → 400 with size validation message")
    void register_shortPassword_returns400() throws Exception {
        AuthRequest request = new AuthRequest("shortpw", "shortpw@test.com", "abc");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value(org.hamcrest.Matchers.containsString(
                                "Password must be at least 6 characters")));
    }

    // =================================================================
    // Login edge cases
    // =================================================================

    @Test
    @DisplayName("login wrong password → 401 'Invalid credentials'")
    void login_wrongPassword_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthRequest("wrongpw", "wrongpw@test.com", "correctpass"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthRequest("wrongpw", null, "wrongpass"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    // =================================================================
    // JWT validation edge cases
    // =================================================================

    @Test
    @DisplayName("malformed JWT → 401")
    void protectedEndpoint_malformedJwt_returns401() throws Exception {
        mockMvc.perform(get("/api/journal/history")
                        .header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("expired JWT → 401")
    void protectedEndpoint_expiredJwt_returns401() throws Exception {
        String expiredToken = generateExpiredToken("anyuser");

        mockMvc.perform(get("/api/journal/history")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("JWT of deleted user → 401 (UsernameNotFoundException path)")
    void protectedEndpoint_deletedUser_returns401() throws Exception {
        // Register + get valid JWT
        String jwt = registerAndLogin("ghost", "password123");

        // Delete the user out from under the token
        userRepository.deleteAll();

        mockMvc.perform(get("/api/journal/history")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isUnauthorized());
    }

    // =================================================================
    // Infrastructure failure — the two bugs we fixed this session
    // =================================================================

    @Test
    @DisplayName("DB down during JWT validation → 503 (not 401)")
    void protectedEndpoint_dbDownDuringFilter_returns503() throws Exception {
        // Valid JWT so the filter reaches the user lookup
        String jwt = registerAndLogin("dbdown", "password123");

        // Now make the user lookup throw as if the DB is unreachable
        doThrow(new DataAccessResourceFailureException("DB down"))
                .when(userDetailsService).loadUserByUsername(anyString());

        mockMvc.perform(get("/api/journal/history")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.error").value("Service Unavailable"))
                .andExpect(jsonPath("$.message").value(
                        "Authentication service is temporarily unavailable. Please try again in a moment."));
    }

    @Test
    @DisplayName("DB down during login → 503 (not 401)")
    void login_dbDownDuringAuthentication_returns503() throws Exception {
        // Register normally first (real DB)
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthRequest("loginuser", "loginuser@test.com", "password123"))))
                .andExpect(status().isCreated());

        // Now make the user lookup throw during login
        doThrow(new DataAccessResourceFailureException("DB down"))
                .when(userDetailsService).loadUserByUsername(anyString());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthRequest("loginuser", null, "password123"))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.message").value(
                        "Authentication service is temporarily unavailable. Please try again in a moment."));
    }
}