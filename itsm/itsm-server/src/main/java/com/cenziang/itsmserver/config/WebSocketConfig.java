package com.cenziang.itsmserver.config;

import com.cenziang.itsmserver.service.AuthTokenService;
import com.cenziang.itsmserver.websocket.ChatWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 配置。
 * <p>
 * 握手时从 query 参数 token 解析 JWT，写入会话属性；连接后按会话订阅群聊。
 * </p>
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final AuthTokenService tokenService;

    public WebSocketConfig(ChatWebSocketHandler chatWebSocketHandler, AuthTokenService tokenService) {
        this.chatWebSocketHandler = chatWebSocketHandler;
        this.tokenService = tokenService;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new HandshakeInterceptor() {
                    @Override
                    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
                        String query = request.getURI().getQuery();
                        String token = extractToken(query);
                        if (token == null) {
                            return false;
                        }
                        try {
                            AuthTokenService.TokenClaims claims = tokenService.parseAndValidate(token);
                            if (!"access".equalsIgnoreCase(claims.tokenType())) {
                                return false;
                            }
                            attributes.put("userId", claims.userId());
                            attributes.put("tenantId", claims.tenantId());
                            attributes.put("roles", claims.roles());
                            attributes.put("permissionsVersion", claims.permissionsVersion());
                            attributes.put("authVersion", claims.authVersion());
                            return true;
                        } catch (Exception ex) {
                            return false;
                        }
                    }

                    @Override
                    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                               WebSocketHandler wsHandler, Exception exception) {
                    }
                });
    }

    private String extractToken(String query) {
        if (query == null) {
            return null;
        }
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && "token".equals(kv[0])) {
                return kv[1];
            }
        }
        return null;
    }
}
