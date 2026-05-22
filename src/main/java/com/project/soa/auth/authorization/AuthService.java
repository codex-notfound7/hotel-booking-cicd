package com.project.soa.auth.authorization;

import com.project.soa.auth.user.User;
import com.project.soa.auth.user.UserInternalService;
import com.project.soa.auth.user.UserRole;
import com.project.soa.common.exception.BusinessRuleException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    private final UserInternalService userService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserInternalService userService,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenService jwtTokenService,
                       AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.authenticationManager = authenticationManager;
    }

    public RegisterResponse register(RegisterRequest request) {
        if (userService.findByEmail(request.email()).isPresent()) {
            throw new BusinessRuleException("Registration failed: email already in use");
        }

        if (request.role() == null ||
                (request.role() != UserRole.GUEST && request.role() != UserRole.MANAGER)) {
            throw new BusinessRuleException("Role must be GUEST or MANAGER");
        }

        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));

        // Set role based on registration type
        if (request.role() == UserRole.MANAGER) {
            user.setRole(UserRole.PENDING_MANAGER); // Admin approval needed
        } else {
            user.setRole(UserRole.GUEST);
        }

        return AuthMapper.toRegisterResponse(userService.save(user));
    }

    public LoginResponse login(LoginRequest request) {
        User user = userService.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (user.getRole() == UserRole.DEACTIVATED) {
            throw new org.springframework.security.authentication.DisabledException("Account is deactivated");
        }
        if (user.getRole() == UserRole.PENDING_MANAGER) {
            throw new org.springframework.security.authentication.DisabledException("Account is pending admin approval");
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        return jwtTokenService.generateTokenPair(user, user.getRole());
    }

    public LoginResponse refresh(RefreshRequest request) {
        RefreshToken stored = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        if (!stored.isValid()) {
            refreshTokenRepository.delete(stored);
            throw new BadCredentialsException("Refresh token expired or revoked. Please log in again.");
        }

        User user = stored.getUser();
        refreshTokenRepository.delete(stored);
        refreshTokenRepository.flush();

        return jwtTokenService.generateTokenPair(user);
    }


    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken)
                .ifPresent(stored -> userService.deleteRefreshTokensForUser(stored.getUser().getId()));
    }
}