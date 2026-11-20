package com.aegiscare.security;

import com.aegiscare.entity.UserAccount;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;

@Service
public class JwtTokenService {
    private final JwtEncoder encoder;
    private final long ttlMinutes;

    public JwtTokenService(JwtEncoder encoder,
                           @Value("${aegiscare.security.jwt.ttl-minutes:30}") long ttlMinutes) {
        this.encoder = encoder;
        this.ttlMinutes = Math.max(5, Math.min(ttlMinutes, 120));
    }

    public IssuedToken issue(UserAccount account) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(ttlMinutes * 60);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("aegiscare")
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(account.getEmail())
                .claim("userId", account.getId())
                .claim("fullName", account.getFullName())
                .claim("roles", List.of(account.getRole().name()))
                .claim("hospitalId", account.getHospitalId() == null ? "" : account.getHospitalId())
                .build();
        String token = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new IssuedToken(token, ttlMinutes * 60);
    }

    public record IssuedToken(String accessToken, long expiresIn) { }
}
