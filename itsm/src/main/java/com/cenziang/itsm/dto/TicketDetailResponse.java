package com.cenziang.itsm.dto;

import com.cenziang.itsm.domain.AuditEvent;
import com.cenziang.itsm.domain.CustomerType;
import com.cenziang.itsm.domain.StatusHistoryEntry;
import com.cenziang.itsm.domain.Ticket;
import com.cenziang.itsm.domain.TicketPriority;
import com.cenziang.itsm.domain.TicketStatus;

import java.time.Instant;
import java.util.List;

public record TicketDetailResponse(
        String ticketId,
        String tenantId,
        String requesterId,
        CustomerType requesterType,
        String title,
        String description,
        String category,
        TicketPriority priority,
        TicketStatus status,
        String environment,
        String agentSessionId,
        String agentAnswer,
        double agentConfidence,
        boolean agentSuggestedHandoff,
        String agentSourceSummary,
        String handoffSummary,
        String assigneeId,
        String analysis,
        String supportNote,
        String solution,
        Integer rating,
        String ratingComment,
        List<String> attachments,
        List<String> statusHistory,
        List<String> auditEvents,
        Instant createdAt,
        Instant updatedAt
) {
    public static TicketDetailResponse from(Ticket ticket) {
        return new TicketDetailResponse(
                ticket.id(),
                ticket.tenantId(),
                ticket.requesterId(),
                ticket.requesterType(),
                ticket.title(),
                ticket.description(),
                ticket.category(),
                ticket.priority(),
                ticket.status(),
                ticket.environment(),
                ticket.agentSessionId(),
                ticket.agentAnswer(),
                ticket.agentConfidence(),
                ticket.agentSuggestedHandoff(),
                ticket.agentSourceSummary(),
                ticket.handoffSummary(),
                ticket.assigneeId(),
                ticket.analysis(),
                ticket.supportNote(),
                ticket.solution(),
                ticket.rating(),
                ticket.ratingComment(),
                ticket.attachments(),
                ticket.statusHistory().stream().map(TicketDetailResponse::formatHistory).toList(),
                ticket.auditEvents().stream().map(TicketDetailResponse::formatAudit).toList(),
                ticket.createdAt(),
                ticket.updatedAt()
        );
    }

    private static String formatHistory(StatusHistoryEntry entry) {
        return entry.changedAt() + " " + entry.fromStatus() + " -> " + entry.toStatus()
                + " by " + entry.operatorId() + "：" + entry.reason();
    }

    private static String formatAudit(AuditEvent event) {
        return event.occurredAt() + " " + event.action()
                + " by " + event.operatorId() + "：" + event.detail();
    }
}
