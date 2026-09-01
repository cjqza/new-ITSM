package com.cenziang.itsmserver.service;

import com.cenziang.itsmcommon.api.BusinessException;
import com.cenziang.itsmcommon.api.ErrorCode;
import com.cenziang.itsmserver.config.properties.ItsmAiAgentProperties;
import com.cenziang.itsmserver.infrastructure.JsonSupport;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;



import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * AI 客服 Agent 服务。
 * <p>
 * 调用 Python LangGraph 服务获取结构化诊断结果。
 * </p>
 */
@Service
public class AiAgentService {
    private static final Logger log = LoggerFactory.getLogger(AiAgentService.class);

    private final ItsmAiAgentProperties properties;
    private final RestClient restClient;
    private final JsonSupport jsonSupport;

    public AiAgentService(ItsmAiAgentProperties properties, JsonSupport jsonSupport) {
        this.properties = properties;
        this.jsonSupport = jsonSupport;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getUrl())
                .build();
    }

    /**
     * 调用 AI Agent 获取诊断结果。
     *
     * @param userMessage  用户消息
     * @param chatHistory  历史消息 [{role:"user"|"assistant", content:"..."}]
     * @return 结构化诊断结果
     */
    public AiAgentResult diagnose(String userMessage, List<Map<String, String>> chatHistory) {
        if (!properties.isEnabled()) {
            return AiAgentResult.fallback("AI 客服功能未启用");
        }

        if (properties.isUseExternal()) {
            try {
                Map<String, Object> body = Map.of(
                        "message", userMessage,
                        "history", chatHistory != null ? chatHistory : List.of()
                );
                String responseJson = restClient.post()
                        .uri("/api/v1/ai/chat")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .body(jsonSupport.write(body))
                        .retrieve()
                        .body(String.class);
                AiAgentResponse resp = jsonSupport.readValue(responseJson, AiAgentResponse.class);
                if (resp == null) {
                    return AiAgentResult.fallback("AI 返回为空");
                }
                return new AiAgentResult(
                        resp.response,
                        resp.classification != null ? resp.classification : "OTHER",
                        resp.priority != null ? resp.priority : "medium",
                        resp.confidence != null ? resp.confidence : BigDecimal.ZERO,
                        resp.shouldHandoff != null ? resp.shouldHandoff : false,
                        resp.handoffReason != null ? resp.handoffReason : ""
                );
            } catch (Exception e) {
                log.error("AI Agent 调用失败: {}", e.getMessage());
                return builtinDiagnose(userMessage);
            }
        }

        return builtinDiagnose(userMessage);
    }

    private AiAgentResult builtinDiagnose(String userMessage) {
        String message = userMessage == null ? "" : userMessage.trim();

        if (matches(message, "转人工", "人工客服", "人工服务", "找客服")) {
            return new AiAgentResult(
                    "好的，我马上为你转接人工客服，并保留当前对话上下文。",
                    "IT_SUPPORT", "high", new BigDecimal("0.98"), true, "用户主动请求人工服务");
        }

        if (matches(message, "账号", "密码", "权限", "登录", "登录不上", "无法登录")) {
            return new AiAgentResult(
                    "建议先重新登录并确认账号是否被锁定。如果问题依旧，我可以直接转人工帮你核实账号权限。",
                    "IT_SUPPORT", "medium", new BigDecimal("0.82"), false, "");
        }

        if (matches(message, "网络", "wifi", "vpn", "断网", "无法上网", "网络异常")) {
            return new AiAgentResult(
                    "建议先检查网络连接、重启路由器或切换网络环境；若 VPN 无法连通，可收集报错截图后转人工继续排查。",
                    "IT_SUPPORT", "medium", new BigDecimal("0.80"), false, "");
        }

        if (matches(message, "邮箱", "邮件", "outlook", "收不到", "发不出去")) {
            return new AiAgentResult(
                    "可以先检查邮箱缓存和收件规则，若发送/接收仍异常，我会帮你转交人工客服查看邮箱服务状态。",
                    "IT_SUPPORT", "medium", new BigDecimal("0.78"), false, "");
        }

        if (matches(message, "系统", "蓝屏", "死机", "更新", "windows")) {
            return new AiAgentResult(
                    "建议先记录蓝屏代码并检查最近系统更新，如果重启后问题仍在，我可以帮你生成工单交给人工处理。",
                    "IT_SUPPORT", "medium", new BigDecimal("0.77"), false, "");
        }

        if (matches(message, "软件", "安装", "office", "版本")) {
            return new AiAgentResult(
                    "可以先确认软件版本和安装权限，尝试修复安装。如果仍然失败，我可以转人工协助你完成安装。",
                    "IT_SUPPORT", "medium", new BigDecimal("0.76"), false, "");
        }

        return new AiAgentResult(
                "收到，我已记录你的问题。你可以补充更多现象，也可以直接点击转人工，我会保留上下文交给客服。",
                "IT_SUPPORT", "low", new BigDecimal("0.58"), false, "");
    }

    private boolean matches(String message, String... keywords) {
        for (String keyword : keywords) {
            if (message.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    // ---------- DTOs ----------

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AiAgentResponse(
            String response,
            String classification,
            String priority,
            BigDecimal confidence,
            @JsonProperty("shouldHandoff") Boolean shouldHandoff,
            String handoffReason
    ) {}

    public record AiAgentResult(
            String response,
            String classification,
            String priority,
            BigDecimal confidence,
            boolean shouldHandoff,
            String handoffReason
    ) {
        public static AiAgentResult fallback(String message) {
            return new AiAgentResult(message, "OTHER", "medium", BigDecimal.ZERO, true, "AI 服务不可用");
        }
    }
}
