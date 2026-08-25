package com.cenziang.itsm.api;

import com.cenziang.itsm.application.RequestContext;
import com.cenziang.itsm.application.TicketWorkflowService;
import com.cenziang.itsm.domain.CustomerType;
import com.cenziang.itsm.dto.ApiResponse;
import com.cenziang.itsm.dto.EvaluationRequest;
import com.cenziang.itsm.dto.HandoffRequest;
import com.cenziang.itsm.dto.SubmitQuestionRequest;
import com.cenziang.itsm.dto.TicketDetailResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user/tickets")
public class UserTicketController {
    private final TicketWorkflowService ticketWorkflowService;

    public UserTicketController(TicketWorkflowService ticketWorkflowService) {
        this.ticketWorkflowService = ticketWorkflowService;
    }

    @PostMapping("/questions")
    public ApiResponse<TicketDetailResponse> submitQuestion(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-User-Type", defaultValue = "INTERNAL") String userType,
            @RequestBody SubmitQuestionRequest request
    ) {
        return ApiResponse.success(ticketWorkflowService.submitQuestion(userContext(tenantId, userId, userType), request));
    }

    @GetMapping("/{ticketId}")
    public ApiResponse<TicketDetailResponse> getTicket(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-User-Type", defaultValue = "INTERNAL") String userType,
            @PathVariable String ticketId
    ) {
        return ApiResponse.success(ticketWorkflowService.getTicket(userContext(tenantId, userId, userType), ticketId));
    }

    @PostMapping("/{ticketId}/handoff")
    public ApiResponse<TicketDetailResponse> handoff(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-User-Type", defaultValue = "INTERNAL") String userType,
            @PathVariable String ticketId,
            @RequestBody HandoffRequest request
    ) {
        return ApiResponse.success(ticketWorkflowService.handoff(userContext(tenantId, userId, userType), ticketId, request));
    }

    @PostMapping("/{ticketId}/confirm")
    public ApiResponse<TicketDetailResponse> confirmResolved(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-User-Type", defaultValue = "INTERNAL") String userType,
            @PathVariable String ticketId
    ) {
        return ApiResponse.success(ticketWorkflowService.confirmResolved(userContext(tenantId, userId, userType), ticketId));
    }

    @PostMapping("/{ticketId}/evaluations")
    public ApiResponse<TicketDetailResponse> evaluate(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-User-Type", defaultValue = "INTERNAL") String userType,
            @PathVariable String ticketId,
            @RequestBody EvaluationRequest request
    ) {
        return ApiResponse.success(ticketWorkflowService.evaluate(userContext(tenantId, userId, userType), ticketId, request));
    }

    @PostMapping("/{ticketId}/reopen")
    public ApiResponse<TicketDetailResponse> reopen(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-User-Type", defaultValue = "INTERNAL") String userType,
            @PathVariable String ticketId,
            @RequestBody HandoffRequest request
    ) {
        return ApiResponse.success(ticketWorkflowService.reopen(userContext(tenantId, userId, userType), ticketId, request));
    }

    private RequestContext userContext(String tenantId, String userId, String userType) {
        return new RequestContext(tenantId, userId, CustomerType.from(userType), "USER");
    }
}
