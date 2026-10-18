package com.mymindmirror.backend.controller;

import com.mymindmirror.backend.annotation.CurrentUser;
import com.mymindmirror.backend.api.ApiKeyApi;
import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.payload.request.ApiKeyRequest;
import com.mymindmirror.backend.payload.response.ApiKeySaveResponse;
import com.mymindmirror.backend.payload.response.ApiKeyStatusResponse;
import com.mymindmirror.backend.service.ApiKeyService;
import com.mymindmirror.backend.service.ai.DynamicAiClientService;
import jakarta.validation.Valid;
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
    public ResponseEntity<ApiKeySaveResponse> updateApiKey(
            @CurrentUser User currentUser,
            @Valid @RequestBody ApiKeyRequest request) {

        ApiKeySaveResponse response =
                apiKeyService.validateAndSaveApiKey(currentUser, request.apiKey());

        // Only evict cached models if the stored key actually changed.
        // A REJECTED outcome leaves the existing key untouched, so the
        // cached model instances are still valid.
        if (!"REJECTED".equals(response.status())) {
            dynamicAiClientService.evictUserModel(currentUser.getId());
        }

        log.info("API key update for user {}: {}",
                currentUser.getUsername(), response.status());

        return ResponseEntity.ok(response);
    }

    @Override
    @GetMapping("/api-key/status")
    public ResponseEntity<ApiKeyStatusResponse> getApiKeyStatus(@CurrentUser User currentUser) {
        ApiKeyStatusResponse response = apiKeyService.getApiKeyStatus(currentUser);
        return ResponseEntity.ok(response);
    }
}