package com.cenziang.itsmpojo.dto;

import com.cenziang.itsmcommon.api.PageResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工单相关 DTO。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class TicketDtos {
    private TicketDtos() {
    }

    @Schema(description = "请求人视图")
    public record TicketRequesterView(
            @Schema(description = "用户主键") String userId,
            @Schema(description = "展示名称") String displayName,
            @Schema(description = "部门名称") String departmentName
    ) {
    }

    @Schema(description = "处理人视图")
    public record TicketAssigneeView(
            @Schema(description = "用户主键") String userId,
            @Schema(description = "展示名称") String displayName
    ) {
    }

    @Schema(description = "分类视图")
    public record TicketClassificationView(
            @Schema(description = "管理单元") String managementUnitId,
            @Schema(description = "症状") String symptomId,
            @Schema(description = "原因") String reasonId,
            @Schema(description = "解决方法") String solutionMethodId,
            @Schema(description = "自定义原因") String customReason,
            @Schema(description = "自定义解决说明") String customSolution,
            @Schema(description = "版本号") Long version
    ) {
    }

    @Schema(description = "会话摘要视图")
    public record TicketConversationView(
            @Schema(description = "会话主键") String sessionId,
            @Schema(description = "会话摘要") String summary,
            @Schema(description = "消息总数") Integer messageCount
    ) {
    }

    @Schema(description = "状态历史视图")
    public record TicketStatusHistoryView(
            @Schema(description = "状态") String status,
            @Schema(description = "发生时间") LocalDateTime occurredAt,
            @Schema(description = "操作者") String operator,
            @Schema(description = "备注") String note
    ) {
    }

    @Schema(description = "审计事件视图")
    public record TicketAuditView(
            @Schema(description = "动作") String action,
            @Schema(description = "发生时间") LocalDateTime occurredAt,
            @Schema(description = "行为人") String actor
    ) {
    }

    @Schema(description = "评价视图")
    public record TicketRatingView(
            @Schema(description = "评价主键") String ratingId,
            @Schema(description = "分数") Integer score,
            @Schema(description = "标签") List<String> tags,
            @Schema(description = "评价内容") String comment,
            @Schema(description = "提交时间") LocalDateTime submittedAt
    ) {
    }

    @Schema(description = "创建工单请求")
    public record CreateTicketRequest(
            @Schema(description = "来源") String source,
            @Schema(description = "会话主键") String sessionId,
            @Schema(description = "标题") String title,
            @Schema(description = "描述") String description,
            @Schema(description = "业务线编码") String businessLineCode,
            @Schema(description = "优先级") String priority,
            @Schema(description = "环境信息") String environment,
            @Schema(description = "附件引用") List<String> attachments
    ) {
    }

    @Schema(description = "创建工单响应")
    public record CreateTicketResponse(
            @Schema(description = "工单主键") String ticketId,
            @Schema(description = "工单号") String ticketNo,
            @Schema(description = "状态") String status,
            @Schema(description = "业务线编码") String businessLineCode,
            @Schema(description = "请求人") String requesterId,
            @Schema(description = "会话主键") String sessionId,
            @Schema(description = "创建时间") LocalDateTime createdAt
    ) {
    }

    @Schema(description = "工单分页查询参数")
    public record TicketPageQuery(
            @Schema(description = "页码") Integer page,
            @Schema(description = "页大小") Integer pageSize,
            @Schema(description = "状态列表") List<String> status,
            @Schema(description = "工单号") String ticketNo,
            @Schema(description = "请求人") String requesterId,
            @Schema(description = "业务线编码") String businessLineCode,
            @Schema(description = "关键字") String keyword,
            @Schema(description = "排序") String sort
    ) {
    }

    @Schema(description = "工单分页项")
    public record TicketPageItem(
            @Schema(description = "工单主键") String ticketId,
            @Schema(description = "工单号") String ticketNo,
            @Schema(description = "标题") String title,
            @Schema(description = "状态") String status,
            @Schema(description = "优先级") String priority,
            @Schema(description = "业务线编码") String businessLineCode,
            @Schema(description = "处理人") String assigneeId,
            @Schema(description = "关联会话") String sessionId,
            @Schema(description = "更新时间") LocalDateTime updatedAt
    ) {
    }

    @Schema(description = "工单详情响应")
    public record TicketDetailResponse(
            @Schema(description = "工单主键") String ticketId,
            @Schema(description = "工单号") String ticketNo,
            @Schema(description = "租户主键") String tenantId,
            @Schema(description = "请求人") TicketRequesterView requester,
            @Schema(description = "标题") String title,
            @Schema(description = "描述") String description,
            @Schema(description = "来源") String source,
            @Schema(description = "状态") String status,
            @Schema(description = "优先级") String priority,
            @Schema(description = "业务线编码") String businessLineCode,
            @Schema(description = "分类") TicketClassificationView classification,
            @Schema(description = "处理人") TicketAssigneeView assignee,
            @Schema(description = "会话摘要") TicketConversationView conversation,
            @Schema(description = "状态历史") List<TicketStatusHistoryView> statusHistory,
            @Schema(description = "审计事件") List<TicketAuditView> auditEvents,
            @Schema(description = "评价") TicketRatingView rating,
            @Schema(description = "创建时间") LocalDateTime createdAt,
            @Schema(description = "更新时间") LocalDateTime updatedAt
    ) {
    }

    @Schema(description = "客服队列查询参数")
    public record SupportQueueQuery(
            @Schema(description = "视图") String view,
            @Schema(description = "页码") Integer page,
            @Schema(description = "页大小") Integer pageSize,
            @Schema(description = "业务线编码") String businessLineCode,
            @Schema(description = "受理范围") String assignee,
            @Schema(description = "关键字") String keyword
    ) {
    }

    @Schema(description = "客服队列项")
    public record SupportQueueItem(
            @Schema(description = "工单主键") String ticketId,
            @Schema(description = "工单号") String ticketNo,
            @Schema(description = "标题") String title,
            @Schema(description = "状态") String status,
            @Schema(description = "优先级") String priority,
            @Schema(description = "业务线编码") String businessLineCode,
            @Schema(description = "请求人") TicketRequesterView requester,
            @Schema(description = "处理人") TicketAssigneeView assignee,
            @Schema(description = "更新时间") LocalDateTime updatedAt
    ) {
    }

    @Schema(description = "受理请求")
    public record SupportAcceptRequest(
            @Schema(description = "受理备注") String note
    ) {
    }

    @Schema(description = "转让工单请求")
    public record TransferTicketRequest(
            @Schema(description = "目标客服用户主键") String targetUserId
    ) {
    }

    @Schema(description = "分类更新请求")
    public record ClassificationUpdateRequest(
            @Schema(description = "管理单元") String managementUnitId,
            @Schema(description = "症状") String symptomId,
            @Schema(description = "原因") String reasonId,
            @Schema(description = "解决方法") String solutionMethodId,
            @Schema(description = "自定义原因") String customReason,
            @Schema(description = "自定义解决说明") String customSolution,
            @Schema(description = "版本号") Long version
    ) {
    }

    @Schema(description = "提交解决请求")
    public record ResolveTicketRequest(
            @Schema(description = "解决说明") String resolution,
            @Schema(description = "解决类型") String resolutionType,
            @Schema(description = "解决方法") String solutionMethodId,
            @Schema(description = "自定义解决说明") String customSolution
    ) {
    }

    @Schema(description = "用户确认解决响应")
    public record ConfirmTicketResponse(
            @Schema(description = "工单主键") String ticketId,
            @Schema(description = "解决时间") LocalDateTime resolvedAt,
            @Schema(description = "是否允许评价") Boolean ratingAllowed
    ) {
    }

    @Schema(description = "关闭工单请求")
    public record CloseTicketRequest(
            @Schema(description = "关闭原因") String closeReason,
            @Schema(description = "备注") String note
    ) {
    }

    @Schema(description = "关闭工单响应")
    public record CloseTicketResponse(
            @Schema(description = "工单主键") String ticketId,
            @Schema(description = "工单状态") String status,
            @Schema(description = "关闭时间") LocalDateTime closedAt,
            @Schema(description = "关闭人") String closedBy,
            @Schema(description = "是否允许评价") Boolean ratingAllowed
    ) {
    }

    @Schema(description = "重开工单请求")
    public record ReopenTicketRequest(
            @Schema(description = "原因") String reason,
            @Schema(description = "补充说明") String additionalDescription
    ) {
    }

    @Schema(description = "重开工单响应")
    public record ReopenTicketResponse(
            @Schema(description = "工单主键") String ticketId,
            @Schema(description = "工单状态") String status,
            @Schema(description = "重开时间") LocalDateTime reopenedAt,
            @Schema(description = "重开人") String reopenedBy
    ) {
    }
}