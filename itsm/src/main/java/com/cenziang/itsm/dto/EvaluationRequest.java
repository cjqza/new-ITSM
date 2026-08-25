package com.cenziang.itsm.dto;

public record EvaluationRequest(
        int rating,
        String comment
) {
}
