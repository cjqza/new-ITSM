package com.cenziang.itsmserver.websocket;

import com.cenziang.itsmserver.infrastructure.JsonSupport;
import com.cenziang.itsmserver.service.DomainEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 监听 Outbox relay 投递的领域事件，向相关群会话的 WebSocket 连接广播。
 */
@Component
public class OutboxRelayEventListener {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final JsonSupport jsonSupport;

    public OutboxRelayEventListener(ChatWebSocketHandler chatWebSocketHandler, JsonSupport jsonSupport) {
        this.chatWebSocketHandler = chatWebSocketHandler;
        this.jsonSupport = jsonSupport;
    }

    @EventListener
    public void onDomainEvent(DomainEvent event) {
        String sessionId = resolveSessionId(event);
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        if ("MESSAGE_SENT".equals(event.eventType())) {
            // 把消息详情一起广播，前端可直接追加无需再请求
            Map<String, Object> payload = jsonSupport.readValue(event.payloadJson(), new TypeReference<Map<String, Object>>() {
            });
            if (payload != null) {
                payload.put("type", event.eventType());
                payload.put("sessionId", sessionId);
                chatWebSocketHandler.broadcast(sessionId, payload);
                return;
            }
        }
        chatWebSocketHandler.broadcast(sessionId, Map.of(
                "type", event.eventType(),
                "sessionId", sessionId));
    }

    private String resolveSessionId(DomainEvent event) {
        if ("MESSAGE_SENT".equals(event.eventType())) {
            return event.aggregateId();
        }
        Map<String, Object> payload = jsonSupport.readValue(event.payloadJson(), new TypeReference<Map<String, Object>>() {
        });
        if (payload == null) {
            return null;
        }
        Object sessionId = payload.get("sessionId");
        return sessionId == null ? null : String.valueOf(sessionId);
    }
}
