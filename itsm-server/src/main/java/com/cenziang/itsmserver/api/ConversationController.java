package com.cenziang.itsmserver.api;

import com.cenziang.itsmcommon.api.ApiResponse;
import com.cenziang.itsmcommon.api.PageResponse;
import com.cenziang.itsmpojo.dto.ConversationDtos;
import com.cenziang.itsmserver.application.RequestContext;
import com.cenziang.itsmserver.service.AgentOrchestrationService;
import com.cenziang.itsmserver.service.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 会话接口。
 */
@Tag(name = "会话", description = "用户咨询会话的创建、读取、消息发送与转人工")
@RestController
@RequestMapping("/api/v1/conversations")
public class ConversationController extends ControllerSupport {
    private final ConversationService conversationService;
    private final AgentOrchestrationService agentOrchestrationService;

    public ConversationController(ConversationService conversationService,
                                  AgentOrchestrationService agentOrchestrationService) {
        this.conversationService = conversationService;
        this.agentOrchestrationService = agentOrchestrationService;
    }

    @Operation(summary = "创建会话", description = "为用户创建一个聊天式咨询会话")
    @PostMapping("/sessions")
    public ApiResponse<ConversationDtos.SessionCreateResponse> createSession(@Parameter(description = "租户 ID", required = true) @RequestHeader(value = "X-Tenant-Id") String tenantId,
                                                                             @Valid @RequestBody ConversationDtos.CreateSessionRequest request,
                                                                             HttpServletRequest httpServletRequest) {
        RequestContext context = context(httpServletRequest, tenantId);
        return ok(conversationService.createSession(context, request), httpServletRequest);
    }

    @Operation(summary = "会话分页查询", description = "查询当前用户的咨询会话列表")
    @GetMapping("/sessions")
    public ApiResponse<PageResponse<ConversationDtos.SessionListItem>> listSessions(@Parameter(description = "租户 ID", required = true) @RequestHeader(value = "X-Tenant-Id") String tenantId,
                                                                                    @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
                                                                                    @Parameter(description = "页大小") @RequestParam(defaultValue = "20") int pageSize,
                                                                                    @Parameter(description = "关键字") @RequestParam(required = false) String keyword,
                                                                                    HttpServletRequest httpServletRequest) {
        RequestContext context = context(httpServletRequest, tenantId);
        return ok(conversationService.listSessions(context, page, pageSize, keyword), httpServletRequest);
    }

    @Operation(summary = "查询我参与的会话", description = "返回当前用户作为参与者的会话（客服端消息面板用）")
    @GetMapping("/sessions/mine")
    public ApiResponse<PageResponse<ConversationDtos.SessionListItem>> listMySessions(@Parameter(description = "租户 ID", required = true) @RequestHeader(value = "X-Tenant-Id") String tenantId,
                                                                                     @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
                                                                                     @Parameter(description = "页大小") @RequestParam(defaultValue = "20") int pageSize,
                                                                                     HttpServletRequest httpServletRequest) {
        RequestContext context = context(httpServletRequest, tenantId);
        return ok(conversationService.listMySessions(context, page, pageSize), httpServletRequest);
    }

    @Operation(summary = "读取会话", description = "返回会话基本信息、消息列表和关联工单")
    @GetMapping("/sessions/{sessionId}")
    public ApiResponse<ConversationDtos.SessionDetailResponse> getSession(@Parameter(description = "租户 ID", required = true) @RequestHeader(value = "X-Tenant-Id") String tenantId,
                                                                          @Parameter(description = "会话 ID", required = true) @PathVariable("sessionId") String sessionId,
                                                                          @Parameter(description = "消息页码") @RequestParam(defaultValue = "1") int messagePage,
                                                                          @Parameter(description = "消息页大小") @RequestParam(defaultValue = "50") int messagePageSize,
                                                                          HttpServletRequest httpServletRequest) {
        RequestContext context = context(httpServletRequest, tenantId);
        return ok(conversationService.getSession(context, sessionId, messagePage, messagePageSize), httpServletRequest);
    }

    @Operation(summary = "发送用户消息", description = "保存用户消息并返回 Agent 首轮接待结果")
    @PostMapping("/sessions/{sessionId}/messages")
    public ApiResponse<ConversationDtos.SendMessageResponse> sendMessage(@Parameter(description = "租户 ID", required = true) @RequestHeader(value = "X-Tenant-Id") String tenantId,
                                                                         @Parameter(description = "会话 ID", required = true) @PathVariable("sessionId") String sessionId,
                                                                         @Valid @RequestBody ConversationDtos.SendMessageRequest request,
                                                                         HttpServletRequest httpServletRequest) {
        RequestContext context = context(httpServletRequest, tenantId);
        return ok(conversationService.sendMessage(context, sessionId, request), httpServletRequest);
    }

    @Operation(summary = "用户请求转人工", description = "用户明确请求人工，保留上下文并自动生成服务请求")
    @PostMapping("/sessions/{sessionId}/handoff")
    public ApiResponse<ConversationDtos.AgentDecisionResponse> handoff(@Parameter(description = "租户 ID", required = true) @RequestHeader(value = "X-Tenant-Id") String tenantId,
                                                                       @Parameter(description = "会话 ID", required = true) @PathVariable("sessionId") String sessionId,
                                                                       @Valid @RequestBody ConversationDtos.HandoffRequest request,
                                                                       HttpServletRequest httpServletRequest) {
        RequestContext context = context(httpServletRequest, tenantId);
        return ok(agentOrchestrationService.userHandoff(context, sessionId, request), httpServletRequest);
    }

    @Operation(summary = "结束会话", description = "用户主动结束会话，归档并清理缓存")
    @PostMapping("/sessions/{sessionId}/end")
    public ApiResponse<ConversationDtos.SessionDetailResponse> endSession(@Parameter(description = "租户 ID", required = true) @RequestHeader(value = "X-Tenant-Id") String tenantId,
                                                                          @Parameter(description = "会话 ID", required = true) @PathVariable("sessionId") String sessionId,
                                                                          HttpServletRequest httpServletRequest) {
        RequestContext context = context(httpServletRequest, tenantId);
        return ok(conversationService.endSession(context, sessionId), httpServletRequest);
    }
}
