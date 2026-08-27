package com.cenziang.itsmserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "登录响应")
public record AuthLoginResponse(
        @Schema(description = "访问令牌") String accessToken,
        @Schema(description = "刷新令牌") String refreshToken,
        @Schema(description = "令牌类型") String tokenType,
        @Schema(description = "过期秒数") long expiresIn,
        @Schema(description = "用户摘要") UserSummary user,
        @Schema(description = "租户摘要") TenantSummary tenant,
        @Schema(description = "角色编码列表") List<String> roles,
        @Schema(description = "权限版本") String permissionsVersion
) {
    @Schema(description = "用户摘要")
    public record UserSummary(
            @Schema(description = "用户主键") String userId,
            @Schema(description = "展示名称") String displayName,
            @Schema(description = "部门名称") String departmentName
    ) {
    }

    @Schema(description = "租户摘要")
    public record TenantSummary(
            @Schema(description = "租户主键") String tenantId,
            @Schema(description = "租户名称") String tenantName
    ) {
    }
}