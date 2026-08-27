package com.cenziang.itsmserver.api;

import com.cenziang.itsmcommon.api.ApiResponse;
import com.cenziang.itsmpojo.dto.PermissionRequestDtos;
import com.cenziang.itsmserver.service.PermissionApprovalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 权限申请与审批接口。
 */
@Tag(name = "权限审批", description = "用户提交权限申请，管理员审批并授权")
@RestController
public class PermissionApprovalController extends ControllerSupport {
    private final PermissionApprovalService approvalService;

    public PermissionApprovalController(PermissionApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @Operation(summary = "提交权限申请", description = "普通用户申请 ITSM 权限或管理员权限")
    @PostMapping("/api/v1/permissions/requests")
    public ApiResponse<PermissionRequestDtos.PermissionRequestResponse> submit(@Parameter(description = "租户 ID", required = true) @RequestHeader("X-Tenant-Id") String tenantId,
                                                                               @Valid @RequestBody PermissionRequestDtos.SubmitPermissionRequest request,
                                                                               HttpServletRequest httpServletRequest) {
        return ok(approvalService.submit(context(httpServletRequest, tenantId), request), httpServletRequest);
    }

    @Operation(summary = "查询我的申请", description = "返回当前用户的权限申请记录")
    @GetMapping("/api/v1/permissions/requests/my")
    public ApiResponse<List<PermissionRequestDtos.PermissionRequestView>> my(@Parameter(description = "租户 ID", required = true) @RequestHeader("X-Tenant-Id") String tenantId,
                                                                             HttpServletRequest httpServletRequest) {
        return ok(approvalService.listMy(context(httpServletRequest, tenantId)), httpServletRequest);
    }

    @Operation(summary = "待审批列表", description = "管理员查看待审批的权限申请")
    @GetMapping("/api/v1/admin/permissions/requests")
    public ApiResponse<List<PermissionRequestDtos.PermissionRequestView>> pending(@Parameter(description = "租户 ID", required = true) @RequestHeader("X-Tenant-Id") String tenantId,
                                                                                  HttpServletRequest httpServletRequest) {
        return ok(approvalService.listPending(context(httpServletRequest, tenantId)), httpServletRequest);
    }

    @Operation(summary = "批准申请", description = "管理员批准权限申请并授予对应角色")
    @PostMapping("/api/v1/admin/permissions/requests/{requestId}/approve")
    public ApiResponse<PermissionRequestDtos.PermissionRequestResponse> approve(@Parameter(description = "租户 ID", required = true) @RequestHeader("X-Tenant-Id") String tenantId,
                                                                                @Parameter(description = "申请 ID", required = true) @PathVariable String requestId,
                                                                                HttpServletRequest httpServletRequest) {
        return ok(approvalService.approve(context(httpServletRequest, tenantId), requestId), httpServletRequest);
    }

    @Operation(summary = "驳回申请", description = "管理员驳回权限申请")
    @PostMapping("/api/v1/admin/permissions/requests/{requestId}/reject")
    public ApiResponse<PermissionRequestDtos.PermissionRequestResponse> reject(@Parameter(description = "租户 ID", required = true) @RequestHeader("X-Tenant-Id") String tenantId,
                                                                               @Parameter(description = "申请 ID", required = true) @PathVariable String requestId,
                                                                               HttpServletRequest httpServletRequest) {
        return ok(approvalService.reject(context(httpServletRequest, tenantId), requestId), httpServletRequest);
    }
}
