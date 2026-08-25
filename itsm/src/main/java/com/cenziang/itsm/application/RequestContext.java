package com.cenziang.itsm.application;

import com.cenziang.itsm.domain.CustomerType;

public record RequestContext(
        String tenantId,
        String actorId,
        CustomerType customerType,
        String role
) {
}
