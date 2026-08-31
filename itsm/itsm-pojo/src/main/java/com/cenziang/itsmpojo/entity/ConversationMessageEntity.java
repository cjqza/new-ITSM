package com.cenziang.itsmpojo.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cenziang.itsmpojo.entity.base.TenantCreatedEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * 会话消息实体。
 * <p>
 * 这个表保存用户、客服和 Agent 的对话消息原文。
 * </p>
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("conversation_message")
public class ConversationMessageEntity extends TenantCreatedEntity<ConversationMessageEntity> {
    /**
     * 消息主键。
     */
    @TableId("message_id")
    private String messageId;

    /**
     * 所属会话。
     */
    private String sessionId;

    /**
     * 发送方类型。
     */
    private String senderType;

    /**
     * 发送方主键。
     */
    private String senderId;

    /**
     * 前端消息唯一 ID。
     */
    private String clientMessageId;

    /**
     * 消息内容。
     */
    private String content;

    /**
     * 附件 JSON。
     */
    private String attachmentsJson;
}
