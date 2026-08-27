package com.cenziang.itsmpojo.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cenziang.itsmpojo.entity.base.TenantCreatedEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * 工单动作流水实体。
 * <p>
 * 这个表记录受理、分类、解决、关闭、重开等动作原文。
 * </p>
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("ticket_action_log")
public class TicketActionLogEntity extends TenantCreatedEntity<TicketActionLogEntity> {
    /**
     * 动作流水主键。
     */
    @TableId("action_log_id")
    private String actionLogId;

    /**
     * 工单主键。
     */
    private String ticketId;

    /**
     * 动作类型。
     */
    private String actionType;

    /**
     * 操作者。
     */
    private String operatorId;

    /**
     * 操作者类型。
     */
    private String operatorType;

    /**
     * 动作内容。
     */
    private String actionContent;
}
