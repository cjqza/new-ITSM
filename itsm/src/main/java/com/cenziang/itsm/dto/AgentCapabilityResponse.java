package com.cenziang.itsm.dto;

import java.util.List;

public record AgentCapabilityResponse(
        String provider,
        String implementationStatus,
        List<String> reservedCapabilities,
        List<String> forbiddenCapabilities
) {
}
