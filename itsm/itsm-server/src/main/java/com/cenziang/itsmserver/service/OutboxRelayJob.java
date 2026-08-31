package com.cenziang.itsmserver.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cenziang.itsmpojo.entity.OutboxEventEntity;
import com.cenziang.itsmserver.infrastructure.persistence.mapper.OutboxEventMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Outbox 定时投递器。
 * <p>
 * 周期扫描 NEW 事件，发布到进程内事件总线（WebSocket/SSE 监听器订阅），成功后标记 PUBLISHED，
 * 失败则重试次数 +1 保持可重试。数据落库才是事实源，投递失败只影响实时推送。
 * </p>
 */
@Component
public class OutboxRelayJob {

    private final OutboxEventMapper outboxEventMapper;
    private final ApplicationEventPublisher eventPublisher;

    public OutboxRelayJob(OutboxEventMapper outboxEventMapper, ApplicationEventPublisher eventPublisher) {
        this.outboxEventMapper = outboxEventMapper;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(fixedDelay = 1000)
    public void relay() {
        List<OutboxEventEntity> events = outboxEventMapper.selectList(
                new LambdaQueryWrapper<OutboxEventEntity>()
                        .eq(OutboxEventEntity::getEventStatus, "NEW")
                        .orderByAsc(OutboxEventEntity::getOccurredAt)
                        .last("LIMIT 100"));
        for (OutboxEventEntity event : events) {
            try {
                eventPublisher.publishEvent(new DomainEvent(
                        event.getTenantId(),
                        event.getEventType(),
                        event.getAggregateType(),
                        event.getAggregateId(),
                        event.getPayloadJson()));
                markPublished(event.getEventId());
            } catch (Exception ex) {
                markFailed(event.getEventId(), event.getRetryCount());
            }
        }
    }

    private void markPublished(String eventId) {
        outboxEventMapper.update(null, new LambdaUpdateWrapper<OutboxEventEntity>()
                .eq(OutboxEventEntity::getEventId, eventId)
                .set(OutboxEventEntity::getEventStatus, "PUBLISHED")
                .set(OutboxEventEntity::getPublishedAt, LocalDateTime.now()));
    }

    private void markFailed(String eventId, Integer currentRetry) {
        outboxEventMapper.update(null, new LambdaUpdateWrapper<OutboxEventEntity>()
                .eq(OutboxEventEntity::getEventId, eventId)
                .set(OutboxEventEntity::getRetryCount, (currentRetry == null ? 0 : currentRetry) + 1));
    }
}
