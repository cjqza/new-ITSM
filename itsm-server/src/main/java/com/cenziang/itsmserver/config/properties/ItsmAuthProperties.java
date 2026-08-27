package com.cenziang.itsmserver.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * ITSM 认证相关配置。
 */
@Data
@ConfigurationProperties(prefix = "itsm.auth")
public class ItsmAuthProperties {
    /**
     * JWT 签发者。
     */
    private String issuer;

    /**
     * JWT 受众。
     */
    private String audience;

    /**
     * 访问令牌有效期（秒）。
     */
    private long accessTokenTtlSeconds;

    /**
     * 刷新令牌有效期（秒）。
     */
    private long refreshTokenTtlSeconds;

    /**
     * 登录失败锁定阈值。
     */
    private int loginFailureLockThreshold;

    /**
     * 锁定分钟数。
     */
    private int loginLockMinutes;

    /**
     * BCrypt 强度。
     */
    private int bcryptStrength;

    /**
     * JWT 配置。
     */
    private Jwt jwt = new Jwt();

    /**
     * 种子数据。
     */
    private Seed seed = new Seed();

    /**
     * JWT 配置。
     */
    @Data
    public static class Jwt {
        /**
         * 对称密钥。
         */
        private String secret;

        /**
         * 公钥。
         */
        private String publicKey;

        /**
         * 私钥。
         */
        private String privateKey;
    }

    /**
     * 种子账号配置。
     */
    @Data
    public static class Seed {
        /**
         * 租户主键。
         */
        private String tenantId;

        /**
         * 租户名称。
         */
        private String tenantName;

        /**
         * 用户主键。
         */
        private String userId;

        /**
         * 登录名或 SSO code。
         */
        private String loginName;

        /**
         * 明文密码。
         */
        private String password;


        /**
         * 一次性 SSO 授权码。
         */
        private String ssoCode;
        /**
         * 展示名称。
         */
        private String displayName;

        /**
         * 部门名称。
         */
        private String departmentName;

        /**
         * 角色编码。
         */
        private String[] roles = new String[0];
    }
}
