package com.cenziang.itsmserver.domain;

public record AuthTenant(
        String tenantId,
        String tenantName,
        boolean enabled
) {
}
