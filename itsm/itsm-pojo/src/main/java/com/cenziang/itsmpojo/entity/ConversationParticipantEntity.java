package com.cenziang.itsmpojo.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 会话参与者实体。
 * <p>
 * 转人工后把 conversation_session 升级为“群”，一个会话可包含员工和多个客服成员。
 * </p>
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("conversation_participant")
public class ConversationParticipantEntity {

    @TableId
    private String participantId;

    private String tenantId;

    private String sessionId;

    private String userId;

    private String participantType;

    private LocalDateTime joinedAt;
}
