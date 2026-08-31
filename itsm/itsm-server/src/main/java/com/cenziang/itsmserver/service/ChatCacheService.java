package com.cenziang.itsmserver.service;

import com.cenziang.itsmpojo.dto.ConversationDtos;
import com.cenziang.itsmserver.infrastructure.JsonSupport;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

/**
 * 聊天内容 Redis 缓存。
 * <p>
 * 活跃会话的消息以 JSON 形式缓存在 Redis，设置空闲超时（默认 30 分钟）自动过期；
 * 会话结束 / 工单闭环时由调用方主动 evict 清理缓存。
 * </p>
 */
@Component
public class ChatCacheService {
    private static final String KEY_PREFIX = "itsm:chat:session:";

    private final StringRedisTemplate redisTemplate;
    private final JsonSupport jsonSupport;
    private final Duration ttl;

    public ChatCacheService(StringRedisTemplate redisTemplate,
                            JsonSupport jsonSupport,
                            @Value("${itsm.chat.cache-ttl-minutes:30}") long ttlMinutes) {
        this.redisTemplate = redisTemplate;
        this.jsonSupport = jsonSupport;
        this.ttl = Duration.ofMinutes(ttlMinutes);
    }

    /**
     * 缓存穿透式读取：命中缓存直接返回，未命中则回源并写缓存。
     */
    public List<ConversationDtos.SessionMessageItem> getOrLoad(String sessionId,
                                                               Supplier<List<ConversationDtos.SessionMessageItem>> loader) {
        String key = KEY_PREFIX + sessionId;
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json != null && !json.isBlank()) {
                List<ConversationDtos.SessionMessageItem> cached = jsonSupport.readValue(json, new TypeReference<List<ConversationDtos.SessionMessageItem>>() {
                });
                if (cached != null) {
                    return cached;
                }
            }
        } catch (Exception ignored) {
            // Redis 不可用或反序列化失败时回源数据库
        }
        List<ConversationDtos.SessionMessageItem> messages = loader.get();
        cache(sessionId, messages);
        return messages;
    }

    /**
     * 写缓存（带过期时间）。
     */
    public void cache(String sessionId, List<ConversationDtos.SessionMessageItem> messages) {
        try {
            redisTemplate.opsForValue().set(KEY_PREFIX + sessionId, jsonSupport.write(messages), ttl);
        } catch (Exception ignored) {
        }
    }

    /**
     * 立即清理缓存（会话结束 / 归档）。
     */
    public void evict(String sessionId) {
        try {
            redisTemplate.delete(KEY_PREFIX + sessionId);
        } catch (Exception ignored) {
        }
    }
}
