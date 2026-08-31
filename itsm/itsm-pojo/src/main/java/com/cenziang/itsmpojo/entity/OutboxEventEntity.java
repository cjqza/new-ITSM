package com.cenziang.itsmpojo.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * Outbox 事件实体。
 * <p>
 * 这个表保证事务内产生的领域事件不会丢失，便于异步投递到 RabbitMQ。
 * </p>
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("outbox_event")
public class OutboxEventEntity {
    /**
     * 租户标识。
     */
    private String tenantId;

    /**
     * 事件主键。
     */
    @TableId("event_id")
    private String eventId;

    /**
     * 事件类型。
     */
    private String eventType;

    /**
     * 聚合类型。
     */
    private String aggregateType;

    /**
     * 聚合主键。
     */
    private String aggregateId;

    /**
     * 事件载荷 JSON。
     */
    private String payloadJson;

    /**
     * 事件状态。
     */
    private String eventStatus;

    /**
     * 发生时间。
     */
    private LocalDateTime occurredAt;

    /**
     * 发布时间。
     */
    private LocalDateTime publishedAt;

    /**
     * 重试次数。
     */
    private Integer retryCount;
}
