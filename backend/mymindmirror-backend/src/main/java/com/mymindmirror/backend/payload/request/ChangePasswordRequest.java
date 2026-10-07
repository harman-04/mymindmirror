package com.mymindmirror.backend.payload.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required")
        String currentPassword,

        @NotBlank(message = "New password is required")
        @Size(min = 6, max = 72, message = "New password must be between 6 and 72 characters")
        String newPassword
) {}