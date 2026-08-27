package com.cenziang.itsmserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "当前登录用户响应")
public record AuthMeResponse(
        @Schema(description = "用户主键") String userId,
        @Schema(description = "展示名称") String displayName,
        @Schema(description = "部门名称") String departmentName,
        @Schema(description = "租户主键") String tenantId,
        @Schema(description = "角色编码列表") List<String> roles,
        @Schema(description = "权限版本") String permissionsVersion
) {
}