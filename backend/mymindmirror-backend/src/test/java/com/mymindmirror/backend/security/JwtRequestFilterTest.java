package com.mymindmirror.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mymindmirror.backend.security.services.UserDetailsImpl;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtRequestFilterTest {

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private FilterChain filterChain;


    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private JwtRequestFilter jwtRequestFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private UserDetails buildUserDetails(String username) {
        return new UserDetailsImpl(
                UUID.randomUUID(),
                username,
                username + "@example.com",
                "hashed",
                List.of()
        );
    }

    // ============================================================
    // Case 1: No Authorization header
    // ============================================================

    @Test
    @DisplayName("No Authorization header → chain proceeds, no authentication set")
    void noAuthHeader_shouldSkipAuthentication() throws Exception {
        jwtRequestFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtUtil, userDetailsService);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    // ============================================================
    // Case 2: Authorization header without "Bearer " prefix
    // ============================================================

    @Test
    @DisplayName("Authorization header without Bearer prefix → skip auth, chain proceeds")
    void nonBearerHeader_shouldSkipAuthentication() throws Exception {
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        jwtRequestFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtUtil, userDetailsService);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Empty Bearer token → no extraction attempted")
    void emptyBearerToken_shouldSkipAuthentication() throws Exception {
        request.addHeader("Authorization", "Bearer ");

        // extractUsername("") is called – mock it to throw
        when(jwtUtil.extractUsername("")).thenThrow(new IllegalArgumentException("empty token"));

        jwtRequestFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(userDetailsService);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    // ============================================================
    // Case 3: Happy path – valid token + valid user
    // ============================================================

    @Test
    @DisplayName("Valid Bearer token + valid user → authentication set, chain proceeds")
    void validToken_shouldSetAuthentication() throws Exception {
        request.addHeader("Authorization", "Bearer valid.jwt.token");
        UserDetails userDetails = buildUserDetails("testuser");

        when(jwtUtil.extractUsername("valid.jwt.token")).thenReturn("testuser");
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
        when(jwtUtil.validateToken("valid.jwt.token", userDetails)).thenReturn(true);

        jwtRequestFilter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo("testuser");
        assertThat(auth.getPrincipal()).isSameAs(userDetails);
        assertThat(auth.getAuthorities()).isEqualTo(userDetails.getAuthorities());
        assertThat(auth).isInstanceOf(UsernamePasswordAuthenticationToken.class);
        assertThat(auth.getDetails()).isNotNull();  // WebAuthenticationDetails set

        verify(filterChain).doFilter(request, response);
    }

    // ============================================================
    // Case 4: Token extraction fails (malformed / expired)
    // ============================================================

    @Test
    @DisplayName("Malformed token → extractUsername throws → no auth, chain proceeds")
    void malformedToken_shouldNotAuthenticate() throws Exception {
        request.addHeader("Authorization", "Bearer not.a.jwt");
        when(jwtUtil.extractUsername("not.a.jwt"))
                .thenThrow(new IllegalArgumentException("Malformed JWT"));

        jwtRequestFilter.doFilter(request, response, filterChain);

        verify(jwtUtil).extractUsername("not.a.jwt");
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Expired token → extractUsername throws → no auth, chain proceeds")
    void expiredToken_shouldNotAuthenticate() throws Exception {
        request.addHeader("Authorization", "Bearer expired.jwt");
        when(jwtUtil.extractUsername("expired.jwt"))
                .thenThrow(new RuntimeException("Token expired"));

        jwtRequestFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    // ============================================================
    // Case 5: Valid token but user not found in DB
    // ============================================================

    @Test
    @DisplayName("Valid token but user not found → no auth, no token validation call")
    void userNotFound_shouldNotAuthenticate() throws Exception {
        request.addHeader("Authorization", "Bearer valid.jwt");
        when(jwtUtil.extractUsername("valid.jwt")).thenReturn("ghost");
        when(userDetailsService.loadUserByUsername("ghost"))
                .thenThrow(new UsernameNotFoundException("User not found"));

        jwtRequestFilter.doFilter(request, response, filterChain);

        verify(userDetailsService).loadUserByUsername("ghost");
        verify(jwtUtil, never()).validateToken(anyString(), any(UserDetails.class));
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    // ============================================================
    // Case 6: Valid token but validateToken returns false
    // ============================================================

    @Test
    @DisplayName("validateToken returns false → no auth, chain proceeds")
    void validateTokenFalse_shouldNotAuthenticate() throws Exception {
        request.addHeader("Authorization", "Bearer valid.jwt");
        UserDetails userDetails = buildUserDetails("testuser");

        when(jwtUtil.extractUsername("valid.jwt")).thenReturn("testuser");
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
        when(jwtUtil.validateToken("valid.jwt", userDetails)).thenReturn(false);

        jwtRequestFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    // ============================================================
    // Case 7: Existing authentication in context
    // ============================================================

    @Test
    @DisplayName("Existing authentication → skip loading user, keep existing auth")
    void existingAuthentication_shouldBePreserved() throws Exception {
        request.addHeader("Authorization", "Bearer valid.jwt");
        when(jwtUtil.extractUsername("valid.jwt")).thenReturn("testuser");

        // Pre-populate an existing authentication (simulating upstream filter)
        Authentication existing = new UsernamePasswordAuthenticationToken(
                "already-authenticated", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(existing);

        jwtRequestFilter.doFilter(request, response, filterChain);

        Authentication after = SecurityContextHolder.getContext().getAuthentication();
        assertThat(after).isSameAs(existing);

        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(jwtUtil, never()).validateToken(anyString(), any(UserDetails.class));
        verify(filterChain).doFilter(request, response);
    }



    // ============================================================
    // Case 9: Bearer with extra whitespace – not a real Bearer
    // ============================================================

    @Test
    @DisplayName("Header is 'bearer' lowercase → not recognized (case-sensitive)")
    void lowercaseBearer_shouldNotBeRecognized() throws Exception {
        request.addHeader("Authorization", "bearer valid.jwt");

        jwtRequestFilter.doFilter(request, response, filterChain);

        verifyNoInteractions(jwtUtil, userDetailsService);
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("DB unavailable during user lookup → 503, no auth, chain stops")
    void dbUnavailable_shouldReturn503() throws Exception {
        request.addHeader("Authorization", "Bearer valid.jwt");
        when(jwtUtil.extractUsername("valid.jwt")).thenReturn("testuser");
        when(userDetailsService.loadUserByUsername("testuser"))
                .thenThrow(new DataAccessResourceFailureException("DB down"));

        jwtRequestFilter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain, never()).doFilter(any(), any());
    }
}