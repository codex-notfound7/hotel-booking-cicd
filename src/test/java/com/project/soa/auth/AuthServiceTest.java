package com.project.soa.auth;

import com.project.soa.auth.authorization.*;
import com.project.soa.auth.user.User;
import com.project.soa.auth.user.UserInternalService;
import com.project.soa.auth.user.UserRole;
import com.project.soa.common.exception.BusinessRuleException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserInternalService userService;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtTokenService jwtTokenService;
    @Mock AuthenticationManager authenticationManager;

    @InjectMocks AuthService authService;

    UUID userId = UUID.randomUUID();
    User existingUser;

    @BeforeEach
    void setUp() {
        existingUser = new User();
        existingUser.setId(userId);
        existingUser.setEmail("user@test.com");
        existingUser.setName("Test User");
        existingUser.setRole(UserRole.GUEST);
        existingUser.setPasswordHash("hashed");
    }

    // ── register ──────────────────────────────────────────────────────────────

    @Test
    void register_emailAlreadyInUse_throwsBusinessRule() {
        when(userService.findByEmail("user@test.com")).thenReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("Test", "user@test.com", "password1", UserRole.GUEST)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("email already in use");
    }

    @Test
    void register_nullRole_throwsBusinessRule() {
        when(userService.findByEmail(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("Test", "new@test.com", "password1", null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Role must be GUEST or MANAGER");
    }

    @Test
    void register_adminRole_throwsBusinessRule() {
        when(userService.findByEmail(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("Test", "new@test.com", "password1", UserRole.ADMIN)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Role must be GUEST or MANAGER");
    }

    @Test
    void register_guestRole_savesWithGuestRole() {
        when(userService.findByEmail(any())).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password1")).thenReturn("hashed");
        when(userService.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(userId);
            return u;
        });

        authService.register(new RegisterRequest("Test", "new@test.com", "password1", UserRole.GUEST));

        verify(userService).save(argThat(u -> u.getRole() == UserRole.GUEST));
    }

    @Test
    void register_managerRole_savesAsPendingManager() {
        when(userService.findByEmail(any())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userService.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(userId);
            return u;
        });

        authService.register(new RegisterRequest("Mgr", "mgr@test.com", "password1", UserRole.MANAGER));

        verify(userService).save(argThat(u -> u.getRole() == UserRole.PENDING_MANAGER));
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    void login_userNotFound_throwsBadCredentials() {
        when(userService.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("missing@test.com", "pass")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_deactivatedUser_throwsDisabled() {
        existingUser.setRole(UserRole.DEACTIVATED);
        when(userService.findByEmail("user@test.com")).thenReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> authService.login(new LoginRequest("user@test.com", "pass")))
                .isInstanceOf(DisabledException.class)
                .hasMessageContaining("deactivated");
    }

    @Test
    void login_pendingManager_throwsDisabled() {
        existingUser.setRole(UserRole.PENDING_MANAGER);
        when(userService.findByEmail("user@test.com")).thenReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> authService.login(new LoginRequest("user@test.com", "pass")))
                .isInstanceOf(DisabledException.class)
                .hasMessageContaining("pending");
    }

    @Test
    void login_validCredentials_callsAuthManagerAndReturnsTokens() {
        when(userService.findByEmail("user@test.com")).thenReturn(Optional.of(existingUser));
        LoginResponse expected = new LoginResponse("access-tok", "refresh-tok", "Bearer", 3600, "GUEST");
        when(jwtTokenService.generateTokenPair(existingUser, UserRole.GUEST)).thenReturn(expected);

        LoginResponse result = authService.login(new LoginRequest("user@test.com", "pass"));

        assertThat(result.accessToken()).isEqualTo("access-tok");
        verify(authenticationManager).authenticate(any());
    }

    // ── refresh ───────────────────────────────────────────────────────────────

    @Test
    void refresh_tokenNotFound_throwsBadCredentials() {
        when(refreshTokenRepository.findByToken("bad-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("bad-token")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid refresh token");
    }

    @Test
    void refresh_tokenExpired_deletesAndThrows() {
        RefreshToken expired = new RefreshToken();
        expired.setToken("expired-token");
        expired.setUser(existingUser);
        expired.setExpiresAt(Instant.now().minusSeconds(3600)); // already expired

        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("expired-token")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("expired");

        verify(refreshTokenRepository).delete(expired);
    }

    @Test
    void refresh_validToken_deletesOldAndReturnsNewPair() {
        RefreshToken token = new RefreshToken();
        token.setToken("valid-token");
        token.setUser(existingUser);
        token.setExpiresAt(Instant.now().plusSeconds(86400));
        token.setRevoked(false);

        when(refreshTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));
        LoginResponse expected = new LoginResponse("new-access", "new-refresh", "Bearer", 3600, "GUEST");
        when(jwtTokenService.generateTokenPair(existingUser)).thenReturn(expected);

        LoginResponse result = authService.refresh(new RefreshRequest("valid-token"));

        assertThat(result.accessToken()).isEqualTo("new-access");
        verify(refreshTokenRepository).delete(token);
    }

    // ── logout ────────────────────────────────────────────────────────────────

    @Test
    void logout_tokenExists_revokesAllTokensForUser() {
        RefreshToken token = new RefreshToken();
        token.setToken("valid-token");
        token.setUser(existingUser);
        token.setExpiresAt(Instant.now().plusSeconds(86400));

        when(refreshTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));

        authService.logout("valid-token");

        verify(userService).deleteRefreshTokensForUser(userId);
    }

    @Test
    void logout_tokenNotFound_doesNothing() {
        when(refreshTokenRepository.findByToken("unknown")).thenReturn(Optional.empty());

        authService.logout("unknown");

        verifyNoInteractions(userService);
    }
}
