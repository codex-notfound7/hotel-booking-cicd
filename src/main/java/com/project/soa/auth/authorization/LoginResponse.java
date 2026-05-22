package com.project.soa.auth.authorization;


public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds,
        String role
) {}