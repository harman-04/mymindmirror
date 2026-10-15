package com.mymindmirror.backend.resolver;

import com.mymindmirror.backend.annotation.CurrentUser;
import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.security.services.UserDetailsImpl;
import com.mymindmirror.backend.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurrentUserArgumentResolverTest {

    @Mock
    private UserService userService;

    @Mock
    private NativeWebRequest webRequest;

    private CurrentUserArgumentResolver resolver;
    private UUID userId;
    private UserDetailsImpl userDetails;
    private User user;

    // ============================================================
    // Sample controller-like method for MethodParameter reflection
    // ============================================================

    @SuppressWarnings("unused")
    private void sampleMethod(@CurrentUser User user) { /* for reflection only */ }

    @SuppressWarnings("unused")
    private void sampleMethodWithoutAnnotation(User user) { /* for reflection only */ }

    @SuppressWarnings("unused")
    private void sampleMethodWithStringParam(@CurrentUser String value) { /* for reflection only */ }

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        resolver = new CurrentUserArgumentResolver(userService);

        userId = UUID.randomUUID();
        userDetails = new UserDetailsImpl(
                userId, "testuser", "test@example.com", "hashed", null);

        user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPasswordHash("hashed");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ============================================================
    // supportsParameter()
    // ============================================================

    @Test
    @DisplayName("supportsParameter returns true for @CurrentUser User parameter")
    void supportsParameter_true_whenCurrentUserUser() throws NoSuchMethodException {
        Method method = getClass().getDeclaredMethod("sampleMethod", User.class);
        MethodParameter param = new MethodParameter(method, 0);

        assertThat(resolver.supportsParameter(param)).isTrue();
    }

    @Test
    @DisplayName("supportsParameter returns false when @CurrentUser is missing")
    void supportsParameter_false_whenAnnotationMissing() throws NoSuchMethodException {
        Method method = getClass().getDeclaredMethod("sampleMethodWithoutAnnotation", User.class);
        MethodParameter param = new MethodParameter(method, 0);

        assertThat(resolver.supportsParameter(param)).isFalse();
    }

    @Test
    @DisplayName("supportsParameter returns false when parameter type is not User")
    void supportsParameter_false_whenNotUserType() throws NoSuchMethodException {
        Method method = getClass().getDeclaredMethod("sampleMethodWithStringParam", String.class);
        MethodParameter param = new MethodParameter(method, 0);

        assertThat(resolver.supportsParameter(param)).isFalse();
    }

    // ============================================================
    // resolveArgument() – authentication failures
    // ============================================================

    @Test
    @DisplayName("resolveArgument throws 401 when no authentication in context")
    void resolveArgument_throws401_whenNoAuthentication() throws NoSuchMethodException {
        Method method = getClass().getDeclaredMethod("sampleMethod", User.class);
        MethodParameter param = new MethodParameter(method, 0);

        // No authentication set
        assertThatThrownBy(() -> resolver.resolveArgument(param, null, webRequest, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Authentication required");

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("resolveArgument throws 401 when principal is not UserDetailsImpl")
    void resolveArgument_throws401_whenPrincipalIsNotUserDetailsImpl() throws NoSuchMethodException {
        Method method = getClass().getDeclaredMethod("sampleMethod", User.class);
        MethodParameter param = new MethodParameter(method, 0);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                "some-string-principal", null, null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThatThrownBy(() -> resolver.resolveArgument(param, null, webRequest, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid authentication principal");

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("resolveArgument throws 401 when user is not found in database")
    void resolveArgument_throws401_whenUserNotFound() throws NoSuchMethodException {
        Method method = getClass().getDeclaredMethod("sampleMethod", User.class);
        MethodParameter param = new MethodParameter(method, 0);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(userService.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.resolveArgument(param, null, webRequest, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("User not found");

        verify(userService).findById(userId);
    }

    // ============================================================
    // resolveArgument() – happy path
    // ============================================================

    @Test
    @DisplayName("resolveArgument returns the User entity when authentication is valid")
    void resolveArgument_returnsUser_whenAuthenticated() throws NoSuchMethodException {
        Method method = getClass().getDeclaredMethod("sampleMethod", User.class);
        MethodParameter param = new MethodParameter(method, 0);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(userService.findById(userId)).thenReturn(Optional.of(user));

        Object resolved = resolver.resolveArgument(param, null, webRequest, null);

        assertThat(resolved).isInstanceOf(User.class);
        assertThat(resolved).isSameAs(user);
        verify(userService).findById(userId);
    }

    @Test
    @DisplayName("resolveArgument throws 401 when authentication is present but not authenticated")
    void resolveArgument_throws401_whenAuthenticationNotAuthenticated() throws NoSuchMethodException {
        Method method = getClass().getDeclaredMethod("sampleMethod", User.class);
        MethodParameter param = new MethodParameter(method, 0);

        // 2-arg constructor → authenticated flag stays FALSE
        Authentication unauthenticated =
                new UsernamePasswordAuthenticationToken("principal", "credentials");

        assertThat(unauthenticated.isAuthenticated())
                .as("sanity check: this token must be unauthenticated to hit the branch")
                .isFalse();

        SecurityContextHolder.getContext().setAuthentication(unauthenticated);

        assertThatThrownBy(() -> resolver.resolveArgument(param, null, webRequest, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Authentication required");

        verifyNoInteractions(userService);
    }
}