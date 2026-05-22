package com.project.soa.auth.user;

import com.project.soa.auth.authorization.RefreshTokenRepository;
import com.project.soa.common.exception.DuplicateResourceException;
import com.project.soa.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class UserInternalServiceImpl implements UserInternalService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public UserInternalServiceImpl(UserRepository userRepository,
                                   RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public User getById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public User save(User user) {
        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return userRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public void deleteRefreshTokensForUser(UUID userId) {
        refreshTokenRepository.deleteAllByUserId(userId);
    }

    @Override
    public User updateProfile(UUID id, String name, String email) {
        User user = getById(id);

        if (name != null && !name.isBlank()) {
            user.setName(name);
        }

        if (email != null && !email.isBlank()) {
            userRepository.findByEmail(email).ifPresent(existing -> {
                if (!existing.getId().equals(id)) {
                    throw new DuplicateResourceException("Email is already in use: " + email);
                }
            });
            user.setEmail(email);
        }

        return userRepository.save(user);
    }

    // ---------------- Admin Methods ----------------

    @Override
    public void deactivateUser(UUID userId) {
        User user = getById(userId);
        user.setRole(UserRole.DEACTIVATED);
        deleteRefreshTokensForUser(userId);
        userRepository.save(user);
    }

    @Override
    public void activateUser(UUID userId) {
        User user = getById(userId);
        if (user.getRole() == UserRole.DEACTIVATED) {
            user.setRole(UserRole.GUEST); // default to GUEST on activation
            userRepository.save(user);
        }
    }

    @Override
    public void deleteUser(UUID userId) {
        deleteRefreshTokensForUser(userId);
        userRepository.deleteById(userId);
    }

    @Override
    public void approveManager(UUID userId) {
        User user = getById(userId);
        if (user.getRole() == UserRole.PENDING_MANAGER) {
            user.setRole(UserRole.MANAGER);
            userRepository.save(user);
        }
    }

    @Override
    public void rejectManager(UUID userId) {
        User user = getById(userId);
        if (user.getRole() == UserRole.PENDING_MANAGER) {
            user.setRole(UserRole.GUEST);
            userRepository.save(user);
        }
    }
}