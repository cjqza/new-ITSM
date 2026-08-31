package com.cenziang.itsmpojo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * 认证相关 DTO。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class AuthDtos {
    private AuthDtos() {
    }

    /**
     * 登录请求。
     *
     * @param grantType   授权类型
     * @param ssoCode     一次性授权码
     * @param redirectUri 回调地址
     */
    public record LoginRequest(
            String grantType,
            String ssoCode,
            String redirectUri
    ) {
    }

    /**
     * 用户摘要。
     *
     * @param userId         用户主键
     * @param displayName    展示名称
     * @param departmentName 部门名称
     */
    public record UserSummary(
            String userId,
            String displayName,
            String departmentName
    ) {
    }

    /**
     * 租户摘要。
     *
     * @param tenantId   租户主键
     * @param tenantName 租户名称
     */
    public record TenantSummary(
            String tenantId,
            String tenantName
    ) {
    }

    /**
     * 登录响应。
     *
     * @param accessToken       访问令牌
     * @param refreshToken      刷新令牌
     * @param tokenType         token 类型
     * @param expiresIn         过期秒数
     * @param user              用户摘要
     * @param tenant            租户摘要
     * @param roles             角色编码列表
     * @param permissionsVersion 权限版本
     */
    public record LoginResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresIn,
            UserSummary user,
            TenantSummary tenant,
            List<String> roles,
            String permissionsVersion
    ) {
    }

    /**
     * 当前登录用户响应。
     *
     * @param userId             用户主键
     * @param displayName        展示名称
     * @param departmentName     部门名称
     * @param tenantId           租户主键
     * @param roles              角色编码列表
     * @param permissionsVersion 权限版本
     */
    public record CurrentUserResponse(
            String userId,
            String displayName,
            String departmentName,
            String tenantId,
            List<String> roles,
            String permissionsVersion
    ) {
    }
}
