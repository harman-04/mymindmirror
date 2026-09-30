package com.mymindmirror.backend.controller;

import com.mymindmirror.backend.annotation.CurrentUser;
import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.payload.request.ChatRequest;
import com.mymindmirror.backend.payload.response.ChatResponse;
import com.mymindmirror.backend.service.ChatMemoryService;
import com.mymindmirror.backend.service.JournalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ReflectionChatController {

    private final JournalService journalService;
    private final ChatMemoryService chatMemoryService;

    @PostMapping("/reflect")
    public ResponseEntity<ChatResponse> reflect(
            @CurrentUser User currentUser,
            @Valid @RequestBody ChatRequest request) {

        String answer = journalService.generateReflectionChat(
                currentUser,
                request.query(),
                request.sessionId(),
                request.rememberChat()
        );

        return ResponseEntity.ok(new ChatResponse(answer));
    }

    @PostMapping("/suggest-question")
    public ResponseEntity<ChatResponse> suggestQuestion(@CurrentUser User currentUser) {
        String question = journalService.generateReflectiveQuestion(currentUser);
        return ResponseEntity.ok(new ChatResponse(question));
    }

    @DeleteMapping("/clear-memory")
    public ResponseEntity<Void> clearMemory(
            @CurrentUser User currentUser,
            @RequestParam String sessionId) {

        if (!sessionId.isBlank()) {
            chatMemoryService.clearHistory(currentUser.getId(), sessionId);
        }
        return ResponseEntity.ok().build();
    }
}