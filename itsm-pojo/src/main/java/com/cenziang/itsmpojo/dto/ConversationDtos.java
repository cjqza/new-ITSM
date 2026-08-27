package com.cenziang.itsmpojo.dto;

import com.cenziang.itsmcommon.api.PageResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 会话与 Agent 相关 DTO。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ConversationDtos {
    private ConversationDtos() {
    }

    @Schema(description = "创建会话请求")
    public record CreateSessionRequest(
            @Schema(description = "会话渠道") String channel,
            @Schema(description = "会话主题") String subject
    ) {
    }

    @Schema(description = "会话创建响应")
    public record SessionCreateResponse(
            @Schema(description = "会话主键") String sessionId,
            @Schema(description = "会话状态") String status,
            @Schema(description = "关联工单") String ticketId,
            @Schema(description = "创建时间") LocalDateTime createdAt,
            @Schema(description = "最近消息时间") LocalDateTime lastMessageAt
    ) {
    }

    @Schema(description = "会话列表项")
    public record SessionListItem(
            @Schema(description = "会话主键") String sessionId,
            @Schema(description = "会话所属用户") String userId,
            @Schema(description = "会话渠道") String channel,
            @Schema(description = "会话主题") String subject,
            @Schema(description = "会话状态") String status,
            @Schema(description = "会话摘要") String summary,
            @Schema(description = "关联工单") String ticketId,
            @Schema(description = "最近消息时间") LocalDateTime lastMessageAt,
            @Schema(description = "创建时间") LocalDateTime createdAt
    ) {
    }

    @Schema(description = "消息附件引用")
    public record MessageAttachment(
            @Schema(description = "文件主键") String fileId,
            @Schema(description = "文件名称") String fileName
    ) {
    }

    @Schema(description = "会话消息项")
    public record SessionMessageItem(
            @Schema(description = "消息主键") String messageId,
            @Schema(description = "发送方类型") String senderType,
            @Schema(description = "消息内容") String content,
            @Schema(description = "创建时间") LocalDateTime createdAt
    ) {
    }

    @Schema(description = "会话详情响应")
    public record SessionDetailResponse(
            @Schema(description = "会话主键") String sessionId,
            @Schema(description = "会话状态") String status,
            @Schema(description = "关联工单") String ticketId,
            @Schema(description = "会话摘要") String summary,
            @Schema(description = "消息分页") PageResponse<SessionMessageItem> messages
    ) {
    }

    @Schema(description = "发送消息请求")
    public record SendMessageRequest(
            @Schema(description = "前端消息唯一 ID") String clientMessageId,
            @Schema(description = "消息内容") String content,
            @Schema(description = "附件引用") List<MessageAttachment> attachments
    ) {
    }

    @Schema(description = "Agent 返回消息")
    public record AgentMessageView(
            @Schema(description = "消息主键") String messageId,
            @Schema(description = "消息内容") String content,
            @Schema(description = "置信度") BigDecimal confidence,
            @Schema(description = "来源摘要") String sourceSummary
    ) {
    }

    @Schema(description = "发送消息响应")
    public record SendMessageResponse(
            @Schema(description = "用户消息主键") String messageId,
            @Schema(description = "会话主键") String sessionId,
            @Schema(description = "结果类型") String outcome,
            @Schema(description = "Agent 回复") AgentMessageView agentMessage,
            @Schema(description = "关联工单") String ticketId,
            @Schema(description = "会话状态") String sessionStatus
    ) {
    }

    @Schema(description = "Agent 决策请求")
    public record AgentDecisionRequest(
            @Schema(description = "决策类型") String decision,
            @Schema(description = "自助答复") String answer,
            @Schema(description = "置信度") BigDecimal confidence,
            @Schema(description = "业务线编码") String businessLineCode,
            @Schema(description = "摘要") String summary,
            @Schema(description = "转人工原因") String handoffReason,
            @Schema(description = "建议管理单元") String suggestedManagementUnitId,
            @Schema(description = "建议症状") String suggestedSymptomId
    ) {
    }

    @Schema(description = "Agent 决策响应")
    public record AgentDecisionResponse(
            @Schema(description = "会话主键") String sessionId,
            @Schema(description = "决策类型") String decision,
            @Schema(description = "会话状态") String sessionStatus,
            @Schema(description = "关联工单") String ticketId,
            @Schema(description = "工单状态") String ticketStatus,
            @Schema(description = "是否按事实落库") Boolean acceptedAsFact
    ) {
    }

    @Schema(description = "转人工请求")
    public record HandoffRequest(
            @Schema(description = "转人工原因") String reason,
            @Schema(description = "业务线编码") String businessLineCode
    ) {
    }
}