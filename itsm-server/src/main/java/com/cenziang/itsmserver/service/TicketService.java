package com.cenziang.itsmserver.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cenziang.itsmcommon.api.BusinessException;
import com.cenziang.itsmcommon.api.ErrorCode;
import com.cenziang.itsmcommon.api.PageResponse;
import com.cenziang.itsmpojo.dto.RatingDtos;
import com.cenziang.itsmpojo.dto.TicketDtos;
import com.cenziang.itsmpojo.entity.ConversationMessageEntity;
import com.cenziang.itsmpojo.entity.RatingEntity;
import com.cenziang.itsmpojo.entity.TicketClassificationEntity;
import com.cenziang.itsmpojo.entity.TicketEntity;
import com.cenziang.itsmpojo.entity.TicketStatusHistoryEntity;
import com.cenziang.itsmserver.application.RequestContext;
import com.cenziang.itsmserver.infrastructure.JsonSupport;
import com.cenziang.itsmserver.infrastructure.audit.AuditService;
import com.cenziang.itsmserver.infrastructure.persistence.mapper.ConversationMessageMapper;
import com.cenziang.itsmserver.infrastructure.persistence.mapper.RatingMapper;
import com.cenziang.itsmserver.infrastructure.persistence.mapper.TicketClassificationMapper;
import com.cenziang.itsmserver.infrastructure.persistence.mapper.TicketMapper;
import com.cenziang.itsmserver.infrastructure.persistence.mapper.TicketStatusHistoryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 工单服务。
 * <p>
 * 这是工单生命周期的核心状态机，创建、受理、分类、解决、确认、关闭、重开和评价都从这里执行。
 * </p>
 */
@Service
public class TicketService {
    private static final Set<String> USER_ROLES = Set.of("USER");
    private static final Set<String> SUPPORT_ROLES = Set.of("SUPPORT_AGENT", "SUPPORT_ADMIN");
    private static final Set<String> READ_ONLY_ROLES = Set.of("SUPERVISOR");

    private final TicketMapper ticketMapper;
    private final TicketClassificationMapper classificationMapper;
    private final TicketStatusHistoryMapper statusHistoryMapper;
    private final RatingMapper ratingMapper;
    private final ConversationMessageMapper messageMapper;
    private final JsonSupport jsonSupport;
    private final AuditService auditService;
    private final ConversationService conversationService;
    private final AtomicLong ticketSequence = new AtomicLong(1000);

    public TicketService(TicketMapper ticketMapper, TicketClassificationMapper classificationMapper,
                         TicketStatusHistoryMapper statusHistoryMapper, RatingMapper ratingMapper,
                         ConversationMessageMapper messageMapper, JsonSupport jsonSupport, AuditService auditService,
                         ConversationService conversationService) {
        this.ticketMapper = ticketMapper;
        this.classificationMapper = classificationMapper;
        this.statusHistoryMapper = statusHistoryMapper;
        this.ratingMapper = ratingMapper;
        this.messageMapper = messageMapper;
        this.jsonSupport = jsonSupport;
        this.auditService = auditService;
        this.conversationService = conversationService;
    }

