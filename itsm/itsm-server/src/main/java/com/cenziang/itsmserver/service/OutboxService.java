package com.cenziang.itsmserver.service;

import com.cenziang.itsmpojo.entity.OutboxEventEntity;
import com.cenziang.itsmserver.infrastructure.JsonSupport;
import com.cenziang.itsmserver.infrastructure.persistence.mapper.OutboxEventMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Outbox 事件写入服务。
 * <p>
 * 领域事件与业务数据在同一事务内写入 outbox_event，由 {@link OutboxRelayJob} 定时投递，
 * 保证实时推送与缓存失效等副作用不丢失。
 * </p>
 */
@Service
public class OutboxService {

    private final OutboxEventMapper outboxEventMapper;
    private final JsonSupport jsonSupport;

    public OutboxService(OutboxEventMapper outboxEventMapper, JsonSupport jsonSupport) {
        this.outboxEventMapper = outboxEventMapper;
        this.jsonSupport = jsonSupport;
    }

    /**
     * 写入一条领域事件。
     *
     * @param tenantId      租户
     * @param eventType     事件类型（如 MESSAGE_SENT）
     * @param aggregateType 聚合类型（如 CONVERSATION / TICKET）
     * @param aggregateId   聚合主键（如 sessionId / ticketId）
     * @param payload       事件载荷（会被序列化为 JSON）
     */
    public void publish(String tenantId, String eventType, String aggregateType, String aggregateId, Object payload) {
        outboxEventMapper.insert(new OutboxEventEntity()
                .setEventId("evt_" + UUID.randomUUID().toString().replace("-", ""))
                .setTenantId(tenantId)
                .setEventType(eventType)
                .setAggregateType(aggregateType)
                .setAggregateId(aggregateId)
                .setPayloadJson(jsonSupport.write(payload))
                .setEventStatus("NEW")
                .setOccurredAt(LocalDateTime.now())
                .setRetryCount(0));
    }
}
