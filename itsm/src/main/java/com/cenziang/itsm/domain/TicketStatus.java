package com.cenziang.itsm.domain;

public enum TicketStatus {
    SUBMITTED,
    AGENT_PROCESSING,
    AGENT_ANSWERED,
    PENDING_HUMAN,
    ACCEPTED,
    TECH_ANALYSIS,
    IN_SUPPORT,
    PENDING_USER_CONFIRM,
    RESOLVED,
    CLOSED,
    REOPENED,
    CANCELLED
}