    /**
     * 创建工单。
     */
    @Transactional
    public TicketDtos.CreateTicketResponse create(RequestContext context, TicketDtos.CreateTicketRequest request) {
        if (request.title() == null || request.title().isBlank() || request.description() == null || request.description().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "title and description are required");
        }
        if (request.source() == null || request.source().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "source is required");
        }
        TicketEntity ticket = new TicketEntity()
                .setTicketId("tkt_" + UUID.randomUUID().toString().replace("-", ""))
                .setTicketNo(String.valueOf(System.currentTimeMillis() % 10000000))
                .setTenantId(context.tenantId())
                .setSource(request.source())
                .setSessionId(request.sessionId())
                .setRequesterId(context.userId())
                .setTitle(request.title())
                .setDescription(request.description())
                .setPriority(request.priority() == null ? "MEDIUM" : request.priority())
                .setBusinessLineCode(request.businessLineCode())
                .setStatus("PENDING_ACCEPTANCE")
                .setEnvironment(request.environment())
                .setAttachmentsJson(request.attachments() == null ? null : jsonSupport.write(request.attachments()));
        ticketMapper.insert(ticket);
        recordStatus(ticket, null, "PENDING_ACCEPTANCE", context.userId(), "USER", "TICKET_CREATED", "创建工单");
        auditService.recordTicketAction(context.tenantId(), ticket.getTicketId(), "TICKET_CREATED", context.userId(), "USER", "创建工单");
        return new TicketDtos.CreateTicketResponse(ticket.getTicketId(), ticket.getTicketNo(), ticket.getStatus(),
                ticket.getBusinessLineCode(), ticket.getRequesterId(), ticket.getSessionId(), ticket.getCreatedAt());
    }

    /**
     * 工单分页查询。
     */
    @Transactional(readOnly = true)
    public PageResponse<TicketDtos.TicketPageItem> page(RequestContext context, TicketDtos.TicketPageQuery query) {
        int page = query.page() == null ? 1 : query.page();
        int pageSize = query.pageSize() == null ? 20 : query.pageSize();
        LambdaQueryWrapper<TicketEntity> wrapper = new LambdaQueryWrapper<TicketEntity>()
                .eq(TicketEntity::getTenantId, context.tenantId());
        if (!SUPPORT_ROLES.stream().anyMatch(context.roles()::contains) && !READ_ONLY_ROLES.stream().anyMatch(context.roles()::contains)) {
            wrapper.eq(TicketEntity::getRequesterId, context.userId());
        }
        if (query.status() != null && !query.status().isEmpty()) {
            wrapper.in(TicketEntity::getStatus, query.status());
        }
        wrapper.like(query.ticketNo() != null && !query.ticketNo().isBlank(), TicketEntity::getTicketNo, query.ticketNo())
                .eq(query.businessLineCode() != null && !query.businessLineCode().isBlank(), TicketEntity::getBusinessLineCode, query.businessLineCode())
                .and(query.keyword() != null && !query.keyword().isBlank(), w -> w
                        .like(TicketEntity::getTitle, query.keyword()).or().like(TicketEntity::getTicketNo, query.keyword()))
                .orderByDesc(TicketEntity::getUpdatedAt);
        Page<TicketEntity> result = ticketMapper.selectPage(new Page<>(page, pageSize), wrapper);
        List<TicketDtos.TicketPageItem> items = result.getRecords().stream()
                .map(t -> new TicketDtos.TicketPageItem(t.getTicketId(), t.getTicketNo(), t.getTitle(), t.getStatus(),
                        t.getPriority(), t.getBusinessLineCode(), t.getAssigneeId(), t.getUpdatedAt()))
                .toList();
        return PageResponse.of(items, result.getCurrent(), result.getSize(), result.getTotal());
    }

    /**
     * 工单详情查询。
     */
    @Transactional(readOnly = true)
    public TicketDtos.TicketDetailResponse detail(RequestContext context, String ticketId) {
        TicketEntity ticket = requireTicket(context.tenantId(), ticketId);
        if (!ticket.getRequesterId().equals(context.userId()) && !SUPPORT_ROLES.stream().anyMatch(context.roles()::contains)
                && !READ_ONLY_ROLES.stream().anyMatch(context.roles()::contains)) {
            throw new BusinessException(ErrorCode.DATA_SCOPE_FORBIDDEN);
        }
        TicketClassificationEntity classification = classificationMapper.selectOne(new LambdaQueryWrapper<TicketClassificationEntity>()
                .eq(TicketClassificationEntity::getTenantId, context.tenantId())
                .eq(TicketClassificationEntity::getTicketId, ticketId));
        List<TicketStatusHistoryEntity> histories = statusHistoryMapper.selectList(new LambdaQueryWrapper<TicketStatusHistoryEntity>()
                .eq(TicketStatusHistoryEntity::getTenantId, context.tenantId())
                .eq(TicketStatusHistoryEntity::getTicketId, ticketId)
                .orderByAsc(TicketStatusHistoryEntity::getOccurredAt));
        RatingEntity rating = ratingMapper.selectOne(new LambdaQueryWrapper<RatingEntity>()
                .eq(RatingEntity::getTenantId, context.tenantId())
                .eq(RatingEntity::getTicketId, ticketId));

        TicketDtos.TicketClassificationView classificationView = classification == null ? null
                : new TicketDtos.TicketClassificationView(classification.getManagementUnitId(), classification.getSymptomId(),
                classification.getReasonId(), classification.getSolutionMethodId(), classification.getCustomReason(),
                classification.getCustomSolution(), classification.getVersion());
        List<TicketDtos.TicketStatusHistoryView> historyViews = histories.stream()
                .map(h -> new TicketDtos.TicketStatusHistoryView(h.getToStatus(), h.getOccurredAt(), h.getOperatorId(), h.getActionNote()))
                .toList();
        TicketDtos.TicketRatingView ratingView = rating == null ? null
                : new TicketDtos.TicketRatingView(rating.getRatingId(), rating.getScore(), jsonSupport.readStringList(rating.getTagsJson()),
                rating.getComment(), rating.getCreatedAt());
        return new TicketDtos.TicketDetailResponse(ticket.getTicketId(), ticket.getTicketNo(), ticket.getTenantId(),
                new TicketDtos.TicketRequesterView(ticket.getRequesterId(), null, null), ticket.getTitle(), ticket.getDescription(),
                ticket.getSource(), ticket.getStatus(), ticket.getPriority(), ticket.getBusinessLineCode(), classificationView,
                ticket.getAssigneeId() == null ? null : new TicketDtos.TicketAssigneeView(ticket.getAssigneeId(), null),
                null, historyViews, List.of(), ratingView, ticket.getCreatedAt(), ticket.getUpdatedAt());
    }

    /**
     * 客服队列查询。
     */
    @Transactional(readOnly = true)
    public PageResponse<TicketDtos.SupportQueueItem> supportQueue(RequestContext context, TicketDtos.SupportQueueQuery query) {
        if (!SUPPORT_ROLES.stream().anyMatch(context.roles()::contains) && !READ_ONLY_ROLES.stream().anyMatch(context.roles()::contains)) {
            throw new BusinessException(ErrorCode.ROLE_FORBIDDEN);
        }
        int page = query.page() == null ? 1 : query.page();
        int pageSize = query.pageSize() == null ? 20 : query.pageSize();
        LambdaQueryWrapper<TicketEntity> wrapper = new LambdaQueryWrapper<TicketEntity>()
                .eq(TicketEntity::getTenantId, context.tenantId());
        String view = query.view() == null ? "PENDING" : query.view();
        switch (view) {
            case "PENDING" -> wrapper.eq(TicketEntity::getStatus, "PENDING_ACCEPTANCE");
            case "IN_PROGRESS" -> wrapper.eq(TicketEntity::getStatus, "IN_PROGRESS");
            case "PENDING_CONFIRM" -> wrapper.eq(TicketEntity::getStatus, "PENDING_USER_CONFIRM");
            case "HISTORY" -> wrapper.in(TicketEntity::getStatus, "RESOLVED", "CLOSED");
            default -> throw new BusinessException(ErrorCode.VALIDATION_ERROR, "invalid view");
        }
        wrapper.eq(query.businessLineCode() != null && !query.businessLineCode().isBlank(), TicketEntity::getBusinessLineCode, query.businessLineCode())
                .eq("ME".equalsIgnoreCase(query.assignee()), TicketEntity::getAssigneeId, context.userId())
                .orderByDesc(TicketEntity::getUpdatedAt);
        Page<TicketEntity> result = ticketMapper.selectPage(new Page<>(page, pageSize), wrapper);
        List<TicketDtos.SupportQueueItem> items = result.getRecords().stream()
                .map(t -> new TicketDtos.SupportQueueItem(t.getTicketId(), t.getTicketNo(), t.getTitle(), t.getStatus(), t.getPriority(),
                        t.getBusinessLineCode(), new TicketDtos.TicketRequesterView(t.getRequesterId(), null, null),
                        t.getAssigneeId() == null ? null : new TicketDtos.TicketAssigneeView(t.getAssigneeId(), null), t.getUpdatedAt()))
                .toList();
        return PageResponse.of(items, result.getCurrent(), result.getSize(), result.getTotal());
    }

    /**
     * 客服受理工单。
     */
    @Transactional
    public TicketDtos.TicketDetailResponse accept(RequestContext context, String ticketId, TicketDtos.SupportAcceptRequest request) {
        if (!SUPPORT_ROLES.stream().anyMatch(context.roles()::contains)) {
            throw new BusinessException(ErrorCode.ROLE_FORBIDDEN);
        }
        TicketEntity ticket = requireTicket(context.tenantId(), ticketId);
        if (!Set.of("PENDING_ACCEPTANCE", "REOPENED").contains(ticket.getStatus())) {
            throw new BusinessException(ErrorCode.ILLEGAL_STATE_TRANSITION);
        }
        ticket.setStatus("IN_PROGRESS").setAssigneeId(context.userId()).setAcceptedAt(LocalDateTime.now());
        ticket.setVersion(ticket.getVersion() + 1);
        int updated = ticketMapper.updateById(ticket);
        if (updated < 1) {
            throw new BusinessException(ErrorCode.RESOURCE_CONFLICT, "version conflict");
        }
        recordStatus(ticket, "PENDING_ACCEPTANCE", "IN_PROGRESS", context.userId(), "SUPPORT", "ACCEPT", request.note());
        return detail(context, ticketId);
    }

    /**
     * 更新工单分类。
     */
    @Transactional
    public TicketDtos.TicketDetailResponse classify(RequestContext context, String ticketId, TicketDtos.ClassificationUpdateRequest request) {
        TicketEntity ticket = requireTicket(context.tenantId(), ticketId);
        if (!SUPPORT_ROLES.stream().anyMatch(context.roles()::contains) && !ticket.getAssigneeId().equals(context.userId())) {
            throw new BusinessException(ErrorCode.DATA_SCOPE_FORBIDDEN);
        }
        if (!Set.of("IN_PROGRESS", "REOPENED").contains(ticket.getStatus())) {
            throw new BusinessException(ErrorCode.ILLEGAL_STATE_TRANSITION);
        }
        TicketClassificationEntity classification = classificationMapper.selectOne(new LambdaQueryWrapper<TicketClassificationEntity>()
                .eq(TicketClassificationEntity::getTenantId, context.tenantId())
                .eq(TicketClassificationEntity::getTicketId, ticketId));
        if (classification == null) {
            classification = new TicketClassificationEntity()
                    .setTicketId(ticketId).setTenantId(context.tenantId()).setVersion(0L);
        }
        if (request.version() != null && !request.version().equals(classification.getVersion())) {
            throw new BusinessException(ErrorCode.RESOURCE_CONFLICT, "version mismatch");
        }
        classification.setManagementUnitId(request.managementUnitId())
                .setSymptomId(request.symptomId()).setReasonId(request.reasonId())
                .setSolutionMethodId(request.solutionMethodId()).setCustomReason(request.customReason())
                .setCustomSolution(request.customSolution());
        classification.setVersion(classification.getVersion() + 1);
        if (classification.getVersion() == 1) {
            classificationMapper.insert(classification);
        } else {
            classificationMapper.updateById(classification);
        }
        return detail(context, ticketId);
    }

    /**
     * 提交解决结果。
     */
    @Transactional
    public TicketDtos.TicketDetailResponse resolve(RequestContext context, String ticketId, TicketDtos.ResolveTicketRequest request) {
        if (!SUPPORT_ROLES.stream().anyMatch(context.roles()::contains)) {
            throw new BusinessException(ErrorCode.ROLE_FORBIDDEN);
        }
        TicketEntity ticket = requireTicket(context.tenantId(), ticketId);
        if (!"IN_PROGRESS".equals(ticket.getStatus())) {
            throw new BusinessException(ErrorCode.ILLEGAL_STATE_TRANSITION);
        }
        if (request.resolution() == null || request.resolution().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "resolution is required");
        }
        ticket.setStatus("PENDING_USER_CONFIRM").setResolutionSummary(request.resolution())
                .setResolutionType(request.resolutionType()).setResolvedBy(context.userId()).setResolvedAt(LocalDateTime.now());
        ticket.setVersion(ticket.getVersion() + 1);
        int updated = ticketMapper.updateById(ticket);
        if (updated < 1) {
            throw new BusinessException(ErrorCode.RESOURCE_CONFLICT, "version conflict");
        }
        recordStatus(ticket, "IN_PROGRESS", "PENDING_USER_CONFIRM", context.userId(), "SUPPORT", "RESOLVE", request.resolution());
        return detail(context, ticketId);
    }

    /**
     * 用户确认解决。
     */
    @Transactional
    public TicketDtos.ConfirmTicketResponse confirm(RequestContext context, String ticketId) {
        TicketEntity ticket = requireTicket(context.tenantId(), ticketId);
        if (!ticket.getRequesterId().equals(context.userId())) {
            throw new BusinessException(ErrorCode.DATA_SCOPE_FORBIDDEN);
        }
        if (!"PENDING_USER_CONFIRM".equals(ticket.getStatus())) {
            throw new BusinessException(ErrorCode.ILLEGAL_STATE_TRANSITION);
        }
        ticket.setStatus("RESOLVED");
        ticket.setVersion(ticket.getVersion() + 1);
        int updated = ticketMapper.updateById(ticket);
        if (updated < 1) {
            throw new BusinessException(ErrorCode.RESOURCE_CONFLICT, "version conflict");
        }
        recordStatus(ticket, "PENDING_USER_CONFIRM", "RESOLVED", context.userId(), "USER", "CONFIRM", "用户确认解决");
        return new TicketDtos.ConfirmTicketResponse(ticketId, ticket.getResolvedAt(), true);
    }

    /**
     * 客服关闭工单。
     */
    @Transactional
    public TicketDtos.CloseTicketResponse close(RequestContext context, String ticketId, TicketDtos.CloseTicketRequest request) {
        if (!SUPPORT_ROLES.stream().anyMatch(context.roles()::contains)) {
            throw new BusinessException(ErrorCode.ROLE_FORBIDDEN);
        }
        TicketEntity ticket = requireTicket(context.tenantId(), ticketId);
        if (!"RESOLVED".equals(ticket.getStatus())) {
            throw new BusinessException(ErrorCode.ILLEGAL_STATE_TRANSITION);
        }
        ticket.setStatus("CLOSED").setCloseReason(request.closeReason()).setClosedBy(context.userId()).setClosedAt(LocalDateTime.now());
        ticket.setVersion(ticket.getVersion() + 1);
        int updated = ticketMapper.updateById(ticket);
        if (updated < 1) {
            throw new BusinessException(ErrorCode.RESOURCE_CONFLICT, "version conflict");
        }
        recordStatus(ticket, "RESOLVED", "CLOSED", context.userId(), "SUPPORT", "CLOSE", request.note());
        conversationService.archiveSession(context.tenantId(), ticket.getSessionId());
        return new TicketDtos.CloseTicketResponse(ticketId, ticket.getStatus(), ticket.getClosedAt(), ticket.getClosedBy(), true);
    }

    /**
     * 用户重开工单。
     */
    @Transactional
    public TicketDtos.ReopenTicketResponse reopen(RequestContext context, String ticketId, TicketDtos.ReopenTicketRequest request) {
        TicketEntity ticket = requireTicket(context.tenantId(), ticketId);
        if (!ticket.getRequesterId().equals(context.userId())) {
            throw new BusinessException(ErrorCode.DATA_SCOPE_FORBIDDEN);
        }
        if (!Set.of("PENDING_USER_CONFIRM", "RESOLVED", "CLOSED").contains(ticket.getStatus())) {
            throw new BusinessException(ErrorCode.ILLEGAL_STATE_TRANSITION);
        }
        if (request.reason() == null || request.reason().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "reason is required");
        }
        ticket.setStatus("REOPENED").setReopenReason(request.reason()).setReopenedBy(context.userId()).setReopenedAt(LocalDateTime.now());
        ticket.setVersion(ticket.getVersion() + 1);
        int updated = ticketMapper.updateById(ticket);
        if (updated < 1) {
            throw new BusinessException(ErrorCode.RESOURCE_CONFLICT, "version conflict");
        }
        recordStatus(ticket, ticket.getStatus(), "REOPENED", context.userId(), "USER", "REOPEN", request.reason());
        return new TicketDtos.ReopenTicketResponse(ticketId, ticket.getStatus(), ticket.getReopenedAt(), ticket.getReopenedBy());
    }

    /**
     * 用户评价工单。
     */
    @Transactional
    public RatingDtos.RateTicketResponse rate(RequestContext context, String ticketId, RatingDtos.RateTicketRequest request) {
        TicketEntity ticket = requireTicket(context.tenantId(), ticketId);
        if (!ticket.getRequesterId().equals(context.userId())) {
            throw new BusinessException(ErrorCode.DATA_SCOPE_FORBIDDEN);
        }
        if (!Set.of("RESOLVED", "CLOSED").contains(ticket.getStatus())) {
            throw new BusinessException(ErrorCode.ILLEGAL_STATE_TRANSITION);
        }
        if (request.score() == null || request.score() < 1 || request.score() > 5) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "score must be 1-5");
        }
        Long count = ratingMapper.selectCount(new LambdaQueryWrapper<RatingEntity>()
                .eq(RatingEntity::getTenantId, context.tenantId()).eq(RatingEntity::getTicketId, ticketId));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.RESOURCE_CONFLICT, "rating already exists");
        }
        RatingEntity rating = new RatingEntity()
                .setRatingId("rate_" + UUID.randomUUID().toString().replace("-", ""))
                .setTenantId(context.tenantId()).setTicketId(ticketId).setRequesterId(context.userId())
                .setScore(request.score()).setTagsJson(request.tags() == null ? null : jsonSupport.write(request.tags()))
                .setComment(request.comment());
        ratingMapper.insert(rating);
        return new RatingDtos.RateTicketResponse(ticketId, rating.getRatingId(), rating.getScore(), rating.getCreatedAt());
    }

    private TicketEntity requireTicket(String tenantId, String ticketId) {
        TicketEntity ticket = ticketMapper.selectOne(new LambdaQueryWrapper<TicketEntity>()
                .eq(TicketEntity::getTenantId, tenantId).eq(TicketEntity::getTicketId, ticketId));
        if (ticket == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "ticket not found");
        }
        return ticket;
    }

    private void recordStatus(TicketEntity ticket, String from, String to, String operator, String operatorType, String action, String note) {
        TicketStatusHistoryEntity history = new TicketStatusHistoryEntity()
                .setHistoryId("his_" + UUID.randomUUID().toString().replace("-", ""))
                .setTenantId(ticket.getTenantId()).setTicketId(ticket.getTicketId())
                .setFromStatus(from).setToStatus(to).setOperatorId(operator).setOperatorType(operatorType)
                .setActionType(action).setActionNote(note).setOccurredAt(LocalDateTime.now());
        statusHistoryMapper.insert(history);
    }
}