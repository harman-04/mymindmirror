package com.mymindmirror.backend.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FieldEncryptionUtilTest {

    // 32-byte (256-bit) key, Base64-encoded
    private static final String VALID_MASTER_KEY =
            Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes());

    private FieldEncryptionUtil util;

    @BeforeEach
    void setUp() {
        util = new FieldEncryptionUtil(VALID_MASTER_KEY);
    }

    // ============================================================
    // Constructor
    // ============================================================

    @Test
    @DisplayName("constructor should accept a 32-byte Base64 key")
    void constructor_shouldAccept32ByteKey() {
        FieldEncryptionUtil instance = new FieldEncryptionUtil(VALID_MASTER_KEY);
        assertThat(instance).isNotNull();
    }

    @Test
    @DisplayName("constructor should reject a key shorter than 32 bytes")
    void constructor_shouldRejectShortKey() {
        String shortKey = Base64.getEncoder().encodeToString("short-key".getBytes());
        assertThatThrownBy(() -> new FieldEncryptionUtil(shortKey))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    @DisplayName("constructor should reject a key longer than 32 bytes")
    void constructor_shouldRejectLongKey() {
        String longKey = Base64.getEncoder().encodeToString("a".repeat(64).getBytes());
        assertThatThrownBy(() -> new FieldEncryptionUtil(longKey))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    @DisplayName("constructor should reject a non-Base64 key")
    void constructor_shouldRejectInvalidBase64() {
        assertThatThrownBy(() -> new FieldEncryptionUtil("!!!not-base64!!!"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ============================================================
    // encrypt()
    // ============================================================

    @Test
    @DisplayName("encrypt() should produce valid Base64 output")
    void encrypt_shouldReturnBase64Output() {
        String cipher = util.encrypt("hello world");
        assertThat(cipher).isNotNull().isNotEmpty();
        // Must be valid Base64
        assertThat(Base64.getDecoder().decode(cipher)).isNotEmpty();
    }

    @Test
    @DisplayName("encrypt() should produce different ciphertexts for same plaintext (random IV)")
    void encrypt_shouldUseRandomIv() {
        String c1 = util.encrypt("same input");
        String c2 = util.encrypt("same input");
        assertThat(c1).isNotEqualTo(c2);
    }

    @Test
    @DisplayName("encrypt() should return null when plaintext is null")
    void encrypt_shouldReturnNullForNullInput() {
        assertThat(util.encrypt(null)).isNull();
    }

    @Test
    @DisplayName("encrypt() should handle empty string")
    void encrypt_shouldHandleEmptyString() {
        String cipher = util.encrypt("");
        assertThat(cipher).isNotNull().isNotEmpty();
        assertThat(util.decrypt(cipher)).isEmpty();
    }

    @Test
    @DisplayName("encrypt() should handle unicode and emoji")
    void encrypt_shouldHandleUnicode() {
        String unicode = "नमस्ते दुनिया 🌍 ¡Hola!";
        String cipher = util.encrypt(unicode);
        assertThat(util.decrypt(cipher)).isEqualTo(unicode);
    }

    @Test
    @DisplayName("encrypt() should handle long text")
    void encrypt_shouldHandleLongText() {
        String longText = "A".repeat(10_000);
        String cipher = util.encrypt(longText);
        assertThat(util.decrypt(cipher)).isEqualTo(longText);
    }

    // ============================================================
    // decrypt()
    // ============================================================

    @Test
    @DisplayName("decrypt() should restore original plaintext (round-trip)")
    void decrypt_shouldRestoreOriginal() {
        String plaintext = "Secret API Key: sk-abcdef1234567890";
        String cipher = util.encrypt(plaintext);
        String decrypted = util.decrypt(cipher);

        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    @DisplayName("decrypt() should return null when input is null")
    void decrypt_shouldReturnNullForNullInput() {
        assertThat(util.decrypt(null)).isNull();
    }

    @Test
    @DisplayName("decrypt() should throw on invalid Base64 input")
    void decrypt_shouldThrowOnInvalidBase64() {
        assertThatThrownBy(() -> util.decrypt("not-valid-base64!@#"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to decrypt");
    }

    @Test
    @DisplayName("decrypt() should throw on data shorter than IV length (12 bytes)")
    void decrypt_shouldThrowOnTooShortData() {
        String tooShort = Base64.getEncoder().encodeToString(new byte[]{1, 2, 3});
        assertThatThrownBy(() -> util.decrypt(tooShort))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to decrypt");
    }

    @Test
    @DisplayName("decrypt() should throw on tampered ciphertext (GCM auth failure)")
    void decrypt_shouldThrowOnTamperedData() {
        String cipher = util.encrypt("original");
        byte[] decoded = Base64.getDecoder().decode(cipher);
        // Flip a bit in the ciphertext portion (not the IV)
        decoded[decoded.length - 1] ^= 0xFF;
        String tampered = Base64.getEncoder().encodeToString(decoded);

        assertThatThrownBy(() -> util.decrypt(tampered))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to decrypt");
    }

    @Test
    @DisplayName("decrypt() should throw when ciphertext is corrupted IV")
    void decrypt_shouldThrowOnCorruptedIv() {
        String cipher = util.encrypt("original");
        byte[] decoded = Base64.getDecoder().decode(cipher);
        // Flip a bit in the IV portion (first 12 bytes)
        decoded[0] ^= 0xFF;
        String tampered = Base64.getEncoder().encodeToString(decoded);

        assertThatThrownBy(() -> util.decrypt(tampered))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to decrypt");
    }

    // ============================================================
    // Cross-instance compatibility
    // ============================================================

    @Test
    @DisplayName("Two instances with same key should decrypt each other's ciphertext")
    void twoInstances_shouldBeInteroperable() {
        FieldEncryptionUtil other = new FieldEncryptionUtil(VALID_MASTER_KEY);
        String cipher = util.encrypt("shared secret");
        assertThat(other.decrypt(cipher)).isEqualTo("shared secret");
    }

    @Test
    @DisplayName("Different keys should NOT decrypt each other's ciphertext")
    void differentKeys_shouldNotDecrypt() {
        String otherKey = Base64.getEncoder().encodeToString("abcdef0123456789abcdef0123456789".getBytes());
        FieldEncryptionUtil other = new FieldEncryptionUtil(otherKey);

        String cipher = util.encrypt("secret");
        assertThatThrownBy(() -> other.decrypt(cipher))
                .isInstanceOf(RuntimeException.class);
    }

    // ============================================================
    // Round-trip: various input types
    // ============================================================

    @ParameterizedTest
    @ValueSource(strings = {
            "Simple",
            "Line 1\nLine 2",
            "Tab\tseparated\tvalues",
            "JSON: {\"key\":\"value\"}",
            "Special chars: !@#$%^&*()",
            "Emoji: 🚀🎉😀",
            "Unicode: مرحبا 你好 नमस्ते"
    })
    @DisplayName("round-trip preserves various input types")
    void roundTrip_preservesText(String original) {
        String cipher = util.encrypt(original);
        assertThat(util.decrypt(cipher)).isEqualTo(original);
    }
}