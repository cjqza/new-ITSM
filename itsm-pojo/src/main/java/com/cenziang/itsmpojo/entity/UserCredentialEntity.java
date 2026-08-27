package com.cenziang.itsmpojo.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cenziang.itsmpojo.entity.base.TenantCreatedUpdatedEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 用户登录凭证实体。
 * <p>
 * 这个表保存登录名、密码哈希、失败次数、锁定时间和认证版本。
 * </p>
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("user_credential")
public class UserCredentialEntity extends TenantCreatedUpdatedEntity<UserCredentialEntity> {
    /**
     * 凭证主键。
     */
    @TableId("credential_id")
    private String credentialId;

    /**
     * 关联用户主键。
     */
    private String userId;

    /**
     * 登录名或 SSO code。
     */
    private String loginName;

    /**
     * 密码哈希。
     */
    private String passwordHash;

    /**
     * 密码算法。
     */
    private String passwordAlgo;

    /**
     * 密码版本。
     */
    private Long passwordVersion;

    /**
     * 认证版本。
     */
    private Long authVersion;

    /**
     * 连续失败次数。
     */
    private Integer failedCount;

    /**
     * 锁定到期时间。
     */
    private LocalDateTime lockedUntil;

    /**
     * 最近一次登录时间。
     */
    private LocalDateTime lastLoginAt;

    /**
     * 最近一次登录 IP。
     */
    private String lastLoginIp;

    /**
     * 凭证状态。
     */
    private String status;
}
