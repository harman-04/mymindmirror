package com.mymindmirror.backend.exception;

import com.mymindmirror.backend.payload.response.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import static org.assertj.core.api.Assertions.assertThat;

class RestExceptionHandlerTest {

    private RestExceptionHandler handler;
    private ServletWebRequest webRequest;

    @BeforeEach
    void setUp() {
        handler = new RestExceptionHandler();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/test/endpoint");
        webRequest = new ServletWebRequest(request);
    }

    // ------------------------------------------------------------------
    // IllegalArgument → 400
    // ------------------------------------------------------------------

    @Test
    @DisplayName("IllegalArgumentException → 400 with the exact message")
    void illegalArgument_returns400() {
        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgumentException(
                new IllegalArgumentException("Start date cannot be after end date."), webRequest);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(400);
        assertThat(body.error()).isEqualTo("Bad Request");
        assertThat(body.message()).isEqualTo("Start date cannot be after end date.");
        assertThat(body.path()).isEqualTo("/api/test/endpoint");
    }

    // ------------------------------------------------------------------
    // AccessDenied → 403
    // ------------------------------------------------------------------

    @Test
    @DisplayName("AccessDeniedException → 403 with generic message in prod")
    void accessDenied_returns403() {
        ResponseEntity<ErrorResponse> response = handler.handleAccessDeniedException(
                new AccessDeniedException("Access is denied"), webRequest);

        assertThat(response.getStatusCode().value()).isEqualTo(403);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(403);
        assertThat(body.error()).isEqualTo("Forbidden");
        assertThat(body.message())
                .isEqualTo("You do not have permission to access this resource.");
        assertThat(body.debug_message()).isNull();  // prod default
    }

    // ------------------------------------------------------------------
    // Authentication → 401
    // ------------------------------------------------------------------

    @Test
    @DisplayName("AuthenticationException → 401 with generic message")
    void authenticationFailed_returns401() {
        ResponseEntity<ErrorResponse> response = handler.handleAuthenticationException(
                new BadCredentialsException("Bad credentials"), webRequest);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(401);
        assertThat(body.error()).isEqualTo("Unauthorized");
        assertThat(body.message()).isEqualTo("Invalid credentials");
    }

    // ------------------------------------------------------------------
    // RuntimeException → 500 (prod: no debug)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("RuntimeException → 500 with generic message in prod")
    void runtimeException_returns500_genericMessage() {
        ResponseEntity<ErrorResponse> response = handler.handleRuntimeException(
                new RuntimeException("NullPointerException at Foo.bar"), webRequest);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(500);
        assertThat(body.error()).isEqualTo("Internal Server Error");
        assertThat(body.message()).isEqualTo("An unexpected error occurred.");
        assertThat(body.debug_message()).isNull();  // prod default
    }

    @Test
    @DisplayName("RuntimeException → 500 with debug_message in dev")
    void runtimeException_returns500_debugMessageInDev() {
        ReflectionTestUtils.setField(handler, "exposeDebugMessage", true);

        ResponseEntity<ErrorResponse> response = handler.handleRuntimeException(
                new RuntimeException("NullPointerException at Foo.bar"), webRequest);

        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.debug_message()).isEqualTo("NullPointerException at Foo.bar");
    }

    // ------------------------------------------------------------------
    // Catch-all Exception → 500
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Unhandled Exception → 500")
    void unhandledException_returns500() {
        ResponseEntity<ErrorResponse> response = handler.handleAllExceptions(
                new Exception("Something exploded"), webRequest);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.message()).isEqualTo("An internal server error occurred.");
    }

    // ------------------------------------------------------------------
    // Path extraction with no URI
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Missing request URI → path is null, response still valid")
    void noRequestUri_pathIsNull() {
        ServletWebRequest emptyRequest = new ServletWebRequest(new MockHttpServletRequest());

        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgumentException(
                new IllegalArgumentException("bad"), emptyRequest);

        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.path()).isEmpty();  // MockHttpServletRequest returns "" by default
    }

