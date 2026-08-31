package com.cenziang.itsmserver.api;

import com.cenziang.itsmcommon.api.ApiResponse;
import com.cenziang.itsmcommon.api.PageResponse;
import com.cenziang.itsmpojo.dto.PermissionDtos;
import com.cenziang.itsmserver.application.RequestContext;
import com.cenziang.itsmserver.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 权限查询接口。
 */
@Tag(name = "权限", description = "当前用户权限、角色列表和角色权限查询")
@RestController
public class PermissionController extends ControllerSupport {
    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Operation(summary = "查询当前用户权限", description = "返回当前用户角色、菜单、按钮和数据范围")
    @GetMapping("/api/v1/permissions/me")
    public ApiResponse<PermissionDtos.PermissionMeResponse> me(@Parameter(description = "租户 ID", required = true) @RequestHeader("X-Tenant-Id") String tenantId,
                                                               HttpServletRequest httpServletRequest) {
        return ok(permissionService.me(context(httpServletRequest, tenantId)), httpServletRequest);
    }

    @Operation(summary = "查询角色列表", description = "管理员查看当前租户可用角色及启用状态")
    @GetMapping("/api/v1/admin/roles")
    public ApiResponse<PageResponse<PermissionDtos.RoleSummary>> roles(@Parameter(description = "租户 ID", required = true) @RequestHeader("X-Tenant-Id") String tenantId,
                                                                       @Parameter(description = "是否仅启用") @RequestParam(defaultValue = "true") boolean enabledOnly,
                                                                       @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
                                                                       @Parameter(description = "页大小") @RequestParam(defaultValue = "20") int pageSize,
                                                                       @Parameter(description = "关键字") @RequestParam(required = false) String keyword,
                                                                       HttpServletRequest httpServletRequest) {
        RequestContext context = context(httpServletRequest, tenantId);
        return ok(permissionService.listRoles(context, new PermissionDtos.RolePageQuery(enabledOnly, page, pageSize, keyword)), httpServletRequest);
    }

    @Operation(summary = "查询角色权限", description = "查看角色包含的菜单、按钮和数据权限")
    @GetMapping("/api/v1/admin/roles/{roleId}/permissions")
    public ApiResponse<PermissionDtos.RolePermissionResponse> rolePermissions(@Parameter(description = "租户 ID", required = true) @RequestHeader("X-Tenant-Id") String tenantId,
                                                                              @Parameter(description = "角色 ID", required = true) @PathVariable String roleId,
                                                                              HttpServletRequest httpServletRequest) {
        return ok(permissionService.rolePermissions(context(httpServletRequest, tenantId), roleId), httpServletRequest);
    }
}