package com.cenziang.itsm.domain;

import java.time.Instant;

public record StatusHistoryEntry(
        TicketStatus fromStatus,
        TicketStatus toStatus,
        String operatorId,
        String reason,
        Instant changedAt
) {
}
