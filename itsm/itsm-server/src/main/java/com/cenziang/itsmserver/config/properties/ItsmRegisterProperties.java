package com.cenziang.itsmserver.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 注册与验证码相关配置。
 */
@Data
@ConfigurationProperties(prefix = "itsm.register")
public class ItsmRegisterProperties {
    /**
     * 开发模式下是否在发码响应中返回验证码（生产应关闭）。
     */
    private boolean exposeVerificationCode = true;

    /**
     * 验证码有效期（秒）。
     */
    private long codeTtlSeconds = 60;

    /**
     * 验证码位数。
     */
    private int codeLength = 6;

    /**
     * 单个 IP 在窗口期内允许的最大注册请求数。
     */
    private int rateLimitMaxPerIp = 5;

    /**
     * 注册限流窗口（秒）。
     */
    private long rateLimitWindowSeconds = 60;

    /**
     * 新注册用户默认分配的角色编码。
     */
    private String defaultRoleCode = "USER";
}
