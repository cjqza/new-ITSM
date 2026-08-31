package com.cenziang.itsmserver.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class AuthLoginAuditRepository {
    private final JdbcTemplate jdbcTemplate;

    public AuthLoginAuditRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void record(String tenantId,
                       String userId,
                       String loginName,
                       String result,
                       String reason,
                       String clientIp,
                       String userAgent,
                       String traceId) {
        jdbcTemplate.update(
                """
                        INSERT INTO auth_login_audit (
                            audit_id, tenant_id, user_id, login_name, result, reason, client_ip, user_agent, trace_id
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                UUID.randomUUID().toString(),
                tenantId,
                userId,
                loginName,
                result,
                reason,
                clientIp,
                userAgent,
                traceId
        );
    }
}
