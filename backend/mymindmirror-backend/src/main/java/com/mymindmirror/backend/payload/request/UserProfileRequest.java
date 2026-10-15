package com.mymindmirror.backend.payload.request;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UserProfileRequest(
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters") String username,
        @Email(message = "Email should be valid") @Size(max = 100, message = "Email must not exceed 100 characters") String email
) {}