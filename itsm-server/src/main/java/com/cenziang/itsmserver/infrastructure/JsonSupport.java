package com.cenziang.itsmserver.infrastructure;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * JSON 序列化辅助。
 * <p>
 * 实体中 attachmentsJson、tagsJson 等字段需要统一读写，避免各服务自行拼接字符串。
 * 这里自建 ObjectMapper，不依赖 Spring Boot 自动配置的 bean，以兼容不同 Jackson 版本。
 * </p>
 */
@Component
public class JsonSupport {
    private final ObjectMapper objectMapper;

    /**
     * 构造 JSON 辅助工具，注册 Java 时间模块。
     */
    public JsonSupport() {
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    /**
     * 序列化对象为 JSON 字符串。
     *
     * @param value 待序列化对象
     * @return JSON 字符串
     */
    public String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("JSON serialization failed", exception);
        }
    }

    /**
     * 反序列化 JSON 字符串为字符串列表。
     *
     * @param json JSON 字符串
     * @return 字符串列表
     */
    public List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception exception) {
            throw new IllegalStateException("JSON deserialization failed", exception);
        }
    }

    /**
     * 反序列化 JSON 字符串为指定类型。
     */
    public <T> T readValue(String json, TypeReference<T> typeReference) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (Exception exception) {
            throw new IllegalStateException("JSON deserialization failed", exception);
        }
    }

    /**
     * 反序列化 JSON 字符串为指定类。
     */
    public <T> T readValue(String json, Class<T> clazz) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception exception) {
            throw new IllegalStateException("JSON deserialization failed", exception);
        }
    }
}
