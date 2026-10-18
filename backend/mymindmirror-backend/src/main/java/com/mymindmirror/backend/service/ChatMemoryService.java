package com.mymindmirror.backend.service;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMemoryService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String KEY_PREFIX = "chat:memory:";
    private static final int MAX_HISTORY_SIZE = 10; // Holds the last 5 User + 5 AI responses
    private static final long SESSION_TTL_HOURS = 1;


    public record ChatMessage(String role, String content) implements Serializable {}


    private String buildKey(UUID userId, String sessionId) {
        return KEY_PREFIX + userId.toString() + ":" + sessionId;
    }

    /**
     * Appends a message to the sliding history log in Redis and resets the 1-hour idle timer.
     */
    public void appendMessage(UUID userId, String sessionId, String role, String content) {
        String key = buildKey(userId, sessionId);
        ChatMessage message = new ChatMessage(role, content);

        // Push to the right side of the list
        redisTemplate.opsForList().rightPush(key, message);

        // Trim left side if history exceeds our rolling cap threshold
        Long size = redisTemplate.opsForList().size(key);
        if (size != null && size > MAX_HISTORY_SIZE) {
            redisTemplate.opsForList().leftPop(key);
        }

        // Reset the inactivity lifecycle clock
        redisTemplate.expire(key, SESSION_TTL_HOURS, TimeUnit.HOURS);
    }

    /**
     * Pulls the contextual timeline history segment out of Redis.
     */
    @SuppressWarnings("unchecked")
    public List<ChatMessage> getHistory(UUID userId, String sessionId) {
        String key = buildKey(userId, sessionId);
        List<Object> rawList = redisTemplate.opsForList().range(key, 0, -1);

        List<ChatMessage> messages = new ArrayList<>();
        if (rawList != null) {
            for (Object obj : rawList) {
                if (obj instanceof ChatMessage) {
                    messages.add((ChatMessage) obj);
                }
            }
        }
        return messages;
    }

    /**
     * Instantly deletes the history partition cache.
     */
    public void clearHistory(UUID userId, String sessionId) {
        String key = buildKey(userId, sessionId);
        redisTemplate.delete(key);
    }

    /**
     * Deletes ALL chat sessions for a specific user using SCAN + UNLINK.
     * This is a non-blocking alternative to redisTemplate.keys(), which is O(N)
     * and can stall the Redis server[reference:7][reference:8].
     */
    public void clearAllUserSessions(UUID userId) {
        String pattern = KEY_PREFIX + userId.toString() + ":*";

        redisTemplate.execute((RedisConnection connection) -> {
            ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();

            int deletedCount = 0;

            // Use try-with-resources to guarantee the cursor is closed (releasing the Redis connection)
            try (Cursor<byte[]> cursor = connection.scan(options)) {
                while (cursor.hasNext()) {
                    // Use UNLINK for non-blocking deletion
                    connection.unlink(cursor.next());
                    deletedCount++;
                }
            }

            log.info("Deleted {} chat sessions for user {}", deletedCount, userId);
            return null;
        });
    }
}