package com.cenziang.itsmserver.domain;

import java.time.LocalDateTime;

public record AuthUser(
        String userId,
        String tenantId,
        String displayName,
        String departmentName,
        String contactPhone,
        String contactEmail,
        boolean enabled
) {
}
