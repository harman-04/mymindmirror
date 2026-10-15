package com.mymindmirror.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.payload.request.ChatRequest;
import com.mymindmirror.backend.security.JwtUtil;
import com.mymindmirror.backend.security.services.UserDetailsImpl;
import com.mymindmirror.backend.service.ChatMemoryService;
import com.mymindmirror.backend.service.JournalService;
import com.mymindmirror.backend.service.UserService;
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

@WebMvcTest(ReflectionChatController.class)
class ReflectionChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JournalService journalService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private ChatMemoryService chatMemoryService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private JwtUtil jwtUtil;

    private User currentUser;
    private UUID userId;

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
    }

    // ===== REFLECT =====

    @Test
    void reflect_shouldReturn200() throws Exception {
        ChatRequest request = new ChatRequest("How am I doing lately?", "session-123", true);
        when(journalService.generateReflectionChat(
                any(User.class), anyString(), anyString(), anyBoolean()))
                .thenReturn("You seem to be doing well based on your recent entries.");

        mockMvc.perform(post("/api/chat/reflect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("You seem to be doing well based on your recent entries."));

    }

    @Test
    void reflect_shouldHandleNullSession() throws Exception {
        ChatRequest request = new ChatRequest("Query", null, false);
        when(journalService.generateReflectionChat(any(User.class), anyString(), isNull(), eq(false)))
                .thenReturn("Answer without session");

        mockMvc.perform(post("/api/chat/reflect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Answer without session"));
    }

    // ===== SUGGEST QUESTION =====

    @Test
    void suggestQuestion_shouldReturn200() throws Exception {
        when(journalService.generateReflectiveQuestion(any(User.class)))
                .thenReturn("What did you learn about yourself this week?");

        mockMvc.perform(post("/api/chat/suggest-question"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("What did you learn about yourself this week?"));
    }

    // ===== CLEAR MEMORY =====

    @Test
    void clearMemory_shouldReturn200_whenSessionIdProvided() throws Exception {
        doNothing().when(chatMemoryService).clearHistory(any(UUID.class), anyString());

        mockMvc.perform(delete("/api/chat/clear-memory")
                        .param("sessionId", "session-456"))
                .andExpect(status().isOk());

        verify(chatMemoryService, times(1)).clearHistory(eq(userId), eq("session-456"));
    }

    @Test
    void clearMemory_shouldReturn200_whenSessionIdBlank() throws Exception {
        // Should not call chatMemoryService when sessionId is blank
        mockMvc.perform(delete("/api/chat/clear-memory")
                        .param("sessionId", "   "))
                .andExpect(status().isOk());

        verify(chatMemoryService, never()).clearHistory(any(UUID.class), anyString());
    }

    @Test
    void reflect_shouldReturn400_whenQueryBlank() throws Exception {
        ChatRequest request = new ChatRequest("", "session-123", true);

        mockMvc.perform(post("/api/chat/reflect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(journalService);
    }

    @Test
    void reflect_shouldReturn400_whenQueryMissing() throws Exception {
        // Send raw JSON without the query field
        String body = "{\"sessionId\":\"session-123\",\"rememberChat\":true}";

        mockMvc.perform(post("/api/chat/reflect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(journalService);
    }
}