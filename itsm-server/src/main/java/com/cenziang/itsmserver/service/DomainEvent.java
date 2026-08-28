package com.cenziang.itsmserver.service;

/**
 * Outbox relay 投递到进程内事件总线时的领域事件载荷。
 */
public record DomainEvent(
        String tenantId,
        String eventType,
        String aggregateType,
        String aggregateId,
        String payloadJson
) {
}
