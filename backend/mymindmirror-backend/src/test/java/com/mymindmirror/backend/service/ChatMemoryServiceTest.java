package com.mymindmirror.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatMemoryServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ListOperations<String, Object> listOperations;

    @InjectMocks
    private ChatMemoryService chatMemoryService;

    private UUID userId;
    private String sessionId;
    private String key;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        sessionId = "session-123";
        key = "chat:memory:" + userId + ":" + sessionId;

        //  Use lenient() to avoid UnnecessaryStubbingException for tests that don't use opsForList()
        lenient().when(redisTemplate.opsForList()).thenReturn(listOperations);
    }

    // ========== appendMessage Tests ==========

    @Test
    void appendMessage_shouldPushMessageAndSetExpiry() {
        // Given
        String role = "user";
        String content = "Hello";

        // When
        chatMemoryService.appendMessage(userId, sessionId, role, content);

        // Then
        ArgumentCaptor<ChatMemoryService.ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMemoryService.ChatMessage.class);
        verify(listOperations).rightPush(eq(key), messageCaptor.capture());
        ChatMemoryService.ChatMessage captured = messageCaptor.getValue();
        assertThat(captured.role()).isEqualTo(role);
        assertThat(captured.content()).isEqualTo(content);

        verify(listOperations).size(key);
        verify(redisTemplate).expire(eq(key), eq(1L), eq(TimeUnit.HOURS));
        verify(listOperations, never()).leftPop(anyString());
    }

    @Test
    void appendMessage_shouldTrimHistoryWhenExceedsMaxSize() {
        // Given
        when(listOperations.size(key)).thenReturn(11L);

        // When
        chatMemoryService.appendMessage(userId, sessionId, "user", "Hello");

        // Then
        verify(listOperations).leftPop(key);
    }

    @Test
    void appendMessage_shouldNotTrimWhenSizeIsNull() {
        // Given
        when(listOperations.size(key)).thenReturn(null);

        // When
        chatMemoryService.appendMessage(userId, sessionId, "user", "Hello");

        // Then
        verify(listOperations, never()).leftPop(anyString());
        verify(redisTemplate).expire(eq(key), eq(1L), eq(TimeUnit.HOURS));
    }

    @Test
    void appendMessage_shouldNotTrimWhenSizeIsLessThanOrEqualToMax() {
        // Given
        when(listOperations.size(key)).thenReturn(10L);

        // When
        chatMemoryService.appendMessage(userId, sessionId, "user", "Hello");

        // Then
        verify(listOperations, never()).leftPop(anyString());
    }

    // ========== getHistory Tests ==========

    @Test
    void getHistory_shouldReturnListOfChatMessages() {
        // Given
        ChatMemoryService.ChatMessage msg1 = new ChatMemoryService.ChatMessage("user", "Hello");
        ChatMemoryService.ChatMessage msg2 = new ChatMemoryService.ChatMessage("model", "Hi there");
        List<Object> rawList = List.of(msg1, msg2);
        when(listOperations.range(key, 0, -1)).thenReturn(rawList);

        // When
        List<ChatMemoryService.ChatMessage> result = chatMemoryService.getHistory(userId, sessionId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(msg1, msg2);
    }

    @Test
    void getHistory_shouldReturnEmptyListWhenRangeReturnsNull() {
        // Given
        when(listOperations.range(key, 0, -1)).thenReturn(null);

        // When
        List<ChatMemoryService.ChatMessage> result = chatMemoryService.getHistory(userId, sessionId);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getHistory_shouldReturnEmptyListWhenRangeReturnsEmptyList() {
        // Given
        when(listOperations.range(key, 0, -1)).thenReturn(List.of());

        // When
        List<ChatMemoryService.ChatMessage> result = chatMemoryService.getHistory(userId, sessionId);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getHistory_shouldFilterOutNonChatMessageObjects() {
        // Given
        ChatMemoryService.ChatMessage msg1 = new ChatMemoryService.ChatMessage("user", "Hello");
        Object invalidObj = "not a message";
        List<Object> rawList = List.of(msg1, invalidObj);
        when(listOperations.range(key, 0, -1)).thenReturn(rawList);

        // When
        List<ChatMemoryService.ChatMessage> result = chatMemoryService.getHistory(userId, sessionId);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(msg1);
    }

    // ========== clearHistory Tests ==========

    @Test
    void clearHistory_shouldDeleteKey() {
        // When
        chatMemoryService.clearHistory(userId, sessionId);

        // Then
        verify(redisTemplate).delete(key);
    }

    // ========== Key Building ==========

    @Test
    void appendMessage_shouldUseCorrectKeyFormat() {
        // Given – we'll capture the key used in rightPush
        chatMemoryService.appendMessage(userId, sessionId, "user", "Hello");

        // Then
        String expectedKey = "chat:memory:" + userId + ":" + sessionId;
        verify(listOperations).rightPush(eq(expectedKey), any());
    }
}