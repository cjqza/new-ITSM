package com.cenziang.itsmserver.domain;

import java.time.LocalDateTime;

public record AuthCredential(
        String credentialId,
        String tenantId,
        String userId,
        String loginName,
        String passwordHash,
        String passwordAlgo,
        long passwordVersion,
        long authVersion,
        int failedCount,
        LocalDateTime lockedUntil,
        LocalDateTime lastLoginAt,
        String lastLoginIp,
        String status
) {
    public boolean isLocked(LocalDateTime now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }
}
