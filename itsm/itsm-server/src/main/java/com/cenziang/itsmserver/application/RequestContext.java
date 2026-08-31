package com.cenziang.itsmserver.application;


import java.util.List;

public record RequestContext(
        String tenantId,
        String userId,
        List<String> roles,
        String permissionsVersion,
        Long authVersion
) {
}
