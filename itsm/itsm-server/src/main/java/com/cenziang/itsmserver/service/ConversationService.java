package com.cenziang.itsmserver.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cenziang.itsmcommon.api.BusinessException;
import com.cenziang.itsmcommon.api.ErrorCode;
import com.cenziang.itsmcommon.api.PageResponse;
import com.cenziang.itsmpojo.dto.ConversationDtos;
import com.cenziang.itsmpojo.entity.AgentDecisionEntity;
import com.cenziang.itsmpojo.entity.AppUserEntity;
import com.cenziang.itsmpojo.entity.ConversationMessageEntity;
import com.cenziang.itsmpojo.entity.ConversationParticipantEntity;
import com.cenziang.itsmpojo.entity.ConversationSessionEntity;
import com.cenziang.itsmserver.application.RequestContext;
import com.cenziang.itsmserver.infrastructure.JsonSupport;
import com.cenziang.itsmserver.infrastructure.persistence.mapper.AgentDecisionMapper;
import com.cenziang.itsmserver.infrastructure.persistence.mapper.AppUserMapper;
import com.cenziang.itsmserver.infrastructure.persistence.mapper.ConversationMessageMapper;
import com.cenziang.itsmserver.infrastructure.persistence.mapper.ConversationParticipantMapper;
import com.cenziang.itsmserver.infrastructure.persistence.mapper.ConversationSessionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final ConversationParticipantMapper participantMapper;
    private final AgentDecisionMapper decisionMapper;
    private final AppUserMapper appUserMapper;
    private final JsonSupport jsonSupport;
    private final ChatCacheService chatCacheService;
    private final OutboxService outboxService;

    public ConversationService(ConversationSessionMapper sessionMapper,
                               ConversationMessageMapper messageMapper,
                               ConversationParticipantMapper participantMapper,
                               AgentDecisionMapper decisionMapper,
                               AppUserMapper appUserMapper,
                               JsonSupport jsonSupport,
                               ChatCacheService chatCacheService,
                               OutboxService outboxService) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.participantMapper = participantMapper;
        this.decisionMapper = decisionMapper;
        this.appUserMapper = appUserMapper;
        this.jsonSupport = jsonSupport;
        this.chatCacheService = chatCacheService;
        this.outboxService = outboxService;
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
        ensureParticipant(context.tenantId(), session.getSessionId(), context.userId(), "USER");
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
     * 查询当前用户参与的所有会话（客服端消息面板用）。
     */
    @Transactional(readOnly = true)
    public PageResponse<ConversationDtos.SessionListItem> listMySessions(RequestContext context, int page, int pageSize) {
        List<ConversationParticipantEntity> participants = participantMapper.selectList(
                new LambdaQueryWrapper<ConversationParticipantEntity>()
                        .eq(ConversationParticipantEntity::getTenantId, context.tenantId())
                        .eq(ConversationParticipantEntity::getUserId, context.userId()));
        List<String> sessionIds = participants.stream()
                .map(ConversationParticipantEntity::getSessionId)
                .distinct()
                .toList();
        if (sessionIds.isEmpty()) {
            return PageResponse.of(List.of(), page, pageSize, 0);
        }
        Page<ConversationSessionEntity> result = sessionMapper.selectPage(
                new Page<>(page, pageSize),
                new LambdaQueryWrapper<ConversationSessionEntity>()
                        .eq(ConversationSessionEntity::getTenantId, context.tenantId())
                        .in(ConversationSessionEntity::getSessionId, sessionIds)
                        .orderByDesc(ConversationSessionEntity::getLastMessageAt)
                        .orderByDesc(ConversationSessionEntity::getCreatedAt));
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
        List<ConversationDtos.ParticipantView> participants = listParticipants(context.tenantId(), sessionId);
        Map<String, String> senderNameMap = new HashMap<>();
        for (ConversationDtos.ParticipantView p : participants) {
            if (p.displayAlias() != null) {
                senderNameMap.put(p.userId(), p.displayAlias());
            }
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
                    .map(m -> new ConversationDtos.SessionMessageItem(
                            m.getMessageId(), m.getSenderType(), m.getSenderId(),
                            senderDisplayName(m.getSenderType(), m.getSenderId(), senderNameMap),
                            m.getContent(), m.getCreatedAt()))
                    .toList();
        });
        return new ConversationDtos.SessionDetailResponse(
                session.getSessionId(), session.getStatus(), session.getTicketId(), session.getSubject(),
                session.getSummary(), participants,
                PageResponse.of(messages, messagePage, messagePageSize, messages.size()));
    }

    /**
     * 发送用户消息，并写入 Agent 首轮决策结果。
     */
    @Transactional
    public ConversationDtos.SendMessageResponse sendMessage(RequestContext context, String sessionId,
                                                            ConversationDtos.SendMessageRequest request) {
        ConversationSessionEntity session = requireSession(context.tenantId(), sessionId);
        boolean isOwner = session.getUserId().equals(context.userId());
        boolean isSupport = context.roles().contains("SUPPORT_AGENT")
                || context.roles().contains("SUPPORT_ADMIN")
                || context.roles().contains("SUPERVISOR");
        if (!isOwner && !isSupport) {
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

        String senderType = isOwner ? "USER" : (isSupport ? "SUPPORT" : "AGENT");
        if (isOwner) {
            ensureParticipant(context.tenantId(), sessionId, context.userId(), "USER");
        } else {
            ensureParticipant(context.tenantId(), sessionId, context.userId(), "SUPPORT");
        }

        ConversationMessageEntity userMessage = new ConversationMessageEntity()
                .setMessageId("msg_" + UUID.randomUUID().toString().replace("-", ""))
                .setTenantId(context.tenantId())
                .setSessionId(sessionId)
                .setSenderType(senderType)
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
        // 查发送者姓名，供 WebSocket 广播直接使用
        String senderName = null;
        if (!"AGENT".equals(senderType) && !"SYSTEM".equals(senderType)) {
            AppUserEntity sender = appUserMapper.selectOne(
                    new LambdaQueryWrapper<AppUserEntity>()
                            .eq(AppUserEntity::getTenantId, context.tenantId())
                            .eq(AppUserEntity::getUserId, context.userId())
                            .select(AppUserEntity::getDisplayName));
            if (sender != null) senderName = sender.getDisplayName();
        } else if ("AGENT".equals(senderType)) {
            senderName = "IT助手";
        }
        outboxService.publish(context.tenantId(), "MESSAGE_SENT", "CONVERSATION", sessionId,
                Map.of("messageId", userMessage.getMessageId(), "senderType", senderType,
                        "senderId", context.userId(), "content", request.content(),
                        "senderDisplayName", senderName != null ? senderName : ""));

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

    /**
     * 将已归档的会话重新激活（工单重开时调用）。
     */
    public void reactivateSession(String tenantId, String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        sessionMapper.update(null, new LambdaUpdateWrapper<ConversationSessionEntity>()
                .eq(ConversationSessionEntity::getTenantId, tenantId)
                .eq(ConversationSessionEntity::getSessionId, sessionId)
                .set(ConversationSessionEntity::getStatus, "ACTIVE"));
        chatCacheService.evict(sessionId);
    }

    /**
     * 发送系统消息（无操作者上下文，供工单流转等自动通知使用）。
     */
    @Transactional
    public void sendSystemMessage(String tenantId, String sessionId, String content) {
        if (sessionId == null || sessionId.isBlank() || content == null || content.isBlank()) {
            return;
        }
        ConversationMessageEntity msg = new ConversationMessageEntity()
                .setMessageId("msg_" + UUID.randomUUID().toString().replace("-", ""))
                .setTenantId(tenantId)
                .setSessionId(sessionId)
                .setSenderType("SYSTEM")
                .setSenderId("SYSTEM")
                .setClientMessageId("sys_" + UUID.randomUUID().toString().replace("-", ""))
                .setContent(content);
        messageMapper.insert(msg);
        sessionMapper.updateById(new ConversationSessionEntity()
                .setSessionId(sessionId)
                .setSummary(content.length() > 100 ? content.substring(0, 100) : content)
                .setLastMessageAt(LocalDateTime.now()));
        chatCacheService.evict(sessionId);
        outboxService.publish(tenantId, "MESSAGE_SENT", "CONVERSATION", sessionId,
                Map.of("messageId", msg.getMessageId(), "senderType", "SYSTEM", "senderId", "SYSTEM", "content", content));
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

    /**
     * 转人工时把会话与工单绑定，并把会话主题改为「工单号 + 问题简述」群名。
     */
    @Transactional
    public void linkTicket(String tenantId, String sessionId, String ticketId, String groupSubject) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        sessionMapper.update(null, new LambdaUpdateWrapper<ConversationSessionEntity>()
                .eq(ConversationSessionEntity::getTenantId, tenantId)
                .eq(ConversationSessionEntity::getSessionId, sessionId)
                .set(ConversationSessionEntity::getTicketId, ticketId)
                .set(ConversationSessionEntity::getSubject, groupSubject));
        chatCacheService.evict(sessionId);
    }

    /**
     * 把会话升级为群：确保员工本人是 USER 参与者（转人工时调用）。
     */
    @Transactional
    public void ensureGroup(String tenantId, String sessionId, String ownerUserId) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        ensureParticipant(tenantId, sessionId, ownerUserId, "USER");
    }

    /**
     * 把某个客服加入群（后续“转让给同事一起讨论”也走这里）。
     */
    @Transactional
    public void addSupportParticipant(String tenantId, String sessionId, String supportUserId) {
        if (sessionId == null || sessionId.isBlank() || supportUserId == null || supportUserId.isBlank()) {
            return;
        }
        ensureParticipant(tenantId, sessionId, supportUserId, "SUPPORT");
    }

    private void ensureParticipant(String tenantId, String sessionId, String userId, String participantType) {
        Long count = participantMapper.selectCount(new LambdaQueryWrapper<ConversationParticipantEntity>()
                .eq(ConversationParticipantEntity::getTenantId, tenantId)
                .eq(ConversationParticipantEntity::getSessionId, sessionId)
                .eq(ConversationParticipantEntity::getUserId, userId));
        if (count != null && count > 0) {
            return;
        }
        participantMapper.insert(new ConversationParticipantEntity()
                .setParticipantId("ptp_" + UUID.randomUUID().toString().replace("-", ""))
                .setTenantId(tenantId)
                .setSessionId(sessionId)
                .setUserId(userId)
                .setParticipantType(participantType)
                .setJoinedAt(LocalDateTime.now()));
    }

    private List<ConversationDtos.ParticipantView> listParticipants(String tenantId, String sessionId) {
        List<ConversationParticipantEntity> rows = participantMapper.selectList(
                new LambdaQueryWrapper<ConversationParticipantEntity>()
                        .eq(ConversationParticipantEntity::getTenantId, tenantId)
                        .eq(ConversationParticipantEntity::getSessionId, sessionId)
                        .orderByAsc(ConversationParticipantEntity::getJoinedAt));
        // 预加载所有参与者的真实姓名
        Set<String> allUserIds = rows.stream()
                .map(ConversationParticipantEntity::getUserId)
                .collect(java.util.stream.Collectors.toSet());
        Map<String, String> realNameMap = new HashMap<>();
        if (!allUserIds.isEmpty()) {
            appUserMapper.selectList(
                    new LambdaQueryWrapper<AppUserEntity>()
                            .eq(AppUserEntity::getTenantId, tenantId)
                            .in(AppUserEntity::getUserId, allUserIds))
                    .forEach(u -> realNameMap.put(u.getUserId(), u.getDisplayName()));
        }
        List<ConversationDtos.ParticipantView> result = new ArrayList<>();
        for (ConversationParticipantEntity p : rows) {
            String alias = realNameMap.getOrDefault(p.getUserId(), null);
            result.add(new ConversationDtos.ParticipantView(p.getUserId(), p.getParticipantType(), alias));
        }
        return result;
    }

    private String senderDisplayName(String senderType, String senderId, Map<String, String> nameMap) {
        if ("AGENT".equals(senderType)) {
            return "IT助手";
        }
        return nameMap.getOrDefault(senderId, null);
    }
}