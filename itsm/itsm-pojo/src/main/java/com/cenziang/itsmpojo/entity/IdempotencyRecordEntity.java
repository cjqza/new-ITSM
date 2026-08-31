package com.cenziang.itsmpojo.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cenziang.itsmpojo.entity.base.TenantCreatedEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 幂等记录实体。
 * <p>
 * 这个表保存命令请求的幂等键、请求摘要和首次响应。
 * </p>
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("idempotency_record")
public class IdempotencyRecordEntity extends TenantCreatedEntity<IdempotencyRecordEntity> {
    /**
     * 幂等主键。
     */
    @TableId("idempotency_id")
    private String idempotencyId;

    /**
     * 调用者。
     */
    private String callerId;

    /**
     * HTTP 方法。
     */
    private String method;

    /**
     * 路径。
     */
    private String path;

    /**
     * 幂等键。
     */
    private String idempotencyKey;

    /**
     * 请求摘要。
     */
    private String requestHash;

    /**
     * 响应码。
     */
    private String responseCode;

    /**
     * 响应体 JSON。
     */
    private String responseBody;

    /**
     * 过期时间。
     */
    private LocalDateTime expiresAt;
}
