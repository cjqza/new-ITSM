package com.cenziang.itsm.dto;

public record SupportCommandRequest(
        String operatorId,
        String note
) {
}
