package com.cenziang.itsmpojo.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cenziang.itsmpojo.entity.base.TenantCreatedUpdatedEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 会话实体。
 * <p>
 * 这个表保存用户咨询会话的状态、摘要和关联工单。
 * </p>
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("conversation_session")
public class ConversationSessionEntity extends TenantCreatedUpdatedEntity<ConversationSessionEntity> {
    /**
     * 会话主键。
     */
    @TableId("session_id")
    private String sessionId;

    /**
     * 会话所属用户。
     */
    private String userId;

    /**
     * 会话渠道。
     */
    private String channel;

    /**
     * 会话主题。
     */
    private String subject;

    /**
     * 会话状态。
     */
    private String status;

    /**
     * 会话摘要。
     */
    private String summary;

    /**
     * 关联工单。
     */
    private String ticketId;

    /**
     * 最近消息时间。
     */
    private LocalDateTime lastMessageAt;
}
