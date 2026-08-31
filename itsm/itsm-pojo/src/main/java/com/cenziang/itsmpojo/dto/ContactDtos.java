package com.cenziang.itsmpojo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 企业联系人相关 DTO。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ContactDtos {
    private ContactDtos() {
    }

    @Schema(description = "联系人视图")
    public record ContactView(
            @Schema(description = "联系人主键") String contactId,
            @Schema(description = "用户主键") String userId,
            @Schema(description = "展示名称") String displayName,
            @Schema(description = "部门名称") String departmentName,
            @Schema(description = "邮箱") String email,
            @Schema(description = "手机号") String phone,
            @Schema(description = "是否启用") boolean enabled
    ) {
    }
}
