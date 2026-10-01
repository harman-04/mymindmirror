package com.mymindmirror.backend.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class EncryptionUtilTest {

    private static final String USER_SECRET = "someLongPasswordHashThatIsAtLeast16BytesLongForSalt";
    private static final String PLAIN_TEXT = "Hello, this is a secret message!";

    // ============================================================
    // ENCRYPTION TESTS
    // ============================================================

    @Test
    @DisplayName("encrypt() should return Base64-encoded ciphertext for valid input")
    void encrypt_shouldReturnValidCiphertext() {
        String ciphertext = EncryptionUtil.encrypt(PLAIN_TEXT, USER_SECRET);

        assertThat(ciphertext).isNotNull().isNotBlank();
        // Must be valid Base64
        assertThat(Base64.getDecoder().decode(ciphertext)).isNotEmpty();
        // Must NOT equal the plaintext
        assertThat(ciphertext).isNotEqualTo(PLAIN_TEXT);
    }

    @Test
    @DisplayName("encrypt() should produce different ciphertexts for the same plaintext (random IV)")
    void encrypt_shouldUseRandomIv() {
        String cipher1 = EncryptionUtil.encrypt(PLAIN_TEXT, USER_SECRET);
        String cipher2 = EncryptionUtil.encrypt(PLAIN_TEXT, USER_SECRET);

        assertThat(cipher1).isNotEqualTo(cipher2);
    }

    @Test
    @DisplayName("encrypt() should handle null plaintext by treating it as an empty string")
    void encrypt_shouldHandleNullPlaintext() {
        String ciphertext = EncryptionUtil.encrypt(null, USER_SECRET);
        assertThat(ciphertext).isNotNull().isNotEmpty();

        String decrypted = EncryptionUtil.decrypt(ciphertext, USER_SECRET);
        assertThat(decrypted).isEmpty();
    }

    @Test
    @DisplayName("encrypt() should handle empty string")
    void encrypt_shouldHandleEmptyString() {
        String ciphertext = EncryptionUtil.encrypt("", USER_SECRET);
        assertThat(ciphertext).isNotNull().isNotEmpty();

        String decrypted = EncryptionUtil.decrypt(ciphertext, USER_SECRET);
        assertThat(decrypted).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("encrypt() should return null when userSecret is null or empty")
    void encrypt_shouldReturnNullForInvalidSecret(String secret) {
        String result = EncryptionUtil.encrypt(PLAIN_TEXT, secret);
        assertThat(result).isNull();
    }

    // ============================================================
    // DECRYPTION TESTS
    // ============================================================

    @Test
    @DisplayName("decrypt() should restore the original plaintext (round-trip)")
    void decrypt_shouldRestoreOriginalText() {
        String ciphertext = EncryptionUtil.encrypt(PLAIN_TEXT, USER_SECRET);
        String decrypted = EncryptionUtil.decrypt(ciphertext, USER_SECRET);

        assertThat(decrypted).isEqualTo(PLAIN_TEXT);
    }

    @Test
    @DisplayName("decrypt() should return input unchanged for null ciphertext")
    void decrypt_withNullCiphertext_shouldReturnNull() {
        String result = EncryptionUtil.decrypt(null, USER_SECRET);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("decrypt() should return input unchanged for empty string ciphertext")
    void decrypt_withEmptyCiphertext_shouldReturnEmpty() {
        String result = EncryptionUtil.decrypt("", USER_SECRET);
        assertThat(result).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("decrypt() should return ciphertext unchanged when secret is null or empty")
    void decrypt_shouldReturnOriginalCiphertext_whenSecretInvalid(String secret) {
        String ciphertext = EncryptionUtil.encrypt(PLAIN_TEXT, USER_SECRET);
        String result = EncryptionUtil.decrypt(ciphertext, secret);

        assertThat(result).isEqualTo(ciphertext);
    }

    @Test
    @DisplayName("decrypt() should return ciphertext unchanged if not Base64 (legacy plaintext)")
    void decrypt_shouldReturnLegacyPlaintext_whenNotBase64() {
        String legacyText = "This is not valid base64!";
        String result = EncryptionUtil.decrypt(legacyText, USER_SECRET);

        // Catches IllegalArgumentException internally
        assertThat(result).isEqualTo(legacyText);
    }

    @Test
    @DisplayName("decrypt() should return original string when data is too short to contain an IV")
    void decrypt_shouldHandleTooShortData() {
        // Valid Base64 but only 10 bytes – no room for 16-byte IV
        String shortData = Base64.getEncoder().encodeToString(new byte[10]);
        String result = EncryptionUtil.decrypt(shortData, USER_SECRET);

        assertThat(result).isEqualTo(shortData);
    }

    @Test
    @DisplayName("decrypt() should return original string when wrong secret is used")
    void decrypt_shouldFailGracefully_withWrongSecret() {
        String ciphertext = EncryptionUtil.encrypt(PLAIN_TEXT, USER_SECRET);
        String wrongSecret = "differentPasswordHashThatIsAlsoLong";

        String result = EncryptionUtil.decrypt(ciphertext, wrongSecret);

        // Catches BadPaddingException internally
        assertThat(result).isEqualTo(ciphertext);
    }

    @Test
    @DisplayName("decrypt() should return original when data is corrupted")
    void decrypt_shouldReturnOriginal_whenDataCorrupted() {
        String ciphertext = EncryptionUtil.encrypt(PLAIN_TEXT, USER_SECRET);

        // Corrupt the ciphertext by flipping a bit
        byte[] decoded = Base64.getDecoder().decode(ciphertext);
        decoded[decoded.length - 1] ^= 0xFF;
        String corrupted = Base64.getEncoder().encodeToString(decoded);

        String result = EncryptionUtil.decrypt(corrupted, USER_SECRET);

        assertThat(result).isEqualTo(corrupted);
    }

    // ============================================================
    // DATA INTEGRITY TESTS
    // ============================================================

    @Test
    @DisplayName("encrypt/decrypt should handle very long text")
    void encryptAndDecrypt_shouldWorkWithLongText() {
        String longText = "A".repeat(10_000);
        String encrypted = EncryptionUtil.encrypt(longText, USER_SECRET);
        String decrypted = EncryptionUtil.decrypt(encrypted, USER_SECRET);

        assertThat(decrypted).isEqualTo(longText);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Simple text",
            "Line 1\nLine 2\nLine 3",
            "Tabs\tand\tspaces",
            "Emoji: 😀 🎉 🚀",
            "Mixed: Hello नमस्ते مرحبا 你好 世界",
            "JSON: {\"key\": \"value\"}"
    })
    @DisplayName("round-trip encryption/decryption preserves all Unicode and special characters")
    void roundTrip_preservesText(String original) {
        String ciphertext = EncryptionUtil.encrypt(original, USER_SECRET);
        String decrypted = EncryptionUtil.decrypt(ciphertext, USER_SECRET);

        assertThat(decrypted).isEqualTo(original);
    }
}