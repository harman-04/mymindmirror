package com.mymindmirror.backend.exception;

import com.mymindmirror.backend.payload.response.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.util.stream.Collectors;


@Slf4j
@org.springframework.web.bind.annotation.RestControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    @Value("${app.error.expose-debug:false}")
    private boolean exposeDebugMessage;

    // ------------------------------------------------------------------
    // Helper
    // ------------------------------------------------------------------

    private ErrorResponse buildResponse(HttpStatus status, String message, WebRequest request, String debug) {
        String path = extractPath(request);
        return exposeDebugMessage && debug != null
                ? ErrorResponse.of(status.value(), status.getReasonPhrase(), message, path, debug)
                : ErrorResponse.of(status.value(), status.getReasonPhrase(), message, path);
    }

    private String extractPath(WebRequest request) {
        if (request instanceof ServletWebRequest swr) {
            return swr.getRequest().getRequestURI();
        }
        return null;
    }



    // ------------------------------------------------------------------
// Missing required @RequestParam
// ------------------------------------------------------------------

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        log.warn("Missing required parameter: {}", ex.getParameterName());

        ErrorResponse body = buildResponse(
                HttpStatus.BAD_REQUEST,
                "Missing required parameter: " + ex.getParameterName(),
                request, ex.getMessage());

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    // ------------------------------------------------------------------
// Type mismatch on @PathVariable / @RequestParam
// (replaces handleMethodArgumentTypeMismatch)
// ------------------------------------------------------------------

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        String paramName = "unknown";
        String value = "unknown";

        if (ex instanceof MethodArgumentTypeMismatchException mismatch) {
            paramName = mismatch.getName();
            value = String.valueOf(mismatch.getValue());
        }

        log.warn("Type mismatch for parameter '{}': value '{}'", paramName, value);

        ErrorResponse body = buildResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid value for parameter '" + paramName + "': " + value,
                request, ex.getMessage());

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }
    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        log.warn("Unsupported media type: {}", ex.getContentType());
        ErrorResponse body = buildResponse(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Unsupported media type: " + ex.getContentType(),
                request, ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        log.warn("Method not allowed: {}", ex.getMethod());
        ErrorResponse body = buildResponse(
                HttpStatus.METHOD_NOT_ALLOWED,
                "HTTP method '" + ex.getMethod() + "' is not supported for this endpoint.",
                request, ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.METHOD_NOT_ALLOWED);
    }

// ------------------------------------------------------------------
    // 1. Bean validation on @RequestBody
    // ------------------------------------------------------------------

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        String errors = ex.getBindingResult().getAllErrors().stream()
                .map(error -> (error instanceof FieldError fieldError)
                        ? fieldError.getField() + ": " + fieldError.getDefaultMessage()
                        : error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        ErrorResponse body = buildResponse(HttpStatus.BAD_REQUEST, "Validation failed: " + errors, request, null);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }


    // ------------------------------------------------------------------
    // 2. Validation on @RequestParam / @PathVariable
    // ------------------------------------------------------------------

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
        log.warn("Constraint violation: {}", ex.getMessage());
        ErrorResponse body = buildResponse(HttpStatus.BAD_REQUEST, "Validation failed: " + ex.getMessage(), request, ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    // ------------------------------------------------------------------
    // 3. Malformed / empty JSON
    // ------------------------------------------------------------------

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        log.warn("Malformed JSON request: {}", ex.getMessage());
        ErrorResponse body = buildResponse(HttpStatus.BAD_REQUEST, "Malformed JSON request or empty body.", request, ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    // ------------------------------------------------------------------
    // 4. Spring Security Access Denied (403)
    // ------------------------------------------------------------------

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        log.warn("Access Denied: {}", ex.getMessage());
        ErrorResponse body = buildResponse(HttpStatus.FORBIDDEN,
                "You do not have permission to access this resource.", request, ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.FORBIDDEN);
    }

    // ------------------------------------------------------------------
    // 5. Authentication Exception (401)
    // ------------------------------------------------------------------

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex, WebRequest request) {
        log.warn("Authentication failed: {}", ex.getMessage());
        ErrorResponse body = buildResponse(HttpStatus.UNAUTHORIZED, "Invalid credentials", request, ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.UNAUTHORIZED);
    }

    // ------------------------------------------------------------------
    // 6. Illegal Argument (400)
    // ------------------------------------------------------------------

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        log.warn("IllegalArgumentException: {}", ex.getMessage());
        ErrorResponse body = buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request, ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    // ------------------------------------------------------------------
    // 7. Generic Runtime (500)
    // ------------------------------------------------------------------

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex, WebRequest request) {
        log.error("RuntimeException: {}", ex.getMessage(), ex);
        ErrorResponse body = buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred.", request, ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(
            ResponseStatusException ex, WebRequest request) {

        int statusCode = ex.getStatusCode().value();
        HttpStatus status = HttpStatus.resolve(statusCode);
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        // Log 5xx at error level, 4xx at warn
        if (status.is5xxServerError()) {
            log.error("ResponseStatusException [{}]: {}", statusCode, ex.getReason(), ex);
        } else {
            log.warn("ResponseStatusException [{}]: {}", statusCode, ex.getReason());
        }

        ErrorResponse body = buildResponse(status, ex.getReason(), request, ex.getMessage());
        return new ResponseEntity<>(body, status);
    }


    @ExceptionHandler(RedisConnectionFailureException.class)
    public ResponseEntity<ErrorResponse> handleRedisConnectionFailure(
            RedisConnectionFailureException ex, WebRequest request) {
        log.error("Redis connection failure: {}", ex.getMessage(), ex);
        ErrorResponse body = buildResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Cache service is temporarily unavailable. Please try again in a moment.",
                request,
                ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.SERVICE_UNAVAILABLE);
    }


    @ExceptionHandler(DataAccessResourceFailureException.class)
    public ResponseEntity<ErrorResponse> handleResourceFailure(
            DataAccessResourceFailureException ex, WebRequest request) {
        log.error("Data resource failure: {}", ex.getMessage(), ex);
        ErrorResponse body = buildResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                "A backend service is temporarily unavailable. Please try again in a moment.",
                request,
                ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(InternalAuthenticationServiceException.class)
    public ResponseEntity<ErrorResponse> handleAuthServiceFailure(
            InternalAuthenticationServiceException ex, WebRequest request) {
        log.error("Authentication service failure (likely DB/dependency down): {}", ex.getMessage(), ex);
        ErrorResponse body = buildResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Authentication service is temporarily unavailable. Please try again in a moment.",
                request,
                ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.SERVICE_UNAVAILABLE);
    }
    // ------------------------------------------------------------------
    // 8. Catch-all (500)
    // ------------------------------------------------------------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllExceptions(Exception ex, WebRequest request) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        ErrorResponse body = buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "An internal server error occurred.", request, ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }


}