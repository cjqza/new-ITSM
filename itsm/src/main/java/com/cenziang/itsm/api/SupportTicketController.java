package com.cenziang.itsm.api;

import com.cenziang.itsm.application.RequestContext;
import com.cenziang.itsm.application.TicketWorkflowService;
import com.cenziang.itsm.domain.CustomerType;
import com.cenziang.itsm.dto.AnalysisRequest;
import com.cenziang.itsm.dto.ApiResponse;
import com.cenziang.itsm.dto.ResolveRequest;
import com.cenziang.itsm.dto.SupportCommandRequest;
import com.cenziang.itsm.dto.TicketDetailResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/support/tickets")
public class SupportTicketController {
    private final TicketWorkflowService ticketWorkflowService;

    public SupportTicketController(TicketWorkflowService ticketWorkflowService) {
        this.ticketWorkflowService = ticketWorkflowService;
    }

    @GetMapping("/queue")
    public ApiResponse<List<TicketDetailResponse>> queue(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-Operator-Id") String operatorId,
            @RequestHeader("X-Role") String role
    ) {
        return ApiResponse.success(ticketWorkflowService.supportQueue(supportContext(tenantId, operatorId, role)));
    }

    @PostMapping("/{ticketId}/accept")
    public ApiResponse<TicketDetailResponse> accept(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-Operator-Id") String operatorId,
            @RequestHeader("X-Role") String role,
            @PathVariable String ticketId,
            @RequestBody SupportCommandRequest request
    ) {
        return ApiResponse.success(ticketWorkflowService.accept(supportContext(tenantId, operatorId, role), ticketId, request));
    }

    @PostMapping("/{ticketId}/analysis")
    public ApiResponse<TicketDetailResponse> analyze(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-Operator-Id") String operatorId,
            @RequestHeader("X-Role") String role,
            @PathVariable String ticketId,
            @RequestBody AnalysisRequest request
    ) {
        return ApiResponse.success(ticketWorkflowService.analyze(supportContext(tenantId, operatorId, role), ticketId, request));
    }

    @PostMapping("/{ticketId}/support")
    public ApiResponse<TicketDetailResponse> support(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-Operator-Id") String operatorId,
            @RequestHeader("X-Role") String role,
            @PathVariable String ticketId,
            @RequestBody SupportCommandRequest request
    ) {
        return ApiResponse.success(ticketWorkflowService.support(supportContext(tenantId, operatorId, role), ticketId, request));
    }

    @PostMapping("/{ticketId}/resolve")
    public ApiResponse<TicketDetailResponse> resolve(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-Operator-Id") String operatorId,
            @RequestHeader("X-Role") String role,
            @PathVariable String ticketId,
            @RequestBody ResolveRequest request
    ) {
        return ApiResponse.success(ticketWorkflowService.resolve(supportContext(tenantId, operatorId, role), ticketId, request));
    }

    @PostMapping("/{ticketId}/close")
    public ApiResponse<TicketDetailResponse> close(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-Operator-Id") String operatorId,
            @RequestHeader("X-Role") String role,
            @PathVariable String ticketId,
            @RequestBody SupportCommandRequest request
    ) {
        return ApiResponse.success(ticketWorkflowService.close(supportContext(tenantId, operatorId, role), ticketId, request));
    }

    private RequestContext supportContext(String tenantId, String operatorId, String role) {
        return new RequestContext(tenantId, operatorId, CustomerType.INTERNAL, role);
    }
}
