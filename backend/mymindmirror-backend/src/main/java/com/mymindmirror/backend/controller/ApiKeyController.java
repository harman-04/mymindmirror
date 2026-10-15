package com.mymindmirror.backend.controller;

import com.mymindmirror.backend.annotation.CurrentUser;
import com.mymindmirror.backend.api.ApiKeyApi;
import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.payload.request.ApiKeyRequest;
import com.mymindmirror.backend.payload.response.ApiKeyStatusResponse;
import com.mymindmirror.backend.service.ApiKeyService;
import com.mymindmirror.backend.service.ai.DynamicAiClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class ApiKeyController implements ApiKeyApi {

    private final ApiKeyService apiKeyService;
    private final DynamicAiClientService dynamicAiClientService;

    @Override
    @PutMapping("/api-key")
    public ResponseEntity<Void> updateApiKey(
            @CurrentUser User currentUser,
            @RequestBody ApiKeyRequest request) {

        String newKey = request.apiKey();

        if (newKey == null || newKey.isBlank()) {
            apiKeyService.saveApiKey(currentUser, null);
            log.info("Cleared API key for user {}", currentUser.getUsername());
        } else {
            apiKeyService.saveApiKey(currentUser, newKey);
            log.info("Updated API key for user {}", currentUser.getUsername());
        }

        //  Controller safely orchestrates the cache clear!
        dynamicAiClientService.evictUserModel(currentUser.getId());

        return ResponseEntity.noContent().build();
    }

    @Override
    @GetMapping("/api-key/status")
    public ResponseEntity<ApiKeyStatusResponse> getApiKeyStatus(@CurrentUser User currentUser) {
        ApiKeyStatusResponse response = apiKeyService.getApiKeyStatus(currentUser);
        return ResponseEntity.ok(response);
    }
}