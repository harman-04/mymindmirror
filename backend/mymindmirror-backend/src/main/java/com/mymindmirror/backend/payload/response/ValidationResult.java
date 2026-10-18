package com.mymindmirror.backend.payload.response;

import com.mymindmirror.backend.enums.ValidationStatus;

public record ValidationResult(ValidationStatus status) {}