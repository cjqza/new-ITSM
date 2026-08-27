package com.cenziang.itsmpojo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 权限申请审批相关 DTO。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class PermissionRequestDtos {
    private PermissionRequestDtos() {
    }

    @Schema(description = "权限申请类型")
    public static final String ITSM_ACCESS = "ITSM_ACCESS";
    public static final String ADMIN = "ADMIN";

    @Schema(description = "提交权限申请请求")
    public record SubmitPermissionRequest(
            @Schema(description = "申请类型：ITSM_ACCESS 或 ADMIN") String requestType,
            @Schema(description = "申请原因") String reason
    ) {
    }

    @Schema(description = "权限申请视图")
    public record PermissionRequestView(
            @Schema(description = "申请主键") String requestId,
            @Schema(description = "申请人主键") String requesterId,
            @Schema(description = "申请人姓名") String requesterName,
            @Schema(description = "申请类型") String requestType,
            @Schema(description = "状态") String status,
            @Schema(description = "申请原因") String reason,
            @Schema(description = "提交时间") LocalDateTime createdAt
    ) {
    }

    @Schema(description = "权限申请处理响应")
    public record PermissionRequestResponse(
            @Schema(description = "申请主键") String requestId,
            @Schema(description = "状态") String status
    ) {
    }
}
