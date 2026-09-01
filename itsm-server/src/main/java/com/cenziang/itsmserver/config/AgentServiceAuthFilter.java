package com.cenziang.itsmserver.config;

import com.cenziang.itsmserver.config.properties.ItsmSecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Agent 编排接口凭证校验过滤器。
 * <p>
 * 仅拦截 /api/v1/agent/** 路径，校验 X-Agent-Service-Key 是否匹配配置值，防止外部调用 Agent 决策接口。
 * </p>
 */
@Component
public class AgentServiceAuthFilter extends OncePerRequestFilter {

    private static final Set<String> PROTECTED_PREFIXES = Set.of("/api/v1/agent/");

    private final ItsmSecurityProperties securityProperties;

    public AgentServiceAuthFilter(ItsmSecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestUri = request.getRequestURI();
        if (isAgentEndpoint(requestUri)) {
            String expectedKey = securityProperties.getAgentServiceKey();
            if (expectedKey == null || expectedKey.isBlank()) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            String actualKey = request.getHeader("X-Agent-Service-Key");
            if (!expectedKey.equals(actualKey)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAgentEndpoint(String requestUri) {
        if (requestUri == null) {
            return false;
        }
        for (String prefix : PROTECTED_PREFIXES) {
            if (requestUri.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
