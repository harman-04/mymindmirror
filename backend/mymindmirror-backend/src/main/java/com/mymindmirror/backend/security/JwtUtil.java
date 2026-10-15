package com.mymindmirror.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class JwtUtil {


    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration.ms}")
    private long expirationMs; // in milliseconds

    private Key key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        log.info("JWT Secret Key initialized.");
    }

    /**
     * Generates a JWT token for the given UserDetails and includes the user's UUID as a claim.
     * @param userDetails The Spring Security UserDetails object.
     * @param userId The UUID of the user.
     * @return The generated JWT string.
     */
    public String generateToken(UserDetails userDetails, UUID userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId.toString()); // Add userId as a string claim
        return createToken(claims, userDetails.getUsername());
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Validates a JWT token against UserDetails.
     * Returns false (instead of throwing) if the token is expired (rejected by the
     * JJWT parser), malformed, or otherwise invalid – so callers never need to
     * wrap this in try/catch.
     *
     * @param token The JWT token string.
     * @param userDetails The UserDetails object to validate against.
     * @return True if the token is valid for the user and not expired, false otherwise.
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return username.equals(userDetails.getUsername());
        } catch (Exception e) {
            log.warn("JWT validation failed for user {}: {}", userDetails.getUsername(), e.getMessage());
            return false;
        }
    }

    /**
     * Extracts the username (subject) from the JWT token.
     * @param token The JWT token string.
     * @return The username.
     */
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }

    /**
     * Validates a JWT token's signature and expiration.
     * This is a simpler validation for filters, without needing UserDetails.
     * @param authToken The JWT token string.
     * @return True if the token is valid (signature and not expired), false otherwise.
     */
    public boolean validateToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(authToken);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | io.jsonwebtoken.MalformedJwtException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (io.jsonwebtoken.UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (java.lang.IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }


    // ---- Unused (kept as reference for potential future use) ----

    /**
     * Extracts the user's UUID from the JWT token claims.
     * @param token The JWT token string.
     * @return The user's UUID.
     * @throws IllegalArgumentException if userId claim is missing or not a valid UUID.
     */
// public UUID extractUserId(String token) {
//     String userIdStr = extractAllClaims(token).get("userId", String.class);
//     if (userIdStr == null) {
//         log.error("JWT token missing 'userId' claim.");
//         throw new IllegalArgumentException("JWT token is missing 'userId' claim.");
//     }
//     try {
//         return UUID.fromString(userIdStr);
//     } catch (IllegalArgumentException e) {
//         log.error("Invalid UUID format for 'userId' claim in JWT: {}", userIdStr);
//         throw new IllegalArgumentException("Invalid UUID format in JWT 'userId' claim.", e);
//     }
// }

    /**
     * Extracts the expiration date from the JWT token.
     * @param token The JWT token string.
     * @return The expiration Date.
     */
// public Date extractExpiration(String token) {
//     return extractAllClaims(token).getExpiration();
// }
}
