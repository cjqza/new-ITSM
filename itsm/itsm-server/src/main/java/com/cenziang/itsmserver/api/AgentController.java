package com.cenziang.itsmserver.api;

import com.cenziang.itsmcommon.api.ApiResponse;
import com.cenziang.itsmpojo.dto.ConversationDtos;
import com.cenziang.itsmserver.application.RequestContext;
import com.cenziang.itsmserver.service.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Agent 编排接口。
 */
@Tag(name = "Agent 编排", description = "接收 Agent 结构化决策，驱动自助回答或转人工")
@RestController
@RequestMapping("/api/v1/agent")
public class AgentController extends ControllerSupport {
    private final ConversationService conversationService;

    public AgentController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @Operation(summary = "提交 Agent 决策", description = "Agent 服务提交自助解决或转人工的结构化决策")
    @PostMapping("/sessions/{sessionId}/decisions")
    public ApiResponse<ConversationDtos.AgentDecisionResponse> decide(@Parameter(description = "租户 ID", required = true) @RequestHeader("X-Tenant-Id") String tenantId,
                                                                      @Parameter(description = "会话 ID", required = true) @PathVariable String sessionId,
                                                                      @Valid @RequestBody ConversationDtos.AgentDecisionRequest request,
                                                                      HttpServletRequest httpServletRequest) {
        RequestContext context = context(httpServletRequest, tenantId);
        return ok(conversationService.recordDecision(context, sessionId, request), httpServletRequest);
    }
}