// ------------------------------------------------------------------
    // Missing JaCoCo Coverage: Validation, Malformed JSON, and Paths
    // ------------------------------------------------------------------

    @Test
    @DisplayName("MethodArgumentNotValidException → 400 and covers ObjectError fallback branch")
    void methodArgumentNotValid_returns400_andCoversObjectError() {
        org.springframework.web.bind.MethodArgumentNotValidException ex = org.mockito.Mockito.mock(org.springframework.web.bind.MethodArgumentNotValidException.class);
        org.springframework.validation.BindingResult bindingResult = org.mockito.Mockito.mock(org.springframework.validation.BindingResult.class);

        // 1. FieldError to cover the true branch of the ternary operator
        org.springframework.validation.FieldError fieldError = new org.springframework.validation.FieldError("user", "email", "must be a well-formed email address");

        // 2. ObjectError to cover the false branch (`: error.getDefaultMessage()`)
        org.springframework.validation.ObjectError globalError = new org.springframework.validation.ObjectError("user", "Passwords do not match");

        org.mockito.Mockito.when(ex.getBindingResult()).thenReturn(bindingResult);
        org.mockito.Mockito.when(bindingResult.getAllErrors()).thenReturn(java.util.List.of(fieldError, globalError));

        ResponseEntity<Object> response = handler.handleMethodArgumentNotValid(
                ex, new org.springframework.http.HttpHeaders(), org.springframework.http.HttpStatus.BAD_REQUEST, webRequest);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        ErrorResponse body = (ErrorResponse) response.getBody();
        assertThat(body.message()).contains("email: must be a well-formed email address");
        assertThat(body.message()).contains("Passwords do not match");
    }

    @Test
    @DisplayName("ConstraintViolationException → 400")
    void constraintViolation_returns400() {
        jakarta.validation.ConstraintViolationException ex = new jakarta.validation.ConstraintViolationException("Invalid query parameter", null);

        ResponseEntity<ErrorResponse> response = handler.handleConstraintViolation(ex, webRequest);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().message()).contains("Invalid query parameter");
    }

    @Test
    @DisplayName("extractPath() returns null when request is not a ServletWebRequest")
    void extractPath_returnsNullForNonServletRequest() {
        // Mock a plain WebRequest to bypass the `if (request instanceof ServletWebRequest)` check
        WebRequest plainWebRequest = org.mockito.Mockito.mock(WebRequest.class);

        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgumentException(
                new IllegalArgumentException("test exception"), plainWebRequest);

        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        // Verifies the exact "return null;" line was hit
        assertThat(body.path()).isNull();
    }

    @Test
    @DisplayName("HttpMessageNotReadableException → 400")
    void httpMessageNotReadable_returns400() {
        // Modern Spring requires an HttpInputMessage in the constructor
        org.springframework.http.HttpInputMessage inputMessage = org.mockito.Mockito.mock(org.springframework.http.HttpInputMessage.class);

        org.springframework.http.converter.HttpMessageNotReadableException ex =
                new org.springframework.http.converter.HttpMessageNotReadableException("JSON parse error", inputMessage);

        ResponseEntity<Object> response = handler.handleHttpMessageNotReadable(
                ex, new org.springframework.http.HttpHeaders(), org.springframework.http.HttpStatus.BAD_REQUEST, webRequest);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(((ErrorResponse) response.getBody()).message()).contains("Malformed JSON request");
    }

    @Test
    @DisplayName("ResponseStatusException → preserves dynamic HTTP status")
    void responseStatusException_returnsDynamicStatus() {
        org.springframework.web.server.ResponseStatusException ex =
                new org.springframework.web.server.ResponseStatusException(HttpStatus.CONFLICT, "Resource already exists");

        ResponseEntity<ErrorResponse> response = handler.handleResponseStatusException(ex, webRequest);

        assertThat(response.getStatusCode().value()).isEqualTo(409); // CONFLICT is 409
        assertThat(response.getBody().message()).isEqualTo("Resource already exists");
    }

    @Test
    @DisplayName("buildResponse() with debug enabled but null debug → no debug_message (short-circuit)")
    void buildResponse_debugEnabledButDebugNull_omitsDebugMessage() {
        // Enable debug globally for this test
        ReflectionTestUtils.setField(handler, "exposeDebugMessage", true);

        // handleMethodArgumentNotValid passes `null` as the debug argument,
        // so we exercise the (exposeDebug=true && debug == null) short-circuit.
        org.springframework.web.bind.MethodArgumentNotValidException ex =
                org.mockito.Mockito.mock(org.springframework.web.bind.MethodArgumentNotValidException.class);
        org.springframework.validation.BindingResult bindingResult =
                org.mockito.Mockito.mock(org.springframework.validation.BindingResult.class);

        org.springframework.validation.ObjectError error =
                new org.springframework.validation.ObjectError("field", "must not be null");

        org.mockito.Mockito.when(ex.getBindingResult()).thenReturn(bindingResult);
        org.mockito.Mockito.when(bindingResult.getAllErrors()).thenReturn(java.util.List.of(error));

        ResponseEntity<Object> response = handler.handleMethodArgumentNotValid(
                ex,
                new org.springframework.http.HttpHeaders(),
                org.springframework.http.HttpStatus.BAD_REQUEST,
                webRequest);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        ErrorResponse body = (ErrorResponse) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.debug_message()).isNull();   // ✅ no debug_message even though exposeDebug=true
        assertThat(body.message()).contains("must not be null");
    }

    // ------------------------------------------------------------------
