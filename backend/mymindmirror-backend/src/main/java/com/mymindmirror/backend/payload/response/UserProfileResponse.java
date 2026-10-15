package com.mymindmirror.backend.payload.response;
import java.util.UUID;

public record UserProfileResponse(UUID id, String username, String email) {}