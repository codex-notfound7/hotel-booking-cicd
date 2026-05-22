package com.project.soa.auth.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserInternalService {

    User getById(UUID id);

    Optional<User> findByEmail(String email);

    User save(User user);

    List<User> findAll();

    long count();

    void deleteRefreshTokensForUser(UUID userId);

    User updateProfile(UUID id, String name, String email);

    // ---------- Admin methods ----------
    void deactivateUser(UUID userId);
    void activateUser(UUID userId);
    void deleteUser(UUID userId);
    void approveManager(UUID userId);
    void rejectManager(UUID userId);
}