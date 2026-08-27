package com.cenziang.itsmserver.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * ITSM 安全相关配置。
 */
@Data
@ConfigurationProperties(prefix = "itsm.security")
public class ItsmSecurityProperties {
    /**
     * Agent 服务凭证密钥。
     */
    private String agentServiceKey;

    /**
     * 内部系统操作者标识。
     */
    private String systemOperatorId = "system";
}
