package com.mymindmirror.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mymindmirror.backend.payload.request.LoginRequest;
import com.mymindmirror.backend.payload.request.RegisterRequest;
import com.mymindmirror.backend.payload.response.AuthResponse;
import com.mymindmirror.backend.security.JwtUtil;
import com.mymindmirror.backend.security.services.UserDetailsImpl;
import com.mymindmirror.backend.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserDetailsService userDetailsService;
    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private JwtUtil jwtUtil;

    // ========== Registration Tests ==========

    @Test
    void registerUser_shouldReturn201_whenValidRequest() throws Exception {
        RegisterRequest request = new RegisterRequest("testuser", "test@example.com", "password123");

        when(userService.registerNewUser("testuser", "test@example.com", "password123"))
                .thenReturn(null);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("User registered successfully."))
                .andExpect(jsonPath("$.token").isEmpty());
    }

    @Test
    void registerUser_shouldReturn400_whenUsernameMissing() throws Exception {
        RegisterRequest request = new RegisterRequest(null, "test@example.com", "password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerUser_shouldReturn400_whenEmailInvalid() throws Exception {
        RegisterRequest request = new RegisterRequest("testuser", "invalid-email", "password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerUser_shouldReturn400_whenEmailMissing() throws Exception {
        RegisterRequest request = new RegisterRequest("testuser", null, "password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value(org.hamcrest.Matchers.containsString("Email is required")));
    }

    @Test
    void registerUser_shouldReturn400_whenPasswordMissing() throws Exception {
        RegisterRequest request = new RegisterRequest("testuser", "test@example.com", null);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerUser_shouldReturn400_whenUserAlreadyExists() throws Exception {
        RegisterRequest request = new RegisterRequest("existinguser", "test@example.com", "password123");

        when(userService.registerNewUser("existinguser", "test@example.com", "password123"))
                .thenThrow(new IllegalArgumentException("Username already exists."));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Username already exists."));
    }

    @Test
    void registerUser_shouldReturn400_whenPasswordTooShort() throws Exception {
        RegisterRequest request = new RegisterRequest("testuser", "test@example.com", "abc");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerUser_shouldReturn400_whenUsernameTooShort() throws Exception {
        RegisterRequest request = new RegisterRequest("ab", "test@example.com", "password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerUser_shouldReturn400_whenUsernameTooLong() throws Exception {
        String tooLong = "a".repeat(51);
        RegisterRequest request = new RegisterRequest(tooLong, "test@example.com", "password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerUser_shouldReturn201_whenUsernameIsExactly3Chars() throws Exception {
        RegisterRequest request = new RegisterRequest("abc", "test@example.com", "password123");

        when(userService.registerNewUser("abc", "test@example.com", "password123"))
                .thenReturn(null);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void registerUser_shouldReturn201_whenUsernameIsExactly50Chars() throws Exception {
        String fiftyChars = "a".repeat(50);
        RegisterRequest request = new RegisterRequest(fiftyChars, "test@example.com", "password123");

        when(userService.registerNewUser(fiftyChars, "test@example.com", "password123"))
                .thenReturn(null);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    // ========== Login Tests ==========

    @Test
    void loginUser_shouldReturn200_whenValidCredentials() throws Exception {
        LoginRequest request = new LoginRequest("testuser", "password123");
        UUID userId = UUID.randomUUID();
        UserDetailsImpl userDetails = new UserDetailsImpl(
                userId, "testuser", "test@example.com", "password", null);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtUtil.generateToken(userDetails, userId)).thenReturn("mock-jwt-token");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock-jwt-token"))
                .andExpect(jsonPath("$.message").value("Login successful."))
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        AuthResponse response = objectMapper.readValue(jsonResponse, AuthResponse.class);
        assertThat(response.token()).isEqualTo("mock-jwt-token");
    }

    @Test
    void loginUser_shouldReturn400_whenUsernameMissing() throws Exception {
        LoginRequest request = new LoginRequest(null, "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginUser_shouldReturn400_whenPasswordMissing() throws Exception {
        LoginRequest request = new LoginRequest("testuser", null);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginUser_shouldReturn401_whenInvalidCredentials() throws Exception {
        LoginRequest request = new LoginRequest("testuser", "wrongpassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new org.springframework.security.authentication.BadCredentialsException("Invalid credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginUser_shouldReturn200_withoutEmailField() throws Exception {
        // LoginRequest has no email field at all — proves login works without one
        LoginRequest request = new LoginRequest("testuser", "password123");
        UUID userId = UUID.randomUUID();
        UserDetailsImpl userDetails = new UserDetailsImpl(
                userId, "testuser", "test@example.com", "password", null);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtUtil.generateToken(userDetails, userId)).thenReturn("mock-jwt-token");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock-jwt-token"));
    }
}