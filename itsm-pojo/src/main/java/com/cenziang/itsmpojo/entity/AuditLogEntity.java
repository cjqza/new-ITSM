package com.cenziang.itsmpojo.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cenziang.itsmpojo.entity.base.TenantCreatedEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * 审计日志实体。
 * <p>
 * 这个表记录关键业务动作的审计轨迹。
 * </p>
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("audit_log")
public class AuditLogEntity extends TenantCreatedEntity<AuditLogEntity> {
    /**
     * 审计主键。
     */
    @TableId("audit_id")
    private String auditId;

    /**
     * 操作者。
     */
    private String operatorId;

    /**
     * 操作者类型。
     */
    private String operatorType;

    /**
     * 动作类型。
     */
    private String actionType;

    /**
     * 资源类型。
     */
    private String resourceType;

    /**
     * 资源主键。
     */
    private String resourceId;

    /**
     * 链路 ID。
     */
    private String requestTraceId;

    /**
     * 明细 JSON。
     */
    private String detailJson;
}
