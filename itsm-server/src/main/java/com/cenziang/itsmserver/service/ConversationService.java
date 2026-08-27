package com.cenziang.itsmserver.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cenziang.itsmcommon.api.BusinessException;
import com.cenziang.itsmcommon.api.ErrorCode;
import com.cenziang.itsmcommon.api.PageResponse;
import com.cenziang.itsmpojo.dto.ConversationDtos;
import com.cenziang.itsmpojo.entity.AgentDecisionEntity;
import com.cenziang.itsmpojo.entity.ConversationMessageEntity;
import com.cenziang.itsmpojo.entity.ConversationSessionEntity;
import com.cenziang.itsmserver.application.RequestContext;
import com.cenziang.itsmserver.infrastructure.JsonSupport;
import com.cenziang.itsmserver.infrastructure.persistence.mapper.AgentDecisionMapper;
import com.cenziang.itsmserver.infrastructure.persistence.mapper.ConversationMessageMapper;
import com.cenziang.itsmserver.infrastructure.persistence.mapper.ConversationSessionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 会话服务。
 * <p>
 * 负责用户咨询会话的创建、读取、消息发送和 Agent 决策落库。
 * </p>
 */
@Service
public class ConversationService {
    private final ConversationSessionMapper sessionMapper;
    private final ConversationMessageMapper messageMapper;
    private final AgentDecisionMapper decisionMapper;
    private final JsonSupport jsonSupport;
    private final ChatCacheService chatCacheService;

