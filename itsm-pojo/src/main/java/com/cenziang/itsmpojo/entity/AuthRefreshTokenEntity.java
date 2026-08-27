package com.cenziang.itsmpojo.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cenziang.itsmpojo.entity.base.TenantCreatedUpdatedEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 刷新令牌实体。
 * <p>
 * 这个表保存 refresh token 的哈希和吊销状态，避免明文落库。
 * </p>
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("auth_refresh_token")
public class AuthRefreshTokenEntity extends TenantCreatedUpdatedEntity<AuthRefreshTokenEntity> {
    /**
     * 刷新令牌主键。
     */
    @TableId("token_id")
    private String tokenId;

    /**
     * 用户主键。
     */
    private String userId;

    /**
     * 令牌哈希。
     */
    private String tokenHash;

    /**
     * 过期时间。
     */
    private LocalDateTime expiresAt;

    /**
     * 吊销时间。
     */
    private LocalDateTime revokedAt;

    /**
     * 替换后的 tokenId。
     */
    private String replacedBy;

    /**
     * 认证版本。
     */
    private Long authVersion;
}
