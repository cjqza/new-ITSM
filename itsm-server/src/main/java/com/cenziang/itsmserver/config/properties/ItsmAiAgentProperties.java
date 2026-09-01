package com.cenziang.itsmserver.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 客服 Agent 配置。
 */
@Data
@ConfigurationProperties(prefix = "itsm.ai-agent")
public class ItsmAiAgentProperties {
    /**
     * 是否启用 AI 客服。
     */
    private boolean enabled = true;

    /**
     * 优先使用 Python Agent 服务。
     * <p>
     * 关闭时系统会走内置关键词诊断，确保在不启动外部 AI 服务的情况下主链路仍可联调。
     * </p>
     */
    private boolean useExternal = true;

    /**
     * Python Agent 服务地址。
     */
    private String url = "http://localhost:8090";

    /**
     * Agent 调用超时（毫秒）。
     */
    private long timeoutMs = 30000;
}
