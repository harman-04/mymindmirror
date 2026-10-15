package com.mymindmirror.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private UserDetails userDetails;
    private UUID userId;

    private static final String TEST_SECRET =
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long EXPIRATION_MS = 86400000L;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", EXPIRATION_MS);
        jwtUtil.init();

        userId = UUID.randomUUID();
        userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("testuser");
    }

    // ============================================================
    // Helpers
    // ============================================================

    private Object getKey() {
        return ReflectionTestUtils.getField(jwtUtil, "key");
    }


    @Test
    @DisplayName("generateToken() embeds the userId as a claim")
    void generateToken_ShouldIncludeUserIdClaim() {
        String token = jwtUtil.generateToken(userDetails, userId);

        Claims claims = Jwts.parserBuilder()
                .setSigningKey((java.security.Key) getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        assertThat(claims.get("userId", String.class)).isEqualTo(userId.toString());
    }

    @Test
    @DisplayName("generateToken() sets the subject to the username")
    void generateToken_ShouldHaveCorrectSubject() {
        String token = jwtUtil.generateToken(userDetails, userId);
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("testuser");
    }


    // ============================================================
    // extractUsername() and extractExpiration()
    // ============================================================

    @Test
    @DisplayName("extractUsername() returns the subject")
    void extractUsername_ShouldReturnSubject() {
        String token = jwtUtil.generateToken(userDetails, userId);
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("testuser");
    }

    // ============================================================
    // validateToken(String) – the defensive one
    // ============================================================

    @Test
    @DisplayName("validateToken(token) → true for a freshly generated token")
    void validateToken_WithValidToken_ShouldReturnTrue() {
        String token = jwtUtil.generateToken(userDetails, userId);
        assertThat(jwtUtil.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("validateToken(token) → false for an expired token")
    void validateToken_WithExpiredToken_ShouldReturnFalse() throws InterruptedException {
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", 100L);
        jwtUtil.init();
        String token = jwtUtil.generateToken(userDetails, userId);
        Thread.sleep(200);

        assertThat(jwtUtil.validateToken(token)).isFalse();

        // Restore for other tests
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", EXPIRATION_MS);
        jwtUtil.init();
    }

    @Test
    @DisplayName("validateToken(token) → false for a malformed token")
    void validateToken_WithMalformedToken_ShouldReturnFalse() {
        assertThat(jwtUtil.validateToken("malformed.token.here")).isFalse();
    }

    @Test
    @DisplayName("validateToken(token) → false for a token signed with a different key")
    void validateToken_WithWrongSignature_ShouldReturnFalse() {
        var wrongKey = Keys.hmacShaKeyFor("different-key-different-key-different-key".getBytes());
        String token = Jwts.builder()
                .setSubject("testuser")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(wrongKey)
                .compact();

        assertThat(jwtUtil.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("validateToken(token) → false for an empty string")
    void validateToken_WithEmptyToken_ShouldReturnFalse() {
        assertThat(jwtUtil.validateToken("")).isFalse();
    }

    @Test
    @DisplayName("validateToken(null) → false (no NPE)")
    void validateToken_WhenTokenIsNull_ShouldReturnFalse() {
        assertThat(jwtUtil.validateToken((String) null)).isFalse();
    }

    @Test
    @DisplayName("validateToken(token) → false for an unsigned token (alg: none)")
    void validateToken_WithUnsignedToken_ShouldReturnFalse() {
        // Header  : {"alg":"none"}
        // Payload : {"sub":"testuser"}
        // Signature: empty (trailing dot)
        // Modern JJWT rejects unsigned tokens as UnsupportedJwtException.
        String unsignedToken = "eyJhbGciOiJub25lIn0.eyJzdWIiOiJ0ZXN0dXNlciJ9.";

        assertThat(jwtUtil.validateToken(unsignedToken)).isFalse();
    }

    // ============================================================
    // validateToken(String, UserDetails) – the refactored method
    // ============================================================

    @Test
    @DisplayName("validateToken(token, userDetails) → true when username matches and token not expired")
    void validateTokenWithUserDetails_WhenUsernameMatchesAndNotExpired_ShouldReturnTrue() {
        String token = jwtUtil.generateToken(userDetails, userId);
        assertThat(jwtUtil.validateToken(token, userDetails)).isTrue();
    }

    @Test
    @DisplayName("validateToken(token, userDetails) → false when username does not match")
    void validateTokenWithUserDetails_WhenUsernameDoesNotMatch_ShouldReturnFalse() {
        String token = jwtUtil.generateToken(userDetails, userId);
        UserDetails differentUser = mock(UserDetails.class);
        when(differentUser.getUsername()).thenReturn("wronguser");

        assertThat(jwtUtil.validateToken(token, differentUser)).isFalse();
    }

    @Test
    @DisplayName("validateToken(token, userDetails) → false (NOT throw) for an expired token")
    void validateTokenWithUserDetails_WhenTokenExpired_ShouldReturnFalse() throws InterruptedException {
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", 100L);
        jwtUtil.init();
        String token = jwtUtil.generateToken(userDetails, userId);
        Thread.sleep(200);

        //  Refactored method catches ExpiredJwtException and returns false
        assertThat(jwtUtil.validateToken(token, userDetails)).isFalse();

        // Restore for other tests
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", EXPIRATION_MS);
        jwtUtil.init();
    }

    @Test
    @DisplayName("validateToken(token, userDetails) → false for a malformed token")
    void validateTokenWithUserDetails_WhenMalformed_ShouldReturnFalse() {
        assertThat(jwtUtil.validateToken("not.a.jwt", userDetails)).isFalse();
    }

    @Test
    @DisplayName("validateToken(token, userDetails) → false for a null token")
    void validateTokenWithUserDetails_WhenTokenNull_ShouldReturnFalse() {
        assertThat(jwtUtil.validateToken((String) null, userDetails)).isFalse();
    }

    @Test
    @DisplayName("validateToken(token, userDetails) → false for a token signed with a foreign key")
    void validateTokenWithUserDetails_WhenSignatureInvalid_ShouldReturnFalse() {
        var wrongKey = Keys.hmacShaKeyFor("different-key-different-key-different-key".getBytes());
        String foreignToken = Jwts.builder()
                .setSubject("testuser")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(wrongKey)
                .compact();

        assertThat(jwtUtil.validateToken(foreignToken, userDetails)).isFalse();
    }
}