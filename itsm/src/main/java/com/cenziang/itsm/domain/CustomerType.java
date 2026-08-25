package com.cenziang.itsm.domain;

public enum CustomerType {
    INTERNAL,
    EXTERNAL;

    public static CustomerType from(String value) {
        if (value == null || value.isBlank()) {
            return INTERNAL;
        }
        return CustomerType.valueOf(value.trim().toUpperCase());
    }
}
