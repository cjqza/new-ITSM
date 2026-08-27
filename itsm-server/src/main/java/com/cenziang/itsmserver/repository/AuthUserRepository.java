package com.cenziang.itsmserver.repository;

import com.cenziang.itsmserver.domain.AuthRole;
import com.cenziang.itsmserver.domain.AuthUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class AuthUserRepository {
    private final JdbcTemplate jdbcTemplate;

    public AuthUserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<AuthUser> findUserByTenantAndLoginName(String tenantId, String loginName) {
        List<AuthUser> users = jdbcTemplate.query(
                """
                        SELECT user_id, tenant_id, display_name, department_name, contact_phone, contact_email, enabled
                        FROM app_user
                        WHERE tenant_id = ? AND EXISTS (
                            SELECT 1 FROM user_credential
                            WHERE user_credential.tenant_id = app_user.tenant_id
                              AND user_credential.user_id = app_user.user_id
                              AND user_credential.login_name = ?
                        )
                        LIMIT 1
                        """,
                (rs, rowNum) -> new AuthUser(
                        rs.getString("user_id"),
                        rs.getString("tenant_id"),
                        rs.getString("display_name"),
                        rs.getString("department_name"),
                        rs.getString("contact_phone"),
                        rs.getString("contact_email"),
                        rs.getBoolean("enabled")
                ),
                tenantId,
                loginName
        );
        return users.stream().findFirst();
    }

    public List<AuthRole> findRolesByUserId(String tenantId, String userId) {
        return jdbcTemplate.query(
                """
                        SELECT r.role_id, r.tenant_id, r.role_code, r.role_name, r.enabled
                        FROM rbac_role r
                        INNER JOIN app_user_role ur ON ur.role_id = r.role_id AND ur.tenant_id = r.tenant_id
                        WHERE ur.tenant_id = ? AND ur.user_id = ? AND r.enabled = 1
                        ORDER BY r.role_code
                        """,
                (rs, rowNum) -> new AuthRole(
                        rs.getString("role_id"),
                        rs.getString("tenant_id"),
                        rs.getString("role_code"),
                        rs.getString("role_name"),
                        rs.getBoolean("enabled")
                ),
                tenantId,
                userId
        );
    }

    public Optional<AuthUser> findUserById(String tenantId, String userId) {
        List<AuthUser> users = jdbcTemplate.query(
                """
                        SELECT user_id, tenant_id, display_name, department_name, contact_phone, contact_email, enabled
                        FROM app_user
                        WHERE tenant_id = ? AND user_id = ?
                        LIMIT 1
                        """,
                (rs, rowNum) -> new AuthUser(
                        rs.getString("user_id"),
                        rs.getString("tenant_id"),
                        rs.getString("display_name"),
                        rs.getString("department_name"),
                        rs.getString("contact_phone"),
                        rs.getString("contact_email"),
                        rs.getBoolean("enabled")
                ),
                tenantId,
                userId
        );
        return users.stream().findFirst();
    }

    public Optional<AuthUser> findUserByTenantAndEmail(String tenantId, String email) {
        List<AuthUser> users = jdbcTemplate.query(
                """
                        SELECT user_id, tenant_id, display_name, department_name, contact_phone, contact_email, enabled
                        FROM app_user
                        WHERE tenant_id = ? AND contact_email = ?
                        LIMIT 1
                        """,
                (rs, rowNum) -> new AuthUser(
                        rs.getString("user_id"),
                        rs.getString("tenant_id"),
                        rs.getString("display_name"),
                        rs.getString("department_name"),
                        rs.getString("contact_phone"),
                        rs.getString("contact_email"),
                        rs.getBoolean("enabled")
                ),
                tenantId,
                email
        );
        return users.stream().findFirst();
    }
}
