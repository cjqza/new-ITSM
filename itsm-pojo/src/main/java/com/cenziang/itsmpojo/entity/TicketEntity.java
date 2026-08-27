package com.cenziang.itsmpojo.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cenziang.itsmpojo.entity.base.TenantCreatedUpdatedVersionEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 工单实体。
 * <p>
 * 这是工单生命周期的核心事实表，所有状态迁移都围绕它进行。
 * </p>
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("ticket")
public class TicketEntity extends TenantCreatedUpdatedVersionEntity<TicketEntity> {
    /**
     * 工单主键。
     */
    @TableId("ticket_id")
    private String ticketId;

    /**
     * 工单号。
     */
    private String ticketNo;

    /**
     * 来源。
     */
    private String source;

    /**
     * 关联会话。
     */
    private String sessionId;

    /**
     * 请求人。
     */
    private String requesterId;

    /**
     * 当前处理人。
     */
    private String assigneeId;

    /**
     * 标题。
     */
    private String title;

    /**
     * 描述。
     */
    private String description;

    /**
     * 环境信息。
     */
    private String environment;

    /**
     * 附件 JSON。
     */
    private String attachmentsJson;

    /**
     * 优先级。
     */
    private String priority;

    /**
     * 业务线编码。
     */
    private String businessLineCode;

    /**
     * 状态。
     */
    private String status;

    /**
     * 接单时间。
     */
    private LocalDateTime acceptedAt;

    /**
     * 解决时间。
     */
    private LocalDateTime resolvedAt;

    /**
     * 关闭时间。
     */
    private LocalDateTime closedAt;

    /**
     * 重开时间。
     */
    private LocalDateTime reopenedAt;

    /**
     * 解决摘要。
     */
    private String resolutionSummary;

    /**
     * 解决类型。
     */
    private String resolutionType;

    /**
     * 解决人。
     */
    private String resolvedBy;

    /**
     * 关闭原因。
     */
    private String closeReason;

    /**
     * 关闭人。
     */
    private String closedBy;

    /**
     * 重开原因。
     */
    private String reopenReason;

    /**
     * 重开人。
     */
    private String reopenedBy;
}
