package com.cenziang.itsmpojo.dto;

import com.cenziang.itsmcommon.api.PageResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 权限相关 DTO。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class PermissionDtos {
    private PermissionDtos() {
    }

    @Schema(description = "数据范围视图")
    public record DataScopeView(
            @Schema(description = "范围类型") String scopeType,
            @Schema(description = "业务线编码") List<String> businessLineCodes,
            @Schema(description = "用户范围") List<String> userIds
    ) {
    }

    @Schema(description = "角色摘要")
    public record RoleSummary(
            @Schema(description = "角色主键") String roleId,
            @Schema(description = "角色编码") String roleCode,
            @Schema(description = "角色名称") String roleName,
            @Schema(description = "是否启用") Boolean enabled,
            @Schema(description = "角色说明") String description,
            @Schema(description = "权限数量") Integer permissionCount
    ) {
    }

    @Schema(description = "权限点")
    public record PermissionItem(
            @Schema(description = "权限编码") String permissionCode,
            @Schema(description = "权限名称") String permissionName,
            @Schema(description = "权限类型") String permissionType
    ) {
    }

    @Schema(description = "当前用户权限响应")
    public record PermissionMeResponse(
            @Schema(description = "用户主键") String userId,
            @Schema(description = "租户主键") String tenantId,
            @Schema(description = "角色摘要列表") List<RoleSummary> roles,
            @Schema(description = "权限编码列表") List<String> permissions,
            @Schema(description = "菜单编码列表") List<String> menus,
            @Schema(description = "数据范围") DataScopeView dataScope,
            @Schema(description = "权限版本") String permissionsVersion
    ) {
    }

    @Schema(description = "角色权限响应")
    public record RolePermissionResponse(
            @Schema(description = "角色主键") String roleId,
            @Schema(description = "角色编码") String roleCode,
            @Schema(description = "权限点列表") List<PermissionItem> permissions,
            @Schema(description = "菜单编码列表") List<String> menus,
            @Schema(description = "数据范围") DataScopeView dataScope
    ) {
    }

    @Schema(description = "角色分页查询参数")
    public record RolePageQuery(
            @Schema(description = "仅启用") Boolean enabledOnly,
            @Schema(description = "页码") Integer page,
            @Schema(description = "页大小") Integer pageSize,
            @Schema(description = "关键字") String keyword
    ) {
    }

    @Schema(description = "角色分页响应")
    public record RolePageResponse(
            @Schema(description = "当前页角色") PageResponse<RoleSummary> items,
            @Schema(description = "页码") long page,
            @Schema(description = "页大小") long pageSize,
            @Schema(description = "总数") long total,
            @Schema(description = "是否有下一页") boolean hasNext
    ) {
    }
}