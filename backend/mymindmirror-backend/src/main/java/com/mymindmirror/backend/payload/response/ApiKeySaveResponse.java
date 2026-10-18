package com.mymindmirror.backend.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Result of a key save attempt. The key is always saved unless status is REJECTED.")
public record ApiKeySaveResponse(
        @Schema(
                description = "Outcome of the save attempt.",
                allowableValues = {"VERIFIED", "REJECTED", "UNVERIFIED", "CLEARED"})
        String status,

        @Schema(description = "Human-readable status message.")
        String message
) {
    public static ApiKeySaveResponse verified() {
        return new ApiKeySaveResponse("VERIFIED", "Key verified and saved.");
    }

    public static ApiKeySaveResponse rejected() {
        return new ApiKeySaveResponse("REJECTED",
                "Your Gemini key was rejected by Google. Check that it is copied correctly and that it has not been revoked.");
    }

    public static ApiKeySaveResponse unverified() {
        return new ApiKeySaveResponse("UNVERIFIED",
                "Key saved, but could not be verified with Google right now. If AI features fail, re-save the key.");
    }

    public static ApiKeySaveResponse cleared() {
        return new ApiKeySaveResponse("CLEARED", "API key cleared. Using shared key.");
    }
}