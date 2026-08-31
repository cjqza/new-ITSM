package com.cenziang.itsmpojo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 员工账号管理相关 DTO。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class EmployeeDtos {
    private EmployeeDtos() {
    }

    @Schema(description = "员工账号视图")
    public record EmployeeView(
            @Schema(description = "用户主键") String userId,
            @Schema(description = "展示名称") String displayName,
            @Schema(description = "部门名称") String departmentName,
            @Schema(description = "手机号") String phone,
            @Schema(description = "邮箱") String email,
            @Schema(description = "是否启用") boolean enabled,
            @Schema(description = "角色编码列表") List<String> roles
    ) {
    }

    @Schema(description = "员工账号修改请求")
    public record EmployeeUpdateRequest(
            @Schema(description = "部门名称") String departmentName,
            @Schema(description = "手机号") String phone,
            @Schema(description = "邮箱") String email
    ) {
    }
}
