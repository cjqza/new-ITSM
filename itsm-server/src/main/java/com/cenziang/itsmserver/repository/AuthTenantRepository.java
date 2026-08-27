package com.cenziang.itsmserver.repository;

import com.cenziang.itsmserver.domain.AuthTenant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class AuthTenantRepository {
    private final JdbcTemplate jdbcTemplate;

    public AuthTenantRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<AuthTenant> findEnabledTenantById(String tenantId) {
        List<AuthTenant> tenants = jdbcTemplate.query(
                """
                        SELECT tenant_id, tenant_name, enabled
                        FROM tenant
                        WHERE tenant_id = ? AND enabled = 1
                        LIMIT 1
                        """,
                (rs, rowNum) -> new AuthTenant(
                        rs.getString("tenant_id"),
                        rs.getString("tenant_name"),
                        rs.getBoolean("enabled")
                ),
                tenantId
        );
        return tenants.stream().findFirst();
    }

    public String findPermissionsVersion(String tenantId) {
        return jdbcTemplate.queryForObject(
                "SELECT permissions_version FROM tenant WHERE tenant_id = ?",
                String.class,
                tenantId
        );
    }

    public long findAuthVersion(String tenantId) {
        Long version = jdbcTemplate.queryForObject(
                "SELECT auth_version FROM tenant WHERE tenant_id = ?",
                Long.class,
                tenantId
        );
        return version == null ? 1L : version;
    }
}
