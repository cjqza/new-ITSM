package com.cenziang.itsmserver.websocket;

import com.cenziang.itsmpojo.dto.ConversationDtos;
import com.cenziang.itsmserver.application.RequestContext;
import com.cenziang.itsmserver.infrastructure.JsonSupport;
import com.cenziang.itsmserver.service.ConversationService;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天 WebSocket 处理器。
 * <p>
 * 客户端连接后 subscribe 到某个群会话；发消息走 REST 事务 + outbox，广播由 {@link OutboxRelayEventListener}
 * 通过 {@link #broadcast(String, Object)} 完成，保证一致性。
 * </p>
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ConversationService conversationService;
    private final JsonSupport jsonSupport;

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final Map<String, Set<WebSocketSession>> subscribersBySession = new ConcurrentHashMap<>();

    public ChatWebSocketHandler(ConversationService conversationService, JsonSupport jsonSupport) {
        this.conversationService = conversationService;
        this.jsonSupport = jsonSupport;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        Map<String, Object> payload = jsonSupport.readValue(message.getPayload(), new TypeReference<Map<String, Object>>() {
        });
        if (payload == null) {
            return;
        }
        String action = String.valueOf(payload.getOrDefault("action", ""));
        if ("subscribe".equals(action)) {
            String sessionId = (String) payload.get("sessionId");
            if (sessionId != null && !sessionId.isBlank()) {
                subscribersBySession.computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet()).add(session);
            }
            return;
        }
        if ("unsubscribe".equals(action)) {
            String sessionId = (String) payload.get("sessionId");
            if (sessionId != null) {
                Set<WebSocketSession> set = subscribersBySession.get(sessionId);
                if (set != null) {
                    set.remove(session);
                }
            }
            return;
        }
        if ("message".equals(action)) {
            String sessionId = (String) payload.get("sessionId");
            String clientMessageId = (String) payload.get("clientMessageId");
            String content = (String) payload.get("content");
            if (sessionId == null || content == null || content.isBlank()) {
                return;
            }
            conversationService.sendMessage(
                    contextOf(session), sessionId,
                    new ConversationDtos.SendMessageRequest(clientMessageId, content, null));
        }
    }

    /**
     * 向订阅了某会话的所有连接广播一个 JSON 事件。
     */
    public void broadcast(String sessionId, Object event) {
        Set<WebSocketSession> subscribers = subscribersBySession.get(sessionId);
        if (subscribers == null || subscribers.isEmpty()) {
            return;
        }
        String json = jsonSupport.write(event);
        for (WebSocketSession session : subscribers) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(json));
                }
            } catch (Exception ignored) {
                // 发送失败不影响其他连接
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        for (Set<WebSocketSession> set : subscribersBySession.values()) {
            set.remove(session);
        }
    }

    @SuppressWarnings("unchecked")
    private RequestContext contextOf(WebSocketSession session) {
        Map<String, Object> attributes = session.getAttributes();
        List<String> roles = (List<String>) attributes.get("roles");
        return new RequestContext(
                (String) attributes.get("tenantId"),
                (String) attributes.get("userId"),
                roles == null ? List.of() : roles,
                (String) attributes.get("permissionsVersion"),
                (Long) attributes.get("authVersion"));
    }
}
