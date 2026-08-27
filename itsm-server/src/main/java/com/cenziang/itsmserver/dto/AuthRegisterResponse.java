package com.cenziang.itsmserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "注册响应")
public record AuthRegisterResponse(
        @Schema(description = "用户主键") String userId,
        @Schema(description = "租户主键") String tenantId,
        @Schema(description = "登录账号") String loginName,
        @Schema(description = "展示名称") String displayName
) {
}
