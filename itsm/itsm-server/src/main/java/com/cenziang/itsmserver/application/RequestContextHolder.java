package com.cenziang.itsmserver.application;

import com.cenziang.itsmcommon.api.BusinessException;
import com.cenziang.itsmcommon.api.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 请求上下文解析工具。
 * <p>
 * 从安全过滤器写入的请求属性中读取当前身份，避免业务接口信任前端传入的可伪造头。
 * </p>
 */
public final class RequestContextHolder {
    private RequestContextHolder() {
    }

    /**
     * 从请求中解析当前身份，并校验租户头与令牌一致。
     */
    public static RequestContext resolve(HttpServletRequest request, String headerTenantId) {
        String userId = (String) request.getAttribute("auth.userId");
        String tokenTenantId = (String) request.getAttribute("auth.tenantId");
        if (userId == null || tokenTenantId == null) {
            throw new BusinessException(ErrorCode.AUTH_REQUIRED);
        }
        if (headerTenantId != null && !headerTenantId.equals(tokenTenantId)) {
            throw new BusinessException(ErrorCode.TENANT_FORBIDDEN);
        }
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) request.getAttribute("auth.roles");
        return new RequestContext(
                tokenTenantId,
                userId,
                roles == null ? List.of() : roles,
                (String) request.getAttribute("auth.permissionsVersion"),
                (Long) request.getAttribute("auth.authVersion")
        );
    }

    /**
     * 校验当前用户是否具备某个角色。
     */
    public static void requireRole(RequestContext context, String role) {
        if (context.roles() == null || !context.roles().contains(role)) {
            throw new BusinessException(ErrorCode.ROLE_FORBIDDEN);
        }
    }
}