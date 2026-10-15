package com.mymindmirror.backend.payload.response;

import java.util.UUID;

public record UserResponse(UUID id, String username, String email) {}
