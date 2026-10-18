package com.mymindmirror.backend.service;

import com.mymindmirror.backend.constants.CacheConstants;
import com.mymindmirror.backend.enums.ValidationStatus;
import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.payload.response.ApiKeySaveResponse;
import com.mymindmirror.backend.payload.response.ApiKeyStatusResponse;
import com.mymindmirror.backend.payload.response.ValidationResult;
import com.mymindmirror.backend.util.FieldEncryptionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final FieldEncryptionUtil encryptionUtil;
    private final UserService userService;
    private final ApiKeyValidator apiKeyValidator;

    public String getDecryptedApiKey(User user) {
        if (user.getGeminiApiKeyEncrypted() == null) {
            return null;
        }
        return encryptionUtil.decrypt(user.getGeminiApiKeyEncrypted());
    }

    public void saveApiKey(User user, String plainApiKey) {
        if (plainApiKey == null || plainApiKey.isBlank()) {
            user.setGeminiApiKeyEncrypted(null);
        } else {
            user.setGeminiApiKeyEncrypted(encryptionUtil.encrypt(plainApiKey));
        }
        userService.save(user);
    }

    /**
     * Validates the key with Google, then saves it. See {@link ApiKeyValidator}
     * for the semantics of each status.
     *
     * <p>The key is saved for VERIFIED and UNVERIFIED outcomes. It is NOT saved
     * for REJECTED — a definitively bad key must never overwrite the user's
     * existing stored key.
     */
    @Caching(evict = {
            @CacheEvict(value = CacheConstants.API_KEY_STATUS, key = "#user.id"),
            @CacheEvict(value = CacheConstants.USER_FULL_PROFILE, key = "#user.id")
    })
    public ApiKeySaveResponse validateAndSaveApiKey(User user, String plainApiKey) {
        if (plainApiKey == null || plainApiKey.isBlank()) {
            saveApiKey(user, null);
            return ApiKeySaveResponse.cleared();
        }

        ValidationResult result = apiKeyValidator.validate(plainApiKey);

        if (result.status() == ValidationStatus.REJECTED) {
            return ApiKeySaveResponse.rejected();
        }

        saveApiKey(user, plainApiKey);

        return result.status() == ValidationStatus.VERIFIED
                ? ApiKeySaveResponse.verified()
                : ApiKeySaveResponse.unverified();
    }

    @Cacheable(value = CacheConstants.API_KEY_STATUS, key = "#user.id")
    public ApiKeyStatusResponse getApiKeyStatus(User user) {
        String decrypted = getDecryptedApiKey(user);
        boolean usingOwnKey = decrypted != null && !decrypted.isBlank();
        String masked = null;
        if (usingOwnKey) {
            int len = decrypted.length();
            masked = "••••••••" + decrypted.substring(Math.max(0, len - 4));
        }
        String message = usingOwnKey
                ? "Using your own Gemini API key."
                : "Using shared API key. For better privacy, add your own key.";
        return new ApiKeyStatusResponse(usingOwnKey, masked, message);
    }
}