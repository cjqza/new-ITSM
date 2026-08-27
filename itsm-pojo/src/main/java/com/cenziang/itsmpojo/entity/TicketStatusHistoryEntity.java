package com.cenziang.itsmpojo.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cenziang.itsmpojo.entity.base.TenantCreatedEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 工单状态历史实体。
 * <p>
 * 这个表记录工单每一次状态迁移，便于审计和问题追踪。
 * </p>
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("ticket_status_history")
public class TicketStatusHistoryEntity extends TenantCreatedEntity<TicketStatusHistoryEntity> {
    /**
     * 历史主键。
     */
    @TableId("history_id")
    private String historyId;

    /**
     * 工单主键。
     */
    private String ticketId;

    /**
     * 原状态。
     */
    private String fromStatus;

    /**
     * 目标状态。
     */
    private String toStatus;

    /**
     * 操作者。
     */
    private String operatorId;

    /**
     * 操作者类型。
     */
    private String operatorType;

    /**
     * 操作类型。
     */
    private String actionType;

    /**
     * 操作说明。
     */
    private String actionNote;

    /**
     * 发生时间。
     */
    private LocalDateTime occurredAt;
}
