package com.cenziang.itsm.domain;

public enum TicketPriority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT;

    public static TicketPriority from(String value) {
        if (value == null || value.isBlank()) {
            return MEDIUM;
        }
        return TicketPriority.valueOf(value.trim().toUpperCase());
    }
}
