package com.mymindmirror.backend.service;

import com.mymindmirror.backend.enums.ValidationStatus;
import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.payload.response.ApiKeySaveResponse;
import com.mymindmirror.backend.payload.response.ApiKeyStatusResponse;
import com.mymindmirror.backend.payload.response.ValidationResult;
import com.mymindmirror.backend.util.FieldEncryptionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiKeyServiceTest {

    @Mock
    private FieldEncryptionUtil encryptionUtil;

    @Mock
    private UserService userService;


    @Mock
    private ApiKeyValidator apiKeyValidator;

    @InjectMocks
    private ApiKeyService apiKeyService;

    private User user;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        user.setUsername("testuser");
    }

    // ========== getDecryptedApiKey Tests ==========

    @Test
    void getDecryptedApiKey_shouldReturnNull_whenEncryptedKeyIsNull() {
        // Given – encrypted key is null
        user.setGeminiApiKeyEncrypted(null);

        // When
        String result = apiKeyService.getDecryptedApiKey(user);

        // Then
        assertThat(result).isNull();
        verify(encryptionUtil, never()).decrypt(anyString());
    }

    @Test
    void getDecryptedApiKey_shouldDecryptAndReturn_whenEncryptedKeyExists() {
        // Given
        String encrypted = "encryptedKey";
        String plain = "plainKey";
        user.setGeminiApiKeyEncrypted(encrypted);
        when(encryptionUtil.decrypt(encrypted)).thenReturn(plain);

        // When
        String result = apiKeyService.getDecryptedApiKey(user);

        // Then
        assertThat(result).isEqualTo(plain);
        verify(encryptionUtil).decrypt(encrypted);
    }

    // ========== saveApiKey Tests ==========

    @Test
    void saveApiKey_shouldEncryptAndSave_whenPlainKeyProvided() {
        // Given
        String plainKey = "my-api-key";
        String encrypted = "encrypted-api-key";
        when(encryptionUtil.encrypt(plainKey)).thenReturn(encrypted);

        // When
        apiKeyService.saveApiKey(user, plainKey);

        // Then
        assertThat(user.getGeminiApiKeyEncrypted()).isEqualTo(encrypted);
        verify(userService).save(user);
        verify(encryptionUtil).encrypt(plainKey);
    }

    @Test
    void saveApiKey_shouldSetEncryptedToNull_whenPlainKeyIsNull() {
        // When
        apiKeyService.saveApiKey(user, null);

        // Then
        assertThat(user.getGeminiApiKeyEncrypted()).isNull();
        verify(userService).save(user);
        verify(encryptionUtil, never()).encrypt(anyString());
    }

    @Test
    void saveApiKey_shouldSetEncryptedToNull_whenPlainKeyIsBlank() {
        // When
        apiKeyService.saveApiKey(user, "   ");

        // Then
        assertThat(user.getGeminiApiKeyEncrypted()).isNull();
        verify(userService).save(user);
        verify(encryptionUtil, never()).encrypt(anyString());
    }

    // ========== getApiKeyStatus Tests ==========

    @Test
    void getApiKeyStatus_shouldReturnUsingOwnKeyFalse_whenNoKey() {
        // Given – no encrypted key
        user.setGeminiApiKeyEncrypted(null);

        // When
        ApiKeyStatusResponse response = apiKeyService.getApiKeyStatus(user);

        // Then
        assertThat(response.usingOwnKey()).isFalse();
        assertThat(response.maskedKey()).isNull();
        assertThat(response.message()).isEqualTo("Using shared API key. For better privacy, add your own key.");
    }

    @Test
    void getApiKeyStatus_shouldReturnMaskedKey_whenKeyExists() {
        // Given
        String plainKey = "abcdefghijklmnopqrstuvwxyz";
        when(encryptionUtil.decrypt(anyString())).thenReturn(plainKey);
        user.setGeminiApiKeyEncrypted("encrypted");

        // When
        ApiKeyStatusResponse response = apiKeyService.getApiKeyStatus(user);

        // Then
        assertThat(response.usingOwnKey()).isTrue();
        assertThat(response.maskedKey()).isEqualTo("••••••••wxyz");
        assertThat(response.message()).isEqualTo("Using your own Gemini API key.");
    }

    @Test
    void getApiKeyStatus_shouldMaskCorrectlyForShortKey() {
        // Given – short key (less than 4 chars)
        String plainKey = "abc";
        when(encryptionUtil.decrypt(anyString())).thenReturn(plainKey);
        user.setGeminiApiKeyEncrypted("encrypted");

        // When
        ApiKeyStatusResponse response = apiKeyService.getApiKeyStatus(user);

        // Then – should show all asterisks? Actually the code does substring(0, len-4) which for len=3 gives Math.max(0, -1) = 0, so it takes from 0 to -1? Let's check:
        // masked = "••••••••" + decrypted.substring(Math.max(0, len - 4));
        // For len=3, len-4 = -1, Math.max(0,-1) = 0, so substring(0,3) = "abc", so masked = "••••••••abc". That's what it does. So we assert that.
        assertThat(response.maskedKey()).isEqualTo("••••••••abc");
        assertThat(response.usingOwnKey()).isTrue();
    }

    // ========== Additional: getApiKeyStatus with null decrypted (should treat as not own) ==========

    @Test
    void getApiKeyStatus_shouldReturnUsingOwnKeyFalse_whenDecryptedIsNull() {
        // Given – encrypted exists but decryption returns null
        user.setGeminiApiKeyEncrypted("encrypted");
        when(encryptionUtil.decrypt(anyString())).thenReturn(null);

        // When
        ApiKeyStatusResponse response = apiKeyService.getApiKeyStatus(user);

        // Then
        assertThat(response.usingOwnKey()).isFalse();
        assertThat(response.maskedKey()).isNull();
        assertThat(response.message()).isEqualTo("Using shared API key. For better privacy, add your own key.");
    }

    @Test
    void getApiKeyStatus_shouldReturnUsingOwnKeyFalse_whenDecryptedIsBlank() {
        // Given – decrypted is blank
        user.setGeminiApiKeyEncrypted("encrypted");
        when(encryptionUtil.decrypt(anyString())).thenReturn("   ");

        // When
        ApiKeyStatusResponse response = apiKeyService.getApiKeyStatus(user);

        // Then
        assertThat(response.usingOwnKey()).isFalse();
        assertThat(response.maskedKey()).isNull();
        assertThat(response.message()).isEqualTo("Using shared API key. For better privacy, add your own key.");
    }

    // ========== validateAndSaveApiKey Tests ==========

    @Test
    void validateAndSaveApiKey_shouldSaveAndReturnVerified_whenValidatorReturnsVerified() {
        when(apiKeyValidator.validate("good-key"))
                .thenReturn(new ValidationResult(ValidationStatus.VERIFIED));
        when(encryptionUtil.encrypt("good-key")).thenReturn("encrypted");

        ApiKeySaveResponse response = apiKeyService.validateAndSaveApiKey(user, "good-key");

        assertThat(response.status()).isEqualTo("VERIFIED");
        assertThat(user.getGeminiApiKeyEncrypted()).isEqualTo("encrypted");
        verify(userService).save(user);
    }

    @Test
    void validateAndSaveApiKey_shouldNotSaveAndReturnRejected_whenValidatorRejects() {
        when(apiKeyValidator.validate("bad-key"))
                .thenReturn(new ValidationResult(ValidationStatus.REJECTED));

        ApiKeySaveResponse response = apiKeyService.validateAndSaveApiKey(user, "bad-key");

        assertThat(response.status()).isEqualTo("REJECTED");
        assertThat(user.getGeminiApiKeyEncrypted()).isNull();
        verify(userService, never()).save(any());
        verify(encryptionUtil, never()).encrypt(anyString());
    }

    @Test
    void validateAndSaveApiKey_shouldSaveAndReturnUnverified_whenValidatorCannotVerify() {
        when(apiKeyValidator.validate("maybe-key"))
                .thenReturn(new ValidationResult(ValidationStatus.UNVERIFIED));
        when(encryptionUtil.encrypt("maybe-key")).thenReturn("encrypted");

        ApiKeySaveResponse response = apiKeyService.validateAndSaveApiKey(user, "maybe-key");

        assertThat(response.status()).isEqualTo("UNVERIFIED");
        assertThat(user.getGeminiApiKeyEncrypted()).isEqualTo("encrypted");
        verify(userService).save(user);
    }

    @Test
    void validateAndSaveApiKey_shouldClearAndReturnCleared_whenKeyIsNull() {
        ApiKeySaveResponse response = apiKeyService.validateAndSaveApiKey(user, null);

        assertThat(response.status()).isEqualTo("CLEARED");
        assertThat(user.getGeminiApiKeyEncrypted()).isNull();
        verify(userService).save(user);
        verify(apiKeyValidator, never()).validate(anyString());
    }

    @Test
    void validateAndSaveApiKey_shouldClearAndReturnCleared_whenKeyIsBlank() {
        ApiKeySaveResponse response = apiKeyService.validateAndSaveApiKey(user, "   ");

        assertThat(response.status()).isEqualTo("CLEARED");
        assertThat(user.getGeminiApiKeyEncrypted()).isNull();
        verify(userService).save(user);
        verify(apiKeyValidator, never()).validate(anyString());
    }

    @Test
    void validateAndSaveApiKey_shouldPreserveExistingKey_whenNewKeyRejected() {
        user.setGeminiApiKeyEncrypted("existing-encrypted-key");
        when(apiKeyValidator.validate("bad-key"))
                .thenReturn(new ValidationResult(
                       ValidationStatus.REJECTED));

        ApiKeySaveResponse response = apiKeyService.validateAndSaveApiKey(user, "bad-key");

        assertThat(response.status()).isEqualTo("REJECTED");
        assertThat(user.getGeminiApiKeyEncrypted()).isEqualTo("existing-encrypted-key");
        verify(userService, never()).save(any());
        verify(encryptionUtil, never()).encrypt(anyString());
    }
}