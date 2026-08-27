package com.cenziang.itsmserver.infrastructure.audit;

import com.cenziang.itsmpojo.entity.AuditLogEntity;
import com.cenziang.itsmpojo.entity.TicketActionLogEntity;
import com.cenziang.itsmserver.infrastructure.JsonSupport;
import com.cenziang.itsmserver.infrastructure.persistence.mapper.AuditLogMapper;
import com.cenziang.itsmserver.infrastructure.persistence.mapper.TicketActionLogMapper;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 审计与动作流水写入服务。
 * <p>
 * 关键业务动作都会落到 audit_log 或 ticket_action_log，便于问题追踪。
 * </p>
 */
@Service
public class AuditService {
    private final AuditLogMapper auditLogMapper;
    private final TicketActionLogMapper actionLogMapper;
    private final JsonSupport jsonSupport;

    public AuditService(AuditLogMapper auditLogMapper, TicketActionLogMapper actionLogMapper, JsonSupport jsonSupport) {
        this.auditLogMapper = auditLogMapper;
        this.actionLogMapper = actionLogMapper;
        this.jsonSupport = jsonSupport;
    }

    /**
     * 记录通用审计事件。
     */
    public void recordAudit(String tenantId, String operatorId, String operatorType, String actionType,
                            String resourceType, String resourceId, String traceId, Object detail) {
        AuditLogEntity entity = new AuditLogEntity()
                .setAuditId(UUID.randomUUID().toString())
                .setTenantId(tenantId)
                .setOperatorId(operatorId)
                .setOperatorType(operatorType)
                .setActionType(actionType)
                .setResourceType(resourceType)
                .setResourceId(resourceId)
                .setRequestTraceId(traceId)
                .setDetailJson(detail == null ? null : jsonSupport.write(detail));
        auditLogMapper.insert(entity);
    }

    /**
     * 记录工单动作流水。
     */
    public void recordTicketAction(String tenantId, String ticketId, String actionType,
                                   String operatorId, String operatorType, String content) {
        TicketActionLogEntity entity = new TicketActionLogEntity()
                .setActionLogId(UUID.randomUUID().toString())
                .setTenantId(tenantId)
                .setTicketId(ticketId)
                .setActionType(actionType)
                .setOperatorId(operatorId)
                .setOperatorType(operatorType)
                .setActionContent(content);
        actionLogMapper.insert(entity);
    }
}