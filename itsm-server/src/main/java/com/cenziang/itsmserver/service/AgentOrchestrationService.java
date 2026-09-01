package com.cenziang.itsmserver.service;

import com.cenziang.itsmcommon.api.BusinessException;
import com.cenziang.itsmcommon.api.ErrorCode;
import com.cenziang.itsmpojo.dto.ConversationDtos;
import com.cenziang.itsmpojo.dto.TicketDtos;
import com.cenziang.itsmpojo.entity.AgentDecisionEntity;
import com.cenziang.itsmpojo.entity.ConversationSessionEntity;
import com.cenziang.itsmserver.application.RequestContext;
import com.cenziang.itsmserver.infrastructure.persistence.mapper.AgentDecisionMapper;
import com.cenziang.itsmserver.infrastructure.persistence.mapper.ConversationSessionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Agent 编排服务。
 * <p>
 * 负责两件事：
 * 1. 接收内部 AI Agent 或人工转接决策，必要时自动创建服务请求；
 * 2. 对接 Agent 外部决策接口（/api/v1/agent/decisions），保证决策落库并驱动后续工单流程。
 * </p>
 */
@Service
public class AgentOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(AgentOrchestrationService.class);

    private final ObjectProvider<ConversationService> conversationServiceProvider;
    private final TicketService ticketService;
    private final ConversationSessionMapper sessionMapper;
    private final AgentDecisionMapper decisionMapper;

    public AgentOrchestrationService(ObjectProvider<ConversationService> conversationServiceProvider,
                                     TicketService ticketService,
                                     ConversationSessionMapper sessionMapper,
                                     AgentDecisionMapper decisionMapper) {
        this.conversationServiceProvider = conversationServiceProvider;
        this.ticketService = ticketService;
        this.sessionMapper = sessionMapper;
        this.decisionMapper = decisionMapper;
    }

    /**
     * 用户在聊天界面主动请求转人工：记录决策并创建服务请求。
     */
    @Transactional
    public ConversationDtos.AgentDecisionResponse userHandoff(RequestContext context,
                                                              String sessionId,
                                                              ConversationDtos.HandoffRequest request) {
        ConversationDtos.AgentDecisionRequest decisionRequest = new ConversationDtos.AgentDecisionRequest(
                "HANDOFF",
                null,
                BigDecimal.ZERO,
                request.businessLineCode(),
                null,
                request.reason(),
                null,
                null
        );
        return handleDecision(context, sessionId, decisionRequest);
    }

    /**
     * 内部 AI Agent 自动转人工：写入 Agent 回复后由调用方触发。
     */
    @Transactional
    public void handleAutoHandoff(RequestContext context,
                                  String sessionId,
                                  String sessionSubject,
                                  String triggerMessage,
                                  AiAgentService.AiAgentResult aiResult) {
        ConversationSessionEntity session = requireSession(context.tenantId(), sessionId);
        if (session.getTicketId() != null && !session.getTicketId().isBlank()) {
            log.info("AI 自动转人工命中已有工单: sessionId={}, ticketId={}", sessionId, session.getTicketId());
            return;
        }

        String title = firstNonBlank(session.getSubject(), session.getSummary(), triggerMessage, "转人工服务");
        String description = firstNonBlank(aiResult.handoffReason(), aiResult.response(), triggerMessage, "用户请求人工处理");
        String businessLineCode = firstNonBlank(aiResult.classification(), "IT_SUPPORT");

        TicketDtos.CreateTicketResponse createdTicket = ticketService.create(
                buildAgentContext(context, session.getUserId()),
                new TicketDtos.CreateTicketRequest(
                        "AGENT_HANDOFF",
                        sessionId,
                        title,
                        description,
                        businessLineCode,
                        priorityFromAi(aiResult.priority()),
                        null,
                        null
                )
        );

        sessionMapper.update(null, new LambdaUpdateWrapper<ConversationSessionEntity>()
                .eq(ConversationSessionEntity::getTenantId, context.tenantId())
                .eq(ConversationSessionEntity::getSessionId, sessionId)
                .set(ConversationSessionEntity::getTicketId, createdTicket.ticketId())
                .set(ConversationSessionEntity::getStatus, "TICKET_CREATED"));

        conversationServiceProvider.getObject().sendSystemMessage(context.tenantId(), sessionId,
                "已自动为你创建服务请求 " + createdTicket.ticketNo() + "，客服即将接入处理。");

        log.info("AI 自动转人工完成: sessionId={}, ticketId={}, ticketNo={}", sessionId, createdTicket.ticketId(), createdTicket.ticketNo());
    }

    /**
     * 接收外部 Agent / 内部编排决策。
     */
    @Transactional
    public ConversationDtos.AgentDecisionResponse recordDecision(RequestContext context,
                                                                 String sessionId,
                                                                 ConversationDtos.AgentDecisionRequest request) {
        return handleDecision(context, sessionId, request);
    }

    @Transactional
    protected ConversationDtos.AgentDecisionResponse handleDecision(RequestContext context,
                                                                    String sessionId,
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

        String ticketId = session.getTicketId();
        String ticketStatus = null;
        boolean createdTicket = false;

        if ("HANDOFF".equalsIgnoreCase(request.decision()) || "HANDOFF_PENDING".equalsIgnoreCase(request.decision())) {
            sessionMapper.update(null, new LambdaUpdateWrapper<ConversationSessionEntity>()
                    .eq(ConversationSessionEntity::getTenantId, context.tenantId())
                    .eq(ConversationSessionEntity::getSessionId, sessionId)
                    .set(ConversationSessionEntity::getStatus, "HANDOFF_PENDING"));

            if (ticketId == null || ticketId.isBlank()) {
                String title = firstNonBlank(session.getSubject(), session.getSummary(), request.summary(), "转人工服务");
                String description = firstNonBlank(request.handoffReason(), request.summary(), "用户请求人工处理");
                TicketDtos.CreateTicketResponse created = ticketService.create(
                        buildAgentContext(context, session.getUserId()),
                        new TicketDtos.CreateTicketRequest(
                                "AGENT_HANDOFF",
                                sessionId,
                                title,
                                description,
                                request.businessLineCode(),
                                null,
                                null,
                                null
                        )
                );
                ticketId = created.ticketId();
                ticketStatus = created.status();
                createdTicket = true;

                sessionMapper.update(null, new LambdaUpdateWrapper<ConversationSessionEntity>()
                        .eq(ConversationSessionEntity::getTenantId, context.tenantId())
                        .eq(ConversationSessionEntity::getSessionId, sessionId)
                        .set(ConversationSessionEntity::getTicketId, created.ticketId())
                        .set(ConversationSessionEntity::getStatus, "TICKET_CREATED"));
            }
        }

        return new ConversationDtos.AgentDecisionResponse(
                sessionId,
                request.decision(),
                createdTicket ? "TICKET_CREATED" : session.getStatus(),
                ticketId,
                ticketStatus,
                true
        );
    }

    private RequestContext buildAgentContext(RequestContext original, String requesterUserId) {
        return new RequestContext(
                original.tenantId(),
                requesterUserId,
                original.roles(),
                original.permissionsVersion(),
                original.authVersion()
        );
    }

    private ConversationSessionEntity requireSession(String tenantId, String sessionId) {
        ConversationSessionEntity session = sessionMapper.selectOne(
                new LambdaQueryWrapper<ConversationSessionEntity>()
                        .eq(ConversationSessionEntity::getTenantId, tenantId)
                        .eq(ConversationSessionEntity::getSessionId, sessionId));
        if (session == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "session not found");
        }
        return session;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String priorityFromAi(String priority) {
        if (priority == null || priority.isBlank()) {
            return null;
        }
        return switch (priority.toLowerCase()) {
            case "high", "urgent" -> "HIGH";
            case "low" -> "LOW";
            default -> "MEDIUM";
        };
    }
}
