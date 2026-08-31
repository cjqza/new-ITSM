package com.cenziang.itsmpojo.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cenziang.itsmpojo.entity.base.TenantCreatedUpdatedVersionEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * 工单分类实体。
 * <p>
 * 这个表保存管理单元、症状、原因和解决方法的标准分类结果。
 * </p>
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("ticket_classification")
public class TicketClassificationEntity extends TenantCreatedUpdatedVersionEntity<TicketClassificationEntity> {
    /**
     * 工单主键。
     */
    @TableId("ticket_id")
    private String ticketId;

    /**
     * 管理单元。
     */
    private String managementUnitId;

    /**
     * 症状。
     */
    private String symptomId;

    /**
     * 原因。
     */
    private String reasonId;

    /**
     * 解决方法。
     */
    private String solutionMethodId;

    /**
     * 自定义原因。
     */
    private String customReason;

    /**
     * 自定义解决说明。
     */
    private String customSolution;
}
