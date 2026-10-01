package com.mymindmirror.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.payload.request.ApiKeyRequest;
import com.mymindmirror.backend.payload.response.ApiKeyStatusResponse;
import com.mymindmirror.backend.security.JwtUtil;
import com.mymindmirror.backend.security.services.UserDetailsImpl;
import com.mymindmirror.backend.service.ApiKeyService;
import com.mymindmirror.backend.service.UserService;
import com.mymindmirror.backend.service.ai.DynamicAiClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ApiKeyController.class)
class ApiKeyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ApiKeyService apiKeyService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private DynamicAiClientService dynamicAiClientService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private JwtUtil jwtUtil;

    private User currentUser;
    private UUID userId;
    private ApiKeyStatusResponse mockStatusResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        currentUser = new User();
        currentUser.setId(userId);
        currentUser.setUsername("testuser");
        currentUser.setEmail("test@example.com");
        currentUser.setPasswordHash("hashed");

        when(userService.findById(eq(userId))).thenReturn(Optional.of(currentUser));

        UserDetailsImpl userDetails = new UserDetailsImpl(
                userId, currentUser.getUsername(), currentUser.getEmail(), currentUser.getPasswordHash(), null);
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockStatusResponse = new ApiKeyStatusResponse(
                true,
                "••••••••abcd",
                "Using your own Gemini API key."
        );
    }

    // ===== UPDATE API KEY =====

    @Test
    void updateApiKey_shouldReturn204_whenKeyProvided() throws Exception {
        ApiKeyRequest request = new ApiKeyRequest("sk-test-key-1234567890");

        doNothing().when(apiKeyService).saveApiKey(any(User.class), anyString());
        doNothing().when(dynamicAiClientService).evictUserModel(any(UUID.class));

        mockMvc.perform(put("/api/users/api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(apiKeyService, times(1)).saveApiKey(any(User.class), eq("sk-test-key-1234567890"));
        verify(dynamicAiClientService, times(1)).evictUserModel(eq(userId));
    }

    @Test
    void updateApiKey_shouldReturn204_whenKeyCleared() throws Exception {
        ApiKeyRequest request = new ApiKeyRequest(null);

        doNothing().when(apiKeyService).saveApiKey(any(User.class), isNull());
        doNothing().when(dynamicAiClientService).evictUserModel(any(UUID.class));

        mockMvc.perform(put("/api/users/api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(apiKeyService, times(1)).saveApiKey(any(User.class), isNull());
        verify(dynamicAiClientService, times(1)).evictUserModel(eq(userId));
    }

    @Test
    void updateApiKey_shouldReturn204_whenKeyBlank() throws Exception {
        ApiKeyRequest request = new ApiKeyRequest("   ");

        doNothing().when(apiKeyService).saveApiKey(any(User.class), isNull());
        doNothing().when(dynamicAiClientService).evictUserModel(any(UUID.class));

        mockMvc.perform(put("/api/users/api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(apiKeyService, times(1)).saveApiKey(any(User.class), isNull());
        verify(dynamicAiClientService, times(1)).evictUserModel(eq(userId));
    }

    // ===== GET API KEY STATUS =====

    @Test
    void getApiKeyStatus_shouldReturn200() throws Exception {
        when(apiKeyService.getApiKeyStatus(any(User.class))).thenReturn(mockStatusResponse);

        mockMvc.perform(get("/api/users/api-key/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usingOwnKey").value(true))
                .andExpect(jsonPath("$.maskedKey").value("••••••••abcd"))
                .andExpect(jsonPath("$.message").value("Using your own Gemini API key."));
    }

    @Test
    void getApiKeyStatus_shouldReturn200_whenNotUsingOwnKey() throws Exception {
        ApiKeyStatusResponse noKeyResponse = new ApiKeyStatusResponse(
                false,
                null,
                "Using shared API key. For better privacy, add your own key."
        );
        when(apiKeyService.getApiKeyStatus(any(User.class))).thenReturn(noKeyResponse);

        mockMvc.perform(get("/api/users/api-key/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usingOwnKey").value(false))
                .andExpect(jsonPath("$.maskedKey").doesNotExist())
                .andExpect(jsonPath("$.message").value("Using shared API key. For better privacy, add your own key."));
    }
}