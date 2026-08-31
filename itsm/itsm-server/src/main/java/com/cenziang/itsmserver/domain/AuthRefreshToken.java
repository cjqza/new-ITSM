package com.cenziang.itsmserver.domain;

import java.time.LocalDateTime;

public record AuthRefreshToken(
        String tokenId,
        String tenantId,
        String userId,
        String tokenHash,
        LocalDateTime expiresAt,
        LocalDateTime revokedAt,
        String replacedBy,
        long authVersion
) {
    public boolean isActive() {
        return revokedAt == null && expiresAt.isAfter(LocalDateTime.now());
    }
}
