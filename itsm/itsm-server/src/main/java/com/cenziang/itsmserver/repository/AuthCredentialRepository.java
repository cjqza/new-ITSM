package com.cenziang.itsmserver.repository;

import com.cenziang.itsmserver.domain.AuthCredential;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class AuthCredentialRepository {
    private final JdbcTemplate jdbcTemplate;

    public AuthCredentialRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<AuthCredential> findByTenantIdAndLoginName(String tenantId, String loginName) {
        List<AuthCredential> credentials = jdbcTemplate.query(
                """
                        SELECT credential_id, tenant_id, user_id, login_name, password_hash, password_algo,
                               password_version, auth_version, failed_count, locked_until,
                               last_login_at, last_login_ip, status
                        FROM user_credential
                        WHERE tenant_id = ? AND login_name = ?
                        LIMIT 1
                        """,
                (rs, rowNum) -> new AuthCredential(
                        rs.getString("credential_id"),
                        rs.getString("tenant_id"),
                        rs.getString("user_id"),
                        rs.getString("login_name"),
                        rs.getString("password_hash"),
                        rs.getString("password_algo"),
                        rs.getLong("password_version"),
                        rs.getLong("auth_version"),
                        rs.getInt("failed_count"),
                        toLocalDateTime(rs.getTimestamp("locked_until")),
                        toLocalDateTime(rs.getTimestamp("last_login_at")),
                        rs.getString("last_login_ip"),
                        rs.getString("status")
                ),
                tenantId,
                loginName
        );
        return credentials.stream().findFirst();
    }

    public Optional<AuthCredential> findByTenantIdAndUserId(String tenantId, String userId) {
        List<AuthCredential> credentials = jdbcTemplate.query(
                """
                        SELECT credential_id, tenant_id, user_id, login_name, password_hash, password_algo,
                               password_version, auth_version, failed_count, locked_until,
                               last_login_at, last_login_ip, status
                        FROM user_credential
                        WHERE tenant_id = ? AND user_id = ?
                        LIMIT 1
                        """,
                (rs, rowNum) -> new AuthCredential(
                        rs.getString("credential_id"),
                        rs.getString("tenant_id"),
                        rs.getString("user_id"),
                        rs.getString("login_name"),
                        rs.getString("password_hash"),
                        rs.getString("password_algo"),
                        rs.getLong("password_version"),
                        rs.getLong("auth_version"),
                        rs.getInt("failed_count"),
                        toLocalDateTime(rs.getTimestamp("locked_until")),
                        toLocalDateTime(rs.getTimestamp("last_login_at")),
                        rs.getString("last_login_ip"),
                        rs.getString("status")
                ),
                tenantId,
                userId
        );
        return credentials.stream().findFirst();
    }

    public void recordFailedLogin(String credentialId, int failedCount, LocalDateTime lockedUntil) {
        jdbcTemplate.update(
                """
                        UPDATE user_credential
                        SET failed_count = ?, locked_until = ?, updated_at = CURRENT_TIMESTAMP(3)
                        WHERE credential_id = ?
                        """,
                failedCount,
                lockedUntil == null ? null : Timestamp.valueOf(lockedUntil),
                credentialId
        );
    }

    public void recordSuccessfulLogin(String credentialId, String lastLoginIp) {
        jdbcTemplate.update(
                """
                        UPDATE user_credential
                        SET failed_count = 0,
                            locked_until = NULL,
                            last_login_at = CURRENT_TIMESTAMP(3),
                            last_login_ip = ?,
                            updated_at = CURRENT_TIMESTAMP(3)
                        WHERE credential_id = ?
                        """,
                lastLoginIp,
                credentialId
        );
    }

    public void incrementAuthVersion(String credentialId) {
        jdbcTemplate.update(
                """
                        UPDATE user_credential
                        SET auth_version = auth_version + 1,
                            updated_at = CURRENT_TIMESTAMP(3)
                        WHERE credential_id = ?
                        """,
                credentialId
        );
    }

    public void updatePassword(String credentialId, String passwordHash, String passwordAlgo) {
        jdbcTemplate.update(
                """
                        UPDATE user_credential
                        SET password_hash = ?,
                            password_algo = ?,
                            password_version = password_version + 1,
                            auth_version = auth_version + 1,
                            failed_count = 0,
                            locked_until = NULL,
                            updated_at = CURRENT_TIMESTAMP(3)
                        WHERE credential_id = ?
                        """,
                passwordHash,
                passwordAlgo,
                credentialId
        );
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
