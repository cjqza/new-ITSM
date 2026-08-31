package com.cenziang.itsmpojo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 同事消息相关 DTO。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ColleagueMessageDtos {
    private ColleagueMessageDtos() {
    }

    @Schema(description = "发送同事消息请求")
    public record SendRequest(
            @Schema(description = "接收方用户主键") String toUserId,
            @Schema(description = "消息内容") String content
    ) {
    }

    @Schema(description = "同事消息视图")
    public record MessageView(
            @Schema(description = "消息主键") Long id,
            @Schema(description = "发送方用户主键") String fromUserId,
            @Schema(description = "接收方用户主键") String toUserId,
            @Schema(description = "消息内容") String content,
            @Schema(description = "发送时间") LocalDateTime createdAt
    ) {
    }

    @Schema(description = "同事会话视图（左侧聊天列表项）")
    public record ConversationView(
            @Schema(description = "对方用户主键") String userId,
            @Schema(description = "对方展示名称") String displayName,
            @Schema(description = "对方部门名称") String departmentName,
            @Schema(description = "最后一条消息内容") String lastMessage,
            @Schema(description = "最后一条消息时间") LocalDateTime lastMessageAt,
            @Schema(description = "未读数量") long unreadCount
    ) {
    }

    @Schema(description = "同事消息分页视图")
    public record MessagePage(
            @Schema(description = "消息列表（按时间升序）") List<MessageView> items,
            @Schema(description = "是否还有更早的消息") boolean hasMore
    ) {
    }
}
