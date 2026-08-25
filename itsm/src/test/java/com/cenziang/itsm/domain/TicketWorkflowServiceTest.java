package com.cenziang.itsm.domain;

import com.cenziang.itsm.agent.StubPythonAgentClient;
import com.cenziang.itsm.application.RequestContext;
import com.cenziang.itsm.application.TicketWorkflowService;
import com.cenziang.itsm.dto.AnalysisRequest;
import com.cenziang.itsm.dto.EvaluationRequest;
import com.cenziang.itsm.dto.HandoffRequest;
import com.cenziang.itsm.dto.ResolveRequest;
import com.cenziang.itsm.dto.SubmitQuestionRequest;
import com.cenziang.itsm.dto.SupportCommandRequest;
import com.cenziang.itsm.dto.TicketDetailResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TicketWorkflowServiceTest {
    private final TicketWorkflowService service = new TicketWorkflowService(new StubPythonAgentClient());
    private final RequestContext user = new RequestContext("tenant-a", "user-1", CustomerType.INTERNAL, "USER");
    private final RequestContext support = new RequestContext("tenant-a", "support-1", CustomerType.INTERNAL, "SUPPORT_AGENT");

    @Test
    void submitQuestionReturnsAgentAnswerWhenConfidenceIsHigh() {
        TicketDetailResponse response = submitNormalQuestion();

        assertEquals(TicketStatus.AGENT_ANSWERED, response.status());
        assertTrue(response.agentConfidence() >= 0.65);
        assertTrue(response.agentSessionId().startsWith("agent-"));
        assertTrue(response.auditEvents().stream().anyMatch(event -> event.contains("AGENT_ANSWERED")));
    }

    @Test
    void fullHumanSupportFlowCanResolveAndEvaluateTicket() {
        TicketDetailResponse submitted = submitNormalQuestion();
        TicketDetailResponse handoff = service.handoff(user, submitted.ticketId(), new HandoffRequest("用户仍无法恢复"));
        TicketDetailResponse accepted = service.accept(support, submitted.ticketId(), new SupportCommandRequest("support-1", "开始处理"));
        TicketDetailResponse analyzed = service.analyze(support, submitted.ticketId(), new AnalysisRequest("support-1", "判断为 VPN 配置过期"));
        TicketDetailResponse inSupport = service.support(support, submitted.ticketId(), new SupportCommandRequest("support-1", "指导用户刷新 VPN 配置"));
        TicketDetailResponse pendingConfirm = service.resolve(support, submitted.ticketId(), new ResolveRequest("support-1", "已刷新配置并恢复连接"));
        TicketDetailResponse resolved = service.confirmResolved(user, submitted.ticketId());
        TicketDetailResponse evaluated = service.evaluate(user, submitted.ticketId(), new EvaluationRequest(5, "响应及时"));

        assertEquals(TicketStatus.PENDING_HUMAN, handoff.status());
        assertEquals(TicketStatus.ACCEPTED, accepted.status());
        assertEquals(TicketStatus.TECH_ANALYSIS, analyzed.status());
        assertEquals(TicketStatus.IN_SUPPORT, inSupport.status());
        assertEquals(TicketStatus.PENDING_USER_CONFIRM, pendingConfirm.status());
        assertEquals(TicketStatus.RESOLVED, resolved.status());
        assertEquals(5, evaluated.rating());
        assertTrue(evaluated.statusHistory().size() >= 7);
    }

    @Test
    void agentLowConfidenceAutomaticallyRoutesToHumanQueue() {
        TicketDetailResponse response = service.submitQuestion(user, new SubmitQuestionRequest(
                "账号权限无法恢复，需要人工",
                "用户多次重置密码后仍然无法访问系统，请转人工",
                "ACCOUNT",
                "HIGH",
                "Windows 11",
                List.of(),
                new SubmitQuestionRequest.AgentReservation(true, "desktop-support", "account")
        ));

        assertEquals(TicketStatus.PENDING_HUMAN, response.status());
        assertTrue(response.agentSuggestedHandoff());
        assertEquals(1, service.supportQueue(support).size());
    }

    @Test
    void supportCannotResolveTicketBeforeAcceptedOrAnalyzed() {
        TicketDetailResponse submitted = submitNormalQuestion();

        BusinessException exception = assertThrows(BusinessException.class, () ->
                service.resolve(support, submitted.ticketId(), new ResolveRequest("support-1", "直接解决"))
        );

        assertEquals("ILLEGAL_TICKET_STATUS", exception.code());
    }

    @Test
    void crossTenantAccessIsForbidden() {
        TicketDetailResponse submitted = submitNormalQuestion();
        RequestContext otherTenantUser = new RequestContext("tenant-b", "user-1", CustomerType.INTERNAL, "USER");

        BusinessException exception = assertThrows(BusinessException.class, () ->
                service.getTicket(otherTenantUser, submitted.ticketId())
        );

        assertEquals("FORBIDDEN", exception.code());
    }

    private TicketDetailResponse submitNormalQuestion() {
        return service.submitQuestion(user, new SubmitQuestionRequest(
                "VPN 无法连接",
                "用户连接企业 VPN 时提示网络异常",
                "NETWORK",
                "MEDIUM",
                "Windows 11 / VPN Client 5.0",
                List.of("screenshot-001.png"),
                new SubmitQuestionRequest.AgentReservation(true, "desktop-support", "network")
        ));
    }
}
