package com.cenziang.itsm.application;

import com.cenziang.itsm.agent.AgentAnswer;
import com.cenziang.itsm.agent.PythonAgentClient;
import com.cenziang.itsm.domain.BusinessException;
import com.cenziang.itsm.domain.CustomerType;
import com.cenziang.itsm.domain.Ticket;
import com.cenziang.itsm.domain.TicketPriority;
import com.cenziang.itsm.domain.TicketStatus;
import com.cenziang.itsm.dto.AnalysisRequest;
import com.cenziang.itsm.dto.EvaluationRequest;
import com.cenziang.itsm.dto.HandoffRequest;
import com.cenziang.itsm.dto.ResolveRequest;
import com.cenziang.itsm.dto.SubmitQuestionRequest;
import com.cenziang.itsm.dto.SupportCommandRequest;
import com.cenziang.itsm.dto.TicketDetailResponse;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class TicketWorkflowService {
    private static final double AGENT_CONFIDENCE_THRESHOLD = 0.65;
    private static final Set<String> SUPPORT_ROLES = Set.of("SUPPORT_AGENT", "TECH_EXPERT", "ADMIN");
    private final PythonAgentClient pythonAgentClient;
    private final AtomicLong ticketSequence = new AtomicLong(1000);
    private final Map<String, Ticket> tickets = new ConcurrentHashMap<>();

    public TicketWorkflowService(PythonAgentClient pythonAgentClient) {
        this.pythonAgentClient = pythonAgentClient;
    }

    public TicketDetailResponse submitQuestion(RequestContext context, SubmitQuestionRequest request) {
        requireTenant(context);
        requireActor(context);
        requireText(request.title(), "title");
        requireText(request.description(), "description");

        String ticketId = "TCK-" + ticketSequence.incrementAndGet();
        Ticket ticket = Ticket.create(
                ticketId,
                context.tenantId(),
                context.actorId(),
                context.customerType() == null ? CustomerType.INTERNAL : context.customerType(),
                request.title().trim(),
                request.description().trim(),
                blankToDefault(request.category(), "DESKTOP"),
                TicketPriority.from(request.priority()),
                request.environment(),
                request.attachments()
        );
        tickets.put(ticketId, ticket);

        ticket.transitionTo(TicketStatus.AGENT_PROCESSING, "system", "提交后进入 Agent 预处理");
        AgentAnswer answer = pythonAgentClient.answer(request);
        ticket.applyAgentAnswer(
                answer.sessionId(),
                answer.answer(),
                answer.confidence(),
                answer.suggestedHandoff(),
                answer.sourceSummary()
        );

        if (answer.suggestedHandoff() || answer.confidence() < AGENT_CONFIDENCE_THRESHOLD) {
            ticket.transitionTo(TicketStatus.PENDING_HUMAN, "python-agent-reserved", "Agent 置信度不足或建议转人工");
        } else {
            ticket.transitionTo(TicketStatus.AGENT_ANSWERED, "python-agent-reserved", "Agent 已回答，等待用户确认");
        }
        return TicketDetailResponse.from(ticket);
    }

    public TicketDetailResponse getTicket(RequestContext context, String ticketId) {
        Ticket ticket = loadTicket(context, ticketId);
        if (!isSupport(context) && !ticket.requesterId().equals(context.actorId())) {
            throw BusinessException.forbidden("只能查看本人或授权范围内的工单");
        }
        return TicketDetailResponse.from(ticket);
    }

    public List<TicketDetailResponse> supportQueue(RequestContext context) {
        requireSupport(context);
        return tickets.values().stream()
                .filter(ticket -> ticket.tenantId().equals(context.tenantId()))
                .filter(ticket -> ticket.status() == TicketStatus.PENDING_HUMAN
                        || ticket.status() == TicketStatus.REOPENED
                        || ticket.status() == TicketStatus.ACCEPTED
                        || ticket.status() == TicketStatus.TECH_ANALYSIS
                        || ticket.status() == TicketStatus.IN_SUPPORT)
                .sorted(Comparator.comparing(Ticket::updatedAt).reversed())
                .map(TicketDetailResponse::from)
                .toList();
    }

    public TicketDetailResponse handoff(RequestContext context, String ticketId, HandoffRequest request) {
        Ticket ticket = loadTicket(context, ticketId);
        if (!ticket.requesterId().equals(context.actorId())) {
            throw BusinessException.forbidden("只有提交人可以主动转人工");
        }
        ensureStatus(ticket, Set.of(TicketStatus.AGENT_ANSWERED, TicketStatus.SUBMITTED, TicketStatus.AGENT_PROCESSING), "当前状态不允许转人工");
        String reason = blankToDefault(request.reason(), "用户主动转人工");
        ticket.markHandoff(context.actorId(), reason);
        ticket.transitionTo(TicketStatus.PENDING_HUMAN, context.actorId(), reason);
        return TicketDetailResponse.from(ticket);
    }

    public TicketDetailResponse accept(RequestContext context, String ticketId, SupportCommandRequest request) {
        requireSupport(context);
        Ticket ticket = loadTicket(context, ticketId);
        ensureStatus(ticket, Set.of(TicketStatus.PENDING_HUMAN, TicketStatus.REOPENED), "只有待人工或重开的工单可以受理");
        String operatorId = operatorId(context, request.operatorId());
        ticket.accept(operatorId, blankToDefault(request.note(), "客服受理"));
        ticket.transitionTo(TicketStatus.ACCEPTED, operatorId, "客服受理");
        return TicketDetailResponse.from(ticket);
    }

    public TicketDetailResponse analyze(RequestContext context, String ticketId, AnalysisRequest request) {
        requireSupport(context);
        requireText(request.analysis(), "analysis");
        Ticket ticket = loadTicket(context, ticketId);
        ensureStatus(ticket, Set.of(TicketStatus.ACCEPTED, TicketStatus.TECH_ANALYSIS), "只有已受理工单可以进入技术分析");
        String operatorId = operatorId(context, request.operatorId());
        ticket.analyze(operatorId, request.analysis().trim());
        ticket.transitionTo(TicketStatus.TECH_ANALYSIS, operatorId, "完成技术分析记录");
        return TicketDetailResponse.from(ticket);
    }

    public TicketDetailResponse support(RequestContext context, String ticketId, SupportCommandRequest request) {
        requireSupport(context);
        Ticket ticket = loadTicket(context, ticketId);
        ensureStatus(ticket, Set.of(TicketStatus.TECH_ANALYSIS, TicketStatus.IN_SUPPORT), "只有技术分析中的工单可以进入技术支持");
        String operatorId = operatorId(context, request.operatorId());
        ticket.support(operatorId, blankToDefault(request.note(), "技术支持处理中"));
        ticket.transitionTo(TicketStatus.IN_SUPPORT, operatorId, "进入技术支持");
        return TicketDetailResponse.from(ticket);
    }

    public TicketDetailResponse resolve(RequestContext context, String ticketId, ResolveRequest request) {
        requireSupport(context);
        requireText(request.solution(), "solution");
        Ticket ticket = loadTicket(context, ticketId);
        ensureStatus(ticket, Set.of(TicketStatus.ACCEPTED, TicketStatus.TECH_ANALYSIS, TicketStatus.IN_SUPPORT), "当前状态不允许提交解决方案");
        String operatorId = operatorId(context, request.operatorId());
        ticket.resolve(operatorId, request.solution().trim());
        ticket.transitionTo(TicketStatus.PENDING_USER_CONFIRM, operatorId, "客服提交解决方案，等待用户确认");
        return TicketDetailResponse.from(ticket);
    }

    public TicketDetailResponse confirmResolved(RequestContext context, String ticketId) {
        Ticket ticket = loadTicket(context, ticketId);
        if (!ticket.requesterId().equals(context.actorId())) {
            throw BusinessException.forbidden("只有提交人可以确认解决");
        }
        ensureStatus(ticket, Set.of(TicketStatus.AGENT_ANSWERED, TicketStatus.PENDING_USER_CONFIRM), "当前状态不允许确认解决");
        ticket.transitionTo(TicketStatus.RESOLVED, context.actorId(), "用户确认解决");
        return TicketDetailResponse.from(ticket);
    }

    public TicketDetailResponse evaluate(RequestContext context, String ticketId, EvaluationRequest request) {
        Ticket ticket = loadTicket(context, ticketId);
        if (!ticket.requesterId().equals(context.actorId())) {
            throw BusinessException.forbidden("只有提交人可以评价工单");
        }
        ensureStatus(ticket, Set.of(TicketStatus.RESOLVED, TicketStatus.CLOSED), "只有已解决或已关闭工单可以评价");
        if (request.rating() < 1 || request.rating() > 5) {
            throw BusinessException.validation("rating 必须在 1 到 5 之间");
        }
        ticket.evaluate(request.rating(), blankToDefault(request.comment(), "用户未填写文字评价"));
        return TicketDetailResponse.from(ticket);
    }

    public TicketDetailResponse close(RequestContext context, String ticketId, SupportCommandRequest request) {
        requireSupport(context);
        Ticket ticket = loadTicket(context, ticketId);
        ensureStatus(ticket, Set.of(TicketStatus.RESOLVED), "只有已解决工单可以关闭");
        ticket.transitionTo(TicketStatus.CLOSED, operatorId(context, request.operatorId()), blankToDefault(request.note(), "客服关闭工单"));
        return TicketDetailResponse.from(ticket);
    }

    public TicketDetailResponse reopen(RequestContext context, String ticketId, HandoffRequest request) {
        Ticket ticket = loadTicket(context, ticketId);
        if (!ticket.requesterId().equals(context.actorId())) {
            throw BusinessException.forbidden("只有提交人可以重开工单");
        }
        ensureStatus(ticket, Set.of(TicketStatus.PENDING_USER_CONFIRM, TicketStatus.RESOLVED, TicketStatus.CLOSED), "当前状态不允许重开");
        ticket.markHandoff(context.actorId(), blankToDefault(request.reason(), "用户反馈未解决"));
        ticket.transitionTo(TicketStatus.REOPENED, context.actorId(), "用户重开工单");
        return TicketDetailResponse.from(ticket);
    }

    private Ticket loadTicket(RequestContext context, String ticketId) {
        requireTenant(context);
        requireText(ticketId, "ticketId");
        Ticket ticket = tickets.get(ticketId);
        if (ticket == null) {
            throw BusinessException.notFound("工单不存在：" + ticketId);
        }
        if (!ticket.tenantId().equals(context.tenantId())) {
            throw BusinessException.forbidden("禁止跨租户访问工单");
        }
        return ticket;
    }

    private void ensureStatus(Ticket ticket, Set<TicketStatus> allowed, String message) {
        if (!allowed.contains(ticket.status())) {
            throw BusinessException.illegalStatus(message + "，当前状态：" + ticket.status());
        }
    }

    private void requireTenant(RequestContext context) {
        if (context == null || context.tenantId() == null || context.tenantId().isBlank()) {
            throw BusinessException.validation("缺少 X-Tenant-Id");
        }
    }

    private void requireActor(RequestContext context) {
        if (context.actorId() == null || context.actorId().isBlank()) {
            throw BusinessException.validation("缺少操作者 ID");
        }
    }

    private void requireSupport(RequestContext context) {
        requireTenant(context);
        requireActor(context);
        if (!isSupport(context)) {
            throw BusinessException.forbidden("当前角色无客服操作权限");
        }
    }

    private boolean isSupport(RequestContext context) {
        return context != null && context.role() != null
                && SUPPORT_ROLES.contains(context.role().trim().toUpperCase(Locale.ROOT));
    }

    private void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw BusinessException.validation(field + " 不能为空");
        }
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private String operatorId(RequestContext context, String requestOperatorId) {
        if (requestOperatorId != null && !requestOperatorId.isBlank()) {
            return requestOperatorId.trim();
        }
        return context.actorId();
    }
}
