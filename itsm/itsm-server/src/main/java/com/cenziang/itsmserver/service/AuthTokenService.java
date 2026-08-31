package com.cenziang.itsmserver.service;

import com.cenziang.itsmserver.config.properties.ItsmAuthProperties;
import com.cenziang.itsmcommon.api.BusinessException;
import com.cenziang.itsmcommon.api.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthTokenService {
    private final ItsmAuthProperties properties;
    private final SecretKey secretKey;

    public AuthTokenService(ItsmAuthProperties properties) {
        this.properties = properties;
        this.secretKey = buildSecretKey(properties.getJwt().getSecret());
    }

    public String createAccessToken(String userId, String tenantId, List<String> roles, String permissionsVersion, long authVersion) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(properties.getIssuer())
                .audience().add(properties.getAudience()).and()
                .subject(userId)
                .claims(Map.of(
                        "tenant_id", tenantId,
                        "roles", roles,
                        "perm_ver", permissionsVersion,
                        "auth_ver", authVersion,
                        "token_type", "access"
                ))
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .notBefore(Date.from(now))
                .expiration(Date.from(now.plusSeconds(properties.getAccessTokenTtlSeconds())))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public TokenClaims parseAndValidate(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(properties.getIssuer())
                .requireAudience(properties.getAudience())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Object rolesValue = claims.get("roles");
        List<String> roles = rolesValue instanceof List<?> list
                ? list.stream().map(String::valueOf).toList()
                : List.of();

        return new TokenClaims(
                claims.getSubject(),
                claims.get("tenant_id", String.class),
                roles,
                claims.get("perm_ver", String.class),
                claims.get("token_type", String.class),
                claims.getId(),
                claims.get("auth_ver", Number.class).longValue(),
                claims.getExpiration().toInstant()
        );
    }

    public String createRefreshToken() {
        return UUID.randomUUID() + "." + UUID.randomUUID() + "." + UUID.randomUUID();
    }

    public String hashRefreshToken(String refreshToken) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            return bytesToHex(digest.digest(refreshToken.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "failed to hash refresh token");
        }
    }

    private SecretKey buildSecretKey(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "jwt secret is blank");
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(bytes, 0, padded, 0, bytes.length);
            bytes = padded;
        }
        return Keys.hmacShaKeyFor(bytes);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(Character.forDigit((b >> 4) & 0xF, 16));
            builder.append(Character.forDigit((b & 0xF), 16));
        }
        return builder.toString().toLowerCase();
    }

    public record TokenClaims(
            String userId,
            String tenantId,
            List<String> roles,
            String permissionsVersion,
            String tokenType,
            String jti,
            long authVersion,
            Instant expiresAt
    ) {
    }
}
