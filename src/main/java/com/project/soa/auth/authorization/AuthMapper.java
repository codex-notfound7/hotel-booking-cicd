package com.project.soa.auth.authorization;

import com.project.soa.auth.user.User;

public final class AuthMapper {

    private AuthMapper() {}

    public static RegisterResponse toRegisterResponse(User user) {
        return new RegisterResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name()
        );
    }
}