// ResponseStatusException → 5xx (covers the log.error branch)
// ------------------------------------------------------------------

    @Test
    @DisplayName("ResponseStatusException with 5xx → keeps 5xx status and logs at error level")
    void responseStatusException_5xx_coversErrorLogBranch() {
        org.springframework.web.server.ResponseStatusException ex =
                new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE, "Upstream is down");

        ResponseEntity<ErrorResponse> response =
                handler.handleResponseStatusException(ex, webRequest);

        // Status is preserved (503), not downgraded
        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Upstream is down");
    }

// ------------------------------------------------------------------
// ResponseStatusException → non-standard code (covers status == null)
// ------------------------------------------------------------------

    @Test
    @DisplayName("ResponseStatusException with non-standard code → falls back to 500")
    void responseStatusException_unknownStatusCode_fallsBackTo500() {
        // 599 has no constant in HttpStatus, so HttpStatus.resolve(599) returns null.
        // HttpStatusCode.valueOf(599) gives us a DefaultHttpStatusCode that still
        // carries the numeric value, which is what the handler reads.
        org.springframework.http.HttpStatusCode custom = org.springframework.http.HttpStatusCode.valueOf(599);
        org.springframework.web.server.ResponseStatusException ex =
                new org.springframework.web.server.ResponseStatusException(custom, "Custom failure");

        ResponseEntity<ErrorResponse> response =
                handler.handleResponseStatusException(ex, webRequest);

        assertThat(response.getStatusCode().value())
                .as("null HttpStatus should be coerced to INTERNAL_SERVER_ERROR")
                .isEqualTo(500);
        assertThat(response.getBody()).isNotNull();
        // reason is still forwarded
        assertThat(response.getBody().message()).isEqualTo("Custom failure");
    }


    // ------------------------------------------------------------------
// RedisConnectionFailureException → 503
// ------------------------------------------------------------------

    @Test
    @DisplayName("RedisConnectionFailureException → 503 with cache-specific message")
    void redisConnectionFailure_returns503() {
        org.springframework.data.redis.RedisConnectionFailureException ex =
                new org.springframework.data.redis.RedisConnectionFailureException("Redis is down");

        ResponseEntity<ErrorResponse> response = handler.handleRedisConnectionFailure(ex, webRequest);

        assertThat(response.getStatusCode().value()).isEqualTo(503);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(503);
        assertThat(body.error()).isEqualTo("Service Unavailable");
        assertThat(body.message()).isEqualTo("Cache service is temporarily unavailable. Please try again in a moment.");
        assertThat(body.path()).isEqualTo("/api/test/endpoint");
    }

// ------------------------------------------------------------------
// DataAccessResourceFailureException → 503
// ------------------------------------------------------------------

    @Test
    @DisplayName("DataAccessResourceFailureException → 503 with generic dependency message")
    void dataAccessResourceFailure_returns503() {
        org.springframework.dao.DataAccessResourceFailureException ex =
                new org.springframework.dao.DataAccessResourceFailureException("DB is down");

        ResponseEntity<ErrorResponse> response = handler.handleResourceFailure(ex, webRequest);

        assertThat(response.getStatusCode().value()).isEqualTo(503);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(503);
        assertThat(body.error()).isEqualTo("Service Unavailable");
        assertThat(body.message()).isEqualTo("A backend service is temporarily unavailable. Please try again in a moment.");
        assertThat(body.path()).isEqualTo("/api/test/endpoint");
    }

    @Test
    @DisplayName("InternalAuthenticationServiceException → 503 (not 401)")
    void internalAuthServiceException_returns503() {
        InternalAuthenticationServiceException ex = new InternalAuthenticationServiceException(
                "Unable to acquire JDBC Connection");

        ResponseEntity<ErrorResponse> response = handler.handleAuthServiceFailure(ex, webRequest);

        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getBody().message())
                .isEqualTo("Authentication service is temporarily unavailable. Please try again in a moment.");
    }
}