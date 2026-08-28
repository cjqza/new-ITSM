package com.cenziang.itsmserver.api;

import com.cenziang.itsmcommon.api.ApiResponse;
import com.cenziang.itsmcommon.api.PageResponse;
import com.cenziang.itsmpojo.dto.TicketDtos;
import com.cenziang.itsmserver.application.RequestContext;
import com.cenziang.itsmserver.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 客服侧工单接口。
 */
@Tag(name = "客服工单", description = "客服队列、受理、分类、解决和关闭工单")
@RestController
@RequestMapping("/api/v1/support/tickets")
public class SupportController extends ControllerSupport {
    private final TicketService ticketService;

    public SupportController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @Operation(summary = "客服队列查询", description = "按待受理、处理中、待确认、历史等视图查询工单")
    @GetMapping
    public ApiResponse<PageResponse<TicketDtos.SupportQueueItem>> queue(@Parameter(description = "租户 ID", required = true) @RequestHeader("X-Tenant-Id") String tenantId,
                                                                        @Parameter(description = "视图") @RequestParam(defaultValue = "PENDING") String view,
                                                                        @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
                                                                        @Parameter(description = "页大小") @RequestParam(defaultValue = "20") int pageSize,
                                                                        @Parameter(description = "业务线编码") @RequestParam(required = false) String businessLineCode,
                                                                        @Parameter(description = "受理范围") @RequestParam(required = false) String assignee,
                                                                        @Parameter(description = "关键字") @RequestParam(required = false) String keyword,
                                                                        HttpServletRequest httpServletRequest) {
        RequestContext context = context(httpServletRequest, tenantId);
        TicketDtos.SupportQueueQuery query = new TicketDtos.SupportQueueQuery(view, page, pageSize, businessLineCode, assignee, keyword);
        return ok(ticketService.supportQueue(context, query), httpServletRequest);
    }

    @Operation(summary = "客服受理工单", description = "客服抢占或接收工单，成为当前处理人")
    @PostMapping("/{ticketId}/accept")
    public ApiResponse<TicketDtos.TicketDetailResponse> accept(@Parameter(description = "租户 ID", required = true) @RequestHeader("X-Tenant-Id") String tenantId,
                                                               @Parameter(description = "工单 ID", required = true) @PathVariable String ticketId,
                                                               @Valid @RequestBody TicketDtos.SupportAcceptRequest request,
                                                               HttpServletRequest httpServletRequest) {
        return ok(ticketService.accept(context(httpServletRequest, tenantId), ticketId, request), httpServletRequest);
    }

    @Operation(summary = "转让工单给同事", description = "更新处理人并把同事拉进会话群")
    @PostMapping("/{ticketId}/transfer")
    public ApiResponse<TicketDtos.TicketDetailResponse> transfer(@Parameter(description = "租户 ID", required = true) @RequestHeader("X-Tenant-Id") String tenantId,
                                                                 @Parameter(description = "工单 ID", required = true) @PathVariable String ticketId,
                                                                 @Valid @RequestBody TicketDtos.TransferTicketRequest request,
                                                                 HttpServletRequest httpServletRequest) {
        return ok(ticketService.transfer(context(httpServletRequest, tenantId), ticketId, request), httpServletRequest);
    }

    @Operation(summary = "更新工单分类", description = "客服按标准字典补齐管理单元、症状、原因和解决方法")
    @PatchMapping("/{ticketId}/classification")
    public ApiResponse<TicketDtos.TicketDetailResponse> classify(@Parameter(description = "租户 ID", required = true) @RequestHeader("X-Tenant-Id") String tenantId,
                                                                 @Parameter(description = "工单 ID", required = true) @PathVariable String ticketId,
                                                                 @Valid @RequestBody TicketDtos.ClassificationUpdateRequest request,
                                                                 HttpServletRequest httpServletRequest) {
        return ok(ticketService.classify(context(httpServletRequest, tenantId), ticketId, request), httpServletRequest);
    }

    @Operation(summary = "提交解决结果", description = "客服记录处理过程与解决方案，工单交给用户确认")
    @PostMapping("/{ticketId}/resolve")
    public ApiResponse<TicketDtos.TicketDetailResponse> resolve(@Parameter(description = "租户 ID", required = true) @RequestHeader("X-Tenant-Id") String tenantId,
                                                                @Parameter(description = "工单 ID", required = true) @PathVariable String ticketId,
                                                                @Valid @RequestBody TicketDtos.ResolveTicketRequest request,
                                                                HttpServletRequest httpServletRequest) {
        return ok(ticketService.resolve(context(httpServletRequest, tenantId), ticketId, request), httpServletRequest);
    }

    @Operation(summary = "客服关闭工单", description = "在工单已解决后完成生命周期关闭")
    @PostMapping("/{ticketId}/close")
    public ApiResponse<TicketDtos.CloseTicketResponse> close(@Parameter(description = "租户 ID", required = true) @RequestHeader("X-Tenant-Id") String tenantId,
                                                             @Parameter(description = "工单 ID", required = true) @PathVariable String ticketId,
                                                             @Valid @RequestBody TicketDtos.CloseTicketRequest request,
                                                             HttpServletRequest httpServletRequest) {
        return ok(ticketService.close(context(httpServletRequest, tenantId), ticketId, request), httpServletRequest);
    }
}