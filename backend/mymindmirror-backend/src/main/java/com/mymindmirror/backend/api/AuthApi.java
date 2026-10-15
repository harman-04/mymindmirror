package com.mymindmirror.backend.api;

import com.mymindmirror.backend.payload.request.LoginRequest;
import com.mymindmirror.backend.payload.request.RegisterRequest;
import com.mymindmirror.backend.payload.response.AuthResponse;
import com.mymindmirror.backend.payload.response.ErrorResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;

/**
 * OpenAPI contract for {@link com.mymindmirror.backend.controller.AuthController}.
 *
 * <p>Contains only documentation annotations. All Spring MVC binding
 * ({@code @PostMapping}, {@code @RequestBody}, {@code @Valid}) lives on the controller.
 */
@Tag(
        name = "Authentication",
        description = "User registration and login. These endpoints are public — no JWT required. " +
                "Use the 'Authorize' button at the top of the page after login to paste the returned token " +
                "and access protected endpoints."
)
public interface AuthApi {

    @SecurityRequirements  // empty → explicitly no security for this operation
    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account with the provided username, email, and password. " +
                    "Returns 201 Created on success. Username and email must be unique across the system; " +
                    "email is required and must be a well-formed address."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "User registered successfully. Response body contains a confirmation message; " +
                            "the 'token' field is null for this endpoint.",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed (missing username, email, or password; username/password " +
                            "length out of range; email malformed) or username/email already exists.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Database or dependent service temporarily unavailable.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<AuthResponse> registerUser(RegisterRequest request);

    @SecurityRequirements  // empty → explicitly no security for this operation
    @Operation(
            summary = "Authenticate and obtain a JWT",
            description = "Verifies the supplied username and password against the user store. On success, " +
                    "returns a JWT token in the response body. Use the 'Authorize' button at the top of this " +
                    "page to paste the token and access protected endpoints."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Login successful. Response body contains the JWT in the 'token' field.",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed (missing username or password), malformed JSON, or empty body.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid credentials.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Authentication service or database temporarily unavailable.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<AuthResponse> createAuthenticationToken(LoginRequest request);
}