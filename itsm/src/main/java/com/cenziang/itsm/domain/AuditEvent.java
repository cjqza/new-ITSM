package com.cenziang.itsm.domain;

import java.time.Instant;

public record AuditEvent(
        String action,
        String operatorId,
        String detail,
        Instant occurredAt
) {
}
