package com.mymindmirror.backend.enums;

public enum ValidationStatus {
    VERIFIED, REJECTED, UNVERIFIED

    // Note: CLEARED is not a validation result — it's a service outcome
    // when the input is blank. See ApiKeySaveResponse.
}