package com.cenziang.itsmpojo.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cenziang.itsmpojo.entity.base.TenantCreatedEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * 登录审计实体。
 * <p>
 * 这个表记录登录成功和失败事件，用于安全追踪。
 * </p>
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("auth_login_audit")
public class AuthLoginAuditEntity extends TenantCreatedEntity<AuthLoginAuditEntity> {
    /**
     * 审计主键。
     */
    @TableId("audit_id")
    private String auditId;

    /**
     * 用户主键。
     */
    private String userId;

    /**
     * 登录名。
     */
    private String loginName;

    /**
     * 结果。
     */
    private String result;

    /**
     * 原因。
     */
    private String reason;

    /**
     * 客户端 IP。
     */
    private String clientIp;

    /**
     * 用户代理。
     */
    private String userAgent;

    /**
     * 链路 ID。
     */
    private String traceId;
}
