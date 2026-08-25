package com.cenziang.itsm.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Ticket {
    private final String id;
    private final String tenantId;
    private final String requesterId;
    private final CustomerType requesterType;
    private final String title;
    private final String description;
    private final String category;
    private final List<String> attachments;
    private final Instant createdAt;
    private final List<StatusHistoryEntry> statusHistory = new ArrayList<>();
    private final List<AuditEvent> auditEvents = new ArrayList<>();
    private final List<TicketMessage> messages = new ArrayList<>();
    private TicketPriority priority;
    private TicketStatus status;
    private String environment;
    private String agentSessionId;
    private String agentAnswer;
    private double agentConfidence;
    private boolean agentSuggestedHandoff;
    private String agentSourceSummary;
    private String handoffSummary;
    private String assigneeId;
    private String analysis;
    private String supportNote;
    private String solution;
    private Integer rating;
    private String ratingComment;
    private Instant updatedAt;

    private Ticket(
            String id,
            String tenantId,
            String requesterId,
            CustomerType requesterType,
            String title,
            String description,
            String category,
            TicketPriority priority,
            String environment,
            List<String> attachments
    ) {
        this.id = id;
        this.tenantId = tenantId;
        this.requesterId = requesterId;
        this.requesterType = requesterType;
        this.title = title;
        this.description = description;
        this.category = category;
        this.priority = priority;
        this.environment = environment;
        this.attachments = new ArrayList<>(attachments == null ? List.of() : attachments);
        this.status = TicketStatus.SUBMITTED;
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
        this.messages.add(new TicketMessage(MessageSender.USER, requesterId, description, createdAt));
        addAudit("TICKET_CREATED", requesterId, "用户提交问题");
    }

    public static Ticket create(
            String id,
            String tenantId,
            String requesterId,
            CustomerType requesterType,
            String title,
            String description,
            String category,
            TicketPriority priority,
            String environment,
            List<String> attachments
    ) {
        return new Ticket(id, tenantId, requesterId, requesterType, title, description, category, priority, environment, attachments);
    }

    public void transitionTo(TicketStatus targetStatus, String operatorId, String reason) {
        TicketStatus previous = this.status;
        this.status = targetStatus;
        this.updatedAt = Instant.now();
        statusHistory.add(new StatusHistoryEntry(previous, targetStatus, operatorId, reason, updatedAt));
        addAudit("STATUS_CHANGED", operatorId, previous + " -> " + targetStatus + "：" + reason);
    }

    public void applyAgentAnswer(String sessionId, String answer, double confidence, boolean suggestedHandoff, String sourceSummary) {
        this.agentSessionId = sessionId;
        this.agentAnswer = answer;
        this.agentConfidence = confidence;
        this.agentSuggestedHandoff = suggestedHandoff;
        this.agentSourceSummary = sourceSummary;
        this.handoffSummary = buildHandoffSummary();
        this.updatedAt = Instant.now();
        this.messages.add(new TicketMessage(MessageSender.AGENT, "python-agent-reserved", answer, updatedAt));
        addAudit("AGENT_ANSWERED", "python-agent-reserved", "置信度：" + confidence + "，建议转人工：" + suggestedHandoff);
    }

    public void markHandoff(String operatorId, String reason) {
        this.handoffSummary = buildHandoffSummary() + "；转人工原因：" + reason;
        addAudit("HANDOFF_REQUESTED", operatorId, reason);
    }

    public void accept(String assigneeId, String note) {
        this.assigneeId = assigneeId;
        addSupportMessage(assigneeId, "受理工单：" + note);
    }

    public void analyze(String operatorId, String analysis) {
        this.analysis = analysis;
        addSupportMessage(operatorId, "技术分析：" + analysis);
    }

    public void support(String operatorId, String note) {
        this.supportNote = note;
        addSupportMessage(operatorId, "技术支持：" + note);
    }

    public void resolve(String operatorId, String solution) {
        this.solution = solution;
        addSupportMessage(operatorId, "解决方案：" + solution);
    }

    public void evaluate(int rating, String comment) {
        this.rating = rating;
        this.ratingComment = comment;
        addAudit("TICKET_EVALUATED", requesterId, "评分：" + rating + "，评价：" + comment);
    }

    public void addAudit(String action, String operatorId, String detail) {
        auditEvents.add(new AuditEvent(action, operatorId, detail, Instant.now()));
    }

    private void addSupportMessage(String operatorId, String content) {
        this.updatedAt = Instant.now();
        messages.add(new TicketMessage(MessageSender.SUPPORT, operatorId, content, updatedAt));
        addAudit("SUPPORT_MESSAGE", operatorId, content);
    }

    private String buildHandoffSummary() {
        return "问题：" + title
                + "；分类：" + category
                + "；环境：" + (environment == null || environment.isBlank() ? "未提供" : environment)
                + "；Agent回答：" + (agentAnswer == null ? "无" : agentAnswer)
                + "；知识来源：" + (agentSourceSummary == null ? "未命中" : agentSourceSummary);
    }

    public String id() {
        return id;
    }

    public String tenantId() {
        return tenantId;
    }

    public String requesterId() {
        return requesterId;
    }

    public CustomerType requesterType() {
        return requesterType;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public String category() {
        return category;
    }

    public TicketPriority priority() {
        return priority;
    }

    public TicketStatus status() {
        return status;
    }

    public String environment() {
        return environment;
    }

    public String agentSessionId() {
        return agentSessionId;
    }

    public String agentAnswer() {
        return agentAnswer;
    }

    public double agentConfidence() {
        return agentConfidence;
    }

    public boolean agentSuggestedHandoff() {
        return agentSuggestedHandoff;
    }

    public String agentSourceSummary() {
        return agentSourceSummary;
    }

    public String handoffSummary() {
        return handoffSummary;
    }

    public String assigneeId() {
        return assigneeId;
    }

    public String analysis() {
        return analysis;
    }

    public String supportNote() {
        return supportNote;
    }

    public String solution() {
        return solution;
    }

    public Integer rating() {
        return rating;
    }

    public String ratingComment() {
        return ratingComment;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public List<String> attachments() {
        return Collections.unmodifiableList(attachments);
    }

    public List<StatusHistoryEntry> statusHistory() {
        return Collections.unmodifiableList(statusHistory);
    }

    public List<AuditEvent> auditEvents() {
        return Collections.unmodifiableList(auditEvents);
    }

    public List<TicketMessage> messages() {
        return Collections.unmodifiableList(messages);
    }
}
