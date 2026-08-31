package com.cenziang.itsmpojo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 部门管理相关 DTO。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class DepartmentDtos {
    private DepartmentDtos() {
    }

    @Schema(description = "部门视图")
    public record DepartmentView(
            @Schema(description = "部门主键") String departmentId,
            @Schema(description = "部门名称") String name,
            @Schema(description = "部门描述") String description,
            @Schema(description = "是否启用") boolean enabled
    ) {
    }

    @Schema(description = "创建部门请求")
    public record DepartmentCreateRequest(
            @Schema(description = "部门名称") String name,
            @Schema(description = "部门描述") String description
    ) {
    }

    @Schema(description = "修改部门请求")
    public record DepartmentUpdateRequest(
            @Schema(description = "部门名称") String name,
            @Schema(description = "部门描述") String description,
            @Schema(description = "是否启用") Boolean enabled
    ) {
    }
}
