package com.project.soa.auth.authorization;

import com.project.soa.auth.user.User;
import com.project.soa.auth.user.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class JwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final String issuer;
    private final long accessTokenMinutes;
    private final long refreshTokenDays;

    public JwtTokenService(
            JwtEncoder jwtEncoder,
            RefreshTokenRepository refreshTokenRepository,
            @Value("${security.jwt.issuer}") String issuer,
            @Value("${security.jwt.access-token-minutes}") long accessTokenMinutes,
            @Value("${security.jwt.refresh-token-days}") long refreshTokenDays) {
        this.jwtEncoder = jwtEncoder;
        this.refreshTokenRepository = refreshTokenRepository;
        this.issuer = issuer;
        this.accessTokenMinutes = accessTokenMinutes;
        this.refreshTokenDays = refreshTokenDays;
    }


    @Transactional
    public LoginResponse generateTokenPair(User user) {
        return generateTokenPair(user, user.getRole());
    }

    @Transactional
    public LoginResponse generateTokenPair(User user, UserRole activeRole) {
        // Validate: GUEST cannot elevate to MANAGER
        if (activeRole == UserRole.MANAGER && user.getRole() != UserRole.MANAGER) {
            throw new org.springframework.security.authentication.BadCredentialsException(
                    "You do not have the MANAGER role");
        }
        String accessToken = buildAccessToken(user, activeRole);
        RefreshToken refreshToken = createRefreshToken(user);
        return new LoginResponse(
                accessToken,
                refreshToken.getToken(),
                "Bearer",
                accessTokenMinutes * 60,
                activeRole.name()
        );
    }

    private String buildAccessToken(User user, UserRole activeRole) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(accessTokenMinutes * 60);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim("role", activeRole.name())
                .claim("email", user.getEmail())
                .claim("type", "access")
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private RefreshToken createRefreshToken(User user) {
        RefreshToken rt = new RefreshToken();
        rt.setToken(UUID.randomUUID().toString());
        rt.setUser(user);
        rt.setExpiresAt(Instant.now().plusSeconds(refreshTokenDays * 24 * 3600));
        return refreshTokenRepository.save(rt);
    }
}