    public ConversationService(ConversationSessionMapper sessionMapper,
                               ConversationMessageMapper messageMapper,
                               AgentDecisionMapper decisionMapper,
                               JsonSupport jsonSupport,
                               ChatCacheService chatCacheService) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.decisionMapper = decisionMapper;
        this.jsonSupport = jsonSupport;
        this.chatCacheService = chatCacheService;
    }

    /**
     * 创建用户咨询会话。
     */
    @Transactional
    public ConversationDtos.SessionCreateResponse createSession(RequestContext context, ConversationDtos.CreateSessionRequest request) {
        if (request.channel() == null || request.channel().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "channel is required");
        }
        ConversationSessionEntity session = new ConversationSessionEntity()
                .setSessionId("ses_" + UUID.randomUUID().toString().replace("-", ""))
                .setTenantId(context.tenantId())
                .setUserId(context.userId())
                .setChannel(request.channel())
                .setSubject(request.subject())
                .setStatus("ACTIVE");
        sessionMapper.insert(session);
        return new ConversationDtos.SessionCreateResponse(
                session.getSessionId(), session.getStatus(), null, session.getCreatedAt(), null);
    }

    /**
     * 分页查询当前用户的咨询会话列表。
     */
    @Transactional(readOnly = true)
    public PageResponse<ConversationDtos.SessionListItem> listSessions(RequestContext context, int page, int pageSize, String keyword) {
        LambdaQueryWrapper<ConversationSessionEntity> wrapper = new LambdaQueryWrapper<ConversationSessionEntity>()
                .eq(ConversationSessionEntity::getTenantId, context.tenantId());
        boolean supportView = context.roles().contains("SUPPORT_AGENT")
                || context.roles().contains("SUPPORT_ADMIN")
                || context.roles().contains("SUPERVISOR");
        if (!supportView) {
            wrapper.eq(ConversationSessionEntity::getUserId, context.userId());
        }
        wrapper.like(keyword != null && !keyword.isBlank(), ConversationSessionEntity::getSubject, keyword);
        wrapper.orderByDesc(ConversationSessionEntity::getLastMessageAt)
                .orderByDesc(ConversationSessionEntity::getCreatedAt);
        Page<ConversationSessionEntity> result = sessionMapper.selectPage(new Page<>(page, pageSize), wrapper);
        List<ConversationDtos.SessionListItem> items = result.getRecords().stream()
                .map(session -> new ConversationDtos.SessionListItem(
                        session.getSessionId(),
                        session.getUserId(),
                        session.getChannel(),
                        session.getSubject(),
                        session.getStatus(),
                        session.getSummary(),
                        session.getTicketId(),
                        session.getLastMessageAt(),
                        session.getCreatedAt()))
                .toList();
        return PageResponse.of(items, result.getCurrent(), result.getSize(), result.getTotal());
    }

    /**
     * 读取会话详情。
     */
    @Transactional(readOnly = true)
    public ConversationDtos.SessionDetailResponse getSession(RequestContext context, String sessionId,
                                                             int messagePage, int messagePageSize) {
        ConversationSessionEntity session = requireSession(context.tenantId(), sessionId);
        if (!session.getUserId().equals(context.userId()) && !context.roles().contains("SUPPORT_AGENT")
                && !context.roles().contains("SUPPORT_ADMIN") && !context.roles().contains("SUPERVISOR")) {
            throw new BusinessException(ErrorCode.DATA_SCOPE_FORBIDDEN);
        }
        List<ConversationDtos.SessionMessageItem> messages = chatCacheService.getOrLoad(sessionId, () -> {
            Page<ConversationMessageEntity> page = messageMapper.selectPage(
                    new Page<>(messagePage, messagePageSize),
                    new LambdaQueryWrapper<ConversationMessageEntity>()
                            .eq(ConversationMessageEntity::getTenantId, context.tenantId())
                            .eq(ConversationMessageEntity::getSessionId, sessionId)
                            .orderByAsc(ConversationMessageEntity::getCreatedAt)
            );
            return page.getRecords().stream()
                    .map(m -> new ConversationDtos.SessionMessageItem(m.getMessageId(), m.getSenderType(), m.getContent(), m.getCreatedAt()))
                    .toList();
        });
        return new ConversationDtos.SessionDetailResponse(
                session.getSessionId(), session.getStatus(), session.getTicketId(), session.getSummary(),
                PageResponse.of(messages, messagePage, messagePageSize, messages.size()));
    }

    /**
     * 发送用户消息，并写入 Agent 首轮决策结果。
     */
    @Transactional
    public ConversationDtos.SendMessageResponse sendMessage(RequestContext context, String sessionId,
                                                            ConversationDtos.SendMessageRequest request) {
        ConversationSessionEntity session = requireSession(context.tenantId(), sessionId);
        if (!session.getUserId().equals(context.userId())) {
            throw new BusinessException(ErrorCode.DATA_SCOPE_FORBIDDEN);
        }
        if (!"ACTIVE".equalsIgnoreCase(session.getStatus())) {
            throw new BusinessException(ErrorCode.ILLEGAL_STATE_TRANSITION);
        }
        if (request.clientMessageId() == null || request.clientMessageId().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "clientMessageId is required");
        }
        if (request.content() == null || request.content().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "content is required");
        }

        Long existing = messageMapper.selectCount(new LambdaQueryWrapper<ConversationMessageEntity>()
                .eq(ConversationMessageEntity::getTenantId, context.tenantId())
                .eq(ConversationMessageEntity::getSessionId, sessionId)
                .eq(ConversationMessageEntity::getClientMessageId, request.clientMessageId()));
        if (existing != null && existing > 0) {
            throw new BusinessException(ErrorCode.RESOURCE_CONFLICT, "message already exists");
        }

        ConversationMessageEntity userMessage = new ConversationMessageEntity()
                .setMessageId("msg_" + UUID.randomUUID().toString().replace("-", ""))
                .setTenantId(context.tenantId())
                .setSessionId(sessionId)
                .setSenderType("USER")
                .setSenderId(context.userId())
                .setClientMessageId(request.clientMessageId())
                .setContent(request.content())
                .setAttachmentsJson(request.attachments() == null ? null : jsonSupport.write(request.attachments()));
        messageMapper.insert(userMessage);
        sessionMapper.updateById(new ConversationSessionEntity()
                .setSessionId(sessionId)
                .setSummary(request.content().length() > 100 ? request.content().substring(0, 100) : request.content())
                .setLastMessageAt(LocalDateTime.now()));
        chatCacheService.evict(sessionId);

        return new ConversationDtos.SendMessageResponse(userMessage.getMessageId(), sessionId, "AGENT_PROCESSING", null, null, session.getStatus());
    }

    /**
     * 记录 Agent 结构化决策。
     */
    @Transactional
    public ConversationDtos.AgentDecisionResponse recordDecision(RequestContext context, String sessionId,
                                                                 ConversationDtos.AgentDecisionRequest request) {
        ConversationSessionEntity session = requireSession(context.tenantId(), sessionId);
        AgentDecisionEntity decision = new AgentDecisionEntity()
                .setDecisionId("dec_" + UUID.randomUUID().toString().replace("-", ""))
                .setTenantId(context.tenantId())
                .setSessionId(sessionId)
                .setDecision(request.decision())
                .setConfidence(request.confidence() == null ? BigDecimal.ZERO : request.confidence())
                .setBusinessLineCode(request.businessLineCode())
                .setSummary(request.summary())
                .setHandoffReason(request.handoffReason())
                .setSuggestedManagementUnitId(request.suggestedManagementUnitId())
                .setSuggestedSymptomId(request.suggestedSymptomId());
        decisionMapper.insert(decision);
        return new ConversationDtos.AgentDecisionResponse(sessionId, request.decision(), session.getStatus(), session.getTicketId(), null, false);
    }

    /**
     * 用户主动结束会话：归档会话并清理 Redis 缓存。
     */
    @Transactional
    public ConversationDtos.SessionDetailResponse endSession(RequestContext context, String sessionId) {
        ConversationSessionEntity session = requireSession(context.tenantId(), sessionId);
        if (!session.getUserId().equals(context.userId())) {
            throw new BusinessException(ErrorCode.DATA_SCOPE_FORBIDDEN);
        }
        archiveSession(context.tenantId(), sessionId);
        return getSession(context, sessionId, 1, 50);
    }

    /**
     * 归档会话（工单闭环 / 用户结束会话时调用）：状态置为 ARCHIVED 并清理缓存。
     */
    @Transactional
    public void archiveSession(String tenantId, String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        sessionMapper.update(null, new LambdaUpdateWrapper<ConversationSessionEntity>()
                .eq(ConversationSessionEntity::getTenantId, tenantId)
                .eq(ConversationSessionEntity::getSessionId, sessionId)
                .set(ConversationSessionEntity::getStatus, "ARCHIVED"));
        chatCacheService.evict(sessionId);
    }

    private ConversationSessionEntity requireSession(String tenantId, String sessionId) {
        ConversationSessionEntity session = sessionMapper.selectOne(new LambdaQueryWrapper<ConversationSessionEntity>()
                .eq(ConversationSessionEntity::getTenantId, tenantId)
                .eq(ConversationSessionEntity::getSessionId, sessionId));
        if (session == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "session not found");
        }
        return session;
    }
}