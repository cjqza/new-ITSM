package com.cenziang.itsmserver.api;

import com.cenziang.itsmcommon.api.ApiResponse;
import com.cenziang.itsmpojo.dto.DepartmentDtos;
import com.cenziang.itsmserver.service.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 部门管理接口（仅管理员）。
 */
@Tag(name = "部门管理", description = "管理员对部门进行增删改查")
@RestController
@RequestMapping("/api/v1/admin/departments")
public class DepartmentController extends ControllerSupport {
    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @Operation(summary = "查询部门列表")
    @GetMapping
    public ApiResponse<List<DepartmentDtos.DepartmentView>> list(@Parameter(description = "租户 ID", required = true) @RequestHeader("X-Tenant-Id") String tenantId,
                                                                 HttpServletRequest httpServletRequest) {
        return ok(departmentService.list(context(httpServletRequest, tenantId)), httpServletRequest);
    }

    @Operation(summary = "创建部门")
    @PostMapping
    public ApiResponse<DepartmentDtos.DepartmentView> create(@Parameter(description = "租户 ID", required = true) @RequestHeader("X-Tenant-Id") String tenantId,
                                                             @Valid @RequestBody DepartmentDtos.DepartmentCreateRequest request,
                                                             HttpServletRequest httpServletRequest) {
        return ok(departmentService.create(context(httpServletRequest, tenantId), request), httpServletRequest);
    }

    @Operation(summary = "修改部门")
    @PatchMapping("/{departmentId}")
    public ApiResponse<DepartmentDtos.DepartmentView> update(@Parameter(description = "租户 ID", required = true) @RequestHeader("X-Tenant-Id") String tenantId,
                                                             @Parameter(description = "部门主键", required = true) @PathVariable String departmentId,
                                                             @Valid @RequestBody DepartmentDtos.DepartmentUpdateRequest request,
                                                             HttpServletRequest httpServletRequest) {
        return ok(departmentService.update(context(httpServletRequest, tenantId), departmentId, request), httpServletRequest);
    }

    @Operation(summary = "删除部门")
    @DeleteMapping("/{departmentId}")
    public ApiResponse<Void> delete(@Parameter(description = "租户 ID", required = true) @RequestHeader("X-Tenant-Id") String tenantId,
                                    @Parameter(description = "部门主键", required = true) @PathVariable String departmentId,
                                    HttpServletRequest httpServletRequest) {
        departmentService.delete(context(httpServletRequest, tenantId), departmentId);
        return ok(null, httpServletRequest);
    }
}
