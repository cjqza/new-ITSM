package com.cenziang.itsmserver.repository;

import com.cenziang.itsmserver.domain.AuthRefreshToken;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class AuthRefreshTokenRepository {
    private final JdbcTemplate jdbcTemplate;

    public AuthRefreshTokenRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(AuthRefreshToken refreshToken) {
        jdbcTemplate.update(
                """
                        INSERT INTO auth_refresh_token (
                            token_id, tenant_id, user_id, token_hash, expires_at, revoked_at, replaced_by, auth_version
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                refreshToken.tokenId(),
                refreshToken.tenantId(),
                refreshToken.userId(),
                refreshToken.tokenHash(),
                Timestamp.valueOf(refreshToken.expiresAt()),
                refreshToken.revokedAt() == null ? null : Timestamp.valueOf(refreshToken.revokedAt()),
                refreshToken.replacedBy(),
                refreshToken.authVersion()
        );
    }

    public Optional<AuthRefreshToken> findActiveByTokenHash(String tokenHash) {
        List<AuthRefreshToken> tokens = jdbcTemplate.query(
                """
                        SELECT token_id, tenant_id, user_id, token_hash, expires_at, revoked_at, replaced_by, auth_version
                        FROM auth_refresh_token
                        WHERE token_hash = ? AND revoked_at IS NULL AND expires_at > CURRENT_TIMESTAMP(3)
                        LIMIT 1
                        """,
                (rs, rowNum) -> new AuthRefreshToken(
                        rs.getString("token_id"),
                        rs.getString("tenant_id"),
                        rs.getString("user_id"),
                        rs.getString("token_hash"),
                        rs.getTimestamp("expires_at").toLocalDateTime(),
                        toLocalDateTime(rs.getTimestamp("revoked_at")),
                        rs.getString("replaced_by"),
                        rs.getLong("auth_version")
                ),
                tokenHash
        );
        return tokens.stream().findFirst();
    }

    public void revokeByHash(String tokenHash, String replacedBy) {
        jdbcTemplate.update(
                """
                        UPDATE auth_refresh_token
                        SET revoked_at = CURRENT_TIMESTAMP(3),
                            replaced_by = ?,
                            updated_at = CURRENT_TIMESTAMP(3)
                        WHERE token_hash = ? AND revoked_at IS NULL
                        """,
                replacedBy,
                tokenHash
        );
    }

    public void revokeAllByTenantAndUserId(String tenantId, String userId) {
        jdbcTemplate.update(
                """
                        UPDATE auth_refresh_token
                        SET revoked_at = CURRENT_TIMESTAMP(3),
                            updated_at = CURRENT_TIMESTAMP(3)
                        WHERE tenant_id = ? AND user_id = ? AND revoked_at IS NULL
                        """,
                tenantId,
                userId
        );
    }

    private LocalDateTime toLocalDateTime(java.sql.Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
