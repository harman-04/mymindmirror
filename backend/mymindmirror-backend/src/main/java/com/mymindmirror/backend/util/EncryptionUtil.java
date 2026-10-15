package com.mymindmirror.backend.util;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Arrays; // Import Arrays for Arrays.copyOf

/**
 * Utility class for encrypting and decrypting journal entry raw text.
 * Uses AES/CBC/PKCS5Padding for encryption with a key derived from the user's password hash
 * and a randomly generated IV for each encryption.
 */
@Slf4j
public class EncryptionUtil {


    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding"; // Changed from ECB to CBC
    private static final int KEY_LENGTH = 128; // 128-bit AES key
    private static final int ITERATION_COUNT = 65536; // Iterations for PBKDF2 (higher is better, but impacts performance)
    private static final int SALT_LENGTH = 16; // 16 bytes for salt (for PBKDF2)
    private static final int IV_LENGTH = 16; // 16 bytes for IV (AES block size)

    // Private constructor to prevent instantiation
    private EncryptionUtil() {}

    /**
     * Derives a secure AES key from the user's password hash using PBKDF2.
     * The password hash itself is not used directly as the key, but as input to the KDF.
     *
     * @param passwordHash The user's password hash (acting as the "password" for KDF).
     * @param salt The salt to use for key derivation. Should be unique per user.
     * @return A SecretKeySpec suitable for AES encryption.
     * @throws NoSuchAlgorithmException If PBKDF2WithHmacSHA256 is not available.
     * @throws InvalidKeySpecException If the key specification is invalid.
     */
    private static SecretKeySpec deriveKey(String passwordHash, byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(passwordHash.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }

    /**
     * Encrypts the given plaintext using AES/CBC/PKCS5Padding.
     * The IV is randomly generated for each encryption and prepended to the ciphertext.
     * The salt for key derivation is derived from the user's password hash (first 16 bytes).
     *
     * @param plaintext The text to encrypt.
     * @param userSecret The user's password hash, used as the base for key derivation.
     * @return Base64 encoded string of IV + ciphertext, or null if encryption fails due to critical error.
     * Returns an encrypted empty string if plaintext is empty.
     */
    public static String encrypt(String plaintext, String userSecret) {
        // Ensure plaintext is never null; treat null as empty string for encryption
        String actualPlaintext = (plaintext != null) ? plaintext : "";

        if (userSecret == null || userSecret.isEmpty()) {
            log.error("Encryption failed: userSecret is null or empty. Cannot encrypt.");
            return null; // Critical error: cannot encrypt without a secret
        }

        try {
            // Use a portion of the userSecret as a consistent salt for key derivation
            // This assumes userSecret (password hash) is long enough (e.g., from BCrypt, it's 60 chars)
//            byte[] salt = Arrays.copyOf(userSecret.getBytes(), SALT_LENGTH);
            byte[] salt = Arrays.copyOf(userSecret.getBytes(StandardCharsets.UTF_8), SALT_LENGTH);

            SecretKeySpec secretKey = deriveKey(userSecret, salt);

            SecureRandom secureRandom = new SecureRandom();
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv); // Generate a random IV for each encryption
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

//            byte[] encryptedBytes = cipher.doFinal(actualPlaintext.getBytes());
            byte[] encryptedBytes = cipher.doFinal(actualPlaintext.getBytes(StandardCharsets.UTF_8));

            // Prepend IV to ciphertext for storage
            byte[] combined = new byte[iv.length + encryptedBytes.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encryptedBytes, 0, combined, iv.length, encryptedBytes.length);

            return Base64.getEncoder().encodeToString(combined);

        } catch (Exception e) {
            log.error("Encryption failed for plaintext (first 20 chars): '{}'. Error: {}",
                    actualPlaintext.length() > 20 ? actualPlaintext.substring(0, 20) + "..." : actualPlaintext,
                    e.getMessage(), e);
            return null; // Return null on encryption failure
        }
    }

    /**
     * Decrypts the given ciphertext using AES/CBC/PKCS5Padding.
     * The IV is extracted from the beginning of the ciphertext.
     * The salt for key derivation is derived from the user's password hash (first 16 bytes).
     *
     * @param ciphertextBase64 The Base64 encoded string of IV + ciphertext.
     * @param userSecret The user's password hash, used as the base for key derivation.
     * @return The decrypted plaintext, or the original ciphertext if decryption fails (to prevent data loss).
     */
    public static String decrypt(String ciphertextBase64, String userSecret) {
        if (ciphertextBase64 == null || ciphertextBase64.isEmpty()) {
            return ciphertextBase64; // Nothing to decrypt, return as is.
        }
        if (userSecret == null || userSecret.isEmpty()) {
            log.error("Decryption failed: userSecret is null or empty. Cannot decrypt.");
            return ciphertextBase64; // Cannot decrypt without a secret
        }

        try {
            byte[] combined = Base64.getDecoder().decode(ciphertextBase64);

            if (combined.length < IV_LENGTH) {
                log.warn("Decryption failed: Combined data too short to contain IV. Returning original string. Data (first 20 chars): {}",
                        ciphertextBase64.length() > 20 ? ciphertextBase64.substring(0, 20) + "..." : ciphertextBase64);
                return ciphertextBase64; // Data is too short to be valid encrypted data with IV
            }

            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            byte[] encryptedBytes = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, IV_LENGTH, encryptedBytes, 0, encryptedBytes.length);

//            byte[] salt = Arrays.copyOf(userSecret.getBytes(), SALT_LENGTH);
            byte[] salt = Arrays.copyOf(userSecret.getBytes(StandardCharsets.UTF_8), SALT_LENGTH);
            SecretKeySpec secretKey = deriveKey(userSecret, salt);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
//            return new String(decryptedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);

        } catch (IllegalArgumentException e) {
            // This catches "Illegal base64 character" errors.
            // Occurs when trying to decode a string that is not valid Base64 (e.g., old plain text entries).
            log.warn("IllegalArgumentException (invalid Base64) during decryption. Returning original string. Error: {}. Data (first 20 chars): {}",
                    e.getMessage(), ciphertextBase64.length() > 20 ? ciphertextBase64.substring(0, 20) + "..." : ciphertextBase64);
            return ciphertextBase64;
        } catch (javax.crypto.BadPaddingException | javax.crypto.IllegalBlockSizeException e) {
            // These catch errors related to incorrect padding or block size, often indicating:
            // 1. The data was not encrypted with the expected padding/mode.
            // 2. The data is corrupted.
            // 3. The wrong key/IV was used.
            log.warn("BadPaddingException or IllegalBlockSizeException during decryption. Likely unencrypted, corrupted data, or wrong key/IV. Returning original string. Error: {}. Data (first 20 chars): {}",
                    e.getMessage(), ciphertextBase64.length() > 20 ? ciphertextBase64.substring(0, 20) + "..." : ciphertextBase64);
            return ciphertextBase64;
        } catch (Exception e) {
            // Catch any other unexpected exceptions during the decryption process.
            log.error("Generic error during decryption for ciphertext (first 20 chars): '{}'. Error: {}",
                    ciphertextBase64.length() > 20 ? ciphertextBase64.substring(0, 20) + "..." : ciphertextBase64,
                    e.getMessage(), e);
            return ciphertextBase64; // Fallback: return original string if any other error occurs
        }
    }
}
