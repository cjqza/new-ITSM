package com.cenziang.itsmserver.api;

import com.cenziang.itsmcommon.api.ApiResponse;
import com.cenziang.itsmcommon.api.PageResponse;
import com.cenziang.itsmpojo.dto.EmployeeDtos;
import com.cenziang.itsmserver.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 员工账号管理接口（仅管理员）。
 */
@Tag(name = "员工账号", description = "管理员查看并修改员工账号的手机号、邮箱与部门")
@RestController
@RequestMapping("/api/v1/admin/employees")
public class EmployeeController extends ControllerSupport {
    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @Operation(summary = "分页条件查询员工账号", description = "支持按关键字、部门、角色过滤")
    @GetMapping
    public ApiResponse<PageResponse<EmployeeDtos.EmployeeView>> list(@Parameter(description = "租户 ID", required = true) @RequestHeader("X-Tenant-Id") String tenantId,
                                                                      @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
                                                                      @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int pageSize,
                                                                      @Parameter(description = "关键字（姓名/工号/邮箱/手机号）") @RequestParam(required = false) String keyword,
                                                                      @Parameter(description = "部门名称") @RequestParam(required = false) String departmentName,
                                                                      @Parameter(description = "角色编码") @RequestParam(required = false) String role,
                                                                      HttpServletRequest httpServletRequest) {
        return ok(employeeService.listEmployees(context(httpServletRequest, tenantId), page, pageSize, keyword, departmentName, role), httpServletRequest);
    }

    @Operation(summary = "修改员工账号", description = "修改员工的手机号、邮箱与部门")
    @PatchMapping("/{userId}")
    public ApiResponse<EmployeeDtos.EmployeeView> update(@Parameter(description = "租户 ID", required = true) @RequestHeader("X-Tenant-Id") String tenantId,
                                                         @Parameter(description = "用户主键", required = true) @PathVariable("userId") String userId,
                                                         @Valid @RequestBody EmployeeDtos.EmployeeUpdateRequest request,
                                                         HttpServletRequest httpServletRequest) {
        return ok(employeeService.update(context(httpServletRequest, tenantId), userId, request), httpServletRequest);
    }
}
