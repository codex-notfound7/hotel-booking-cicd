package com.project.soa.auth.authorization;

import java.util.UUID;

public record RegisterResponse(
        UUID id,
        String name,
        String email,
        String role
) {}