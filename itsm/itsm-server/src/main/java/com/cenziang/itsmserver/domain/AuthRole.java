package com.cenziang.itsmserver.domain;

public record AuthRole(
        String roleId,
        String tenantId,
        String roleCode,
        String roleName,
        boolean enabled
) {
}
