package com.smarthire.modules.auth.dto.response;

public record AuthResponse(
    String accessToken,
    String tokenType,
    long expiresIn,
    UserProfileResponse user
) {
}
