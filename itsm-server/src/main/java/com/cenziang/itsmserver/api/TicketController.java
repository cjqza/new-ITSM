package com.cenziang.itsmserver.api;

import com.cenziang.itsmcommon.api.ApiResponse;
import com.cenziang.itsmcommon.api.PageResponse;
import com.cenziang.itsmpojo.dto.RatingDtos;
import com.cenziang.itsmpojo.dto.TicketDtos;
import com.cenziang.itsmserver.application.RequestContext;
import com.cenziang.itsmserver.service.TicketService;
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

import java.util.List;

/**
 * 用户侧工单接口。
 */
@Tag(name = "用户工单", description = "用户创建、查询、确认、重开和评价工单")
@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController extends ControllerSupport {
    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @Operation(summary = "创建工单", description = "用户手动建单或 Agent 转人工建单")
    @PostMapping
    public ApiResponse<TicketDtos.CreateTicketResponse> create(@Parameter(description = "租户 ID", required = true) @RequestHeader("X-Tenant-Id") String tenantId,
                                                               @Valid @RequestBody TicketDtos.CreateTicketRequest request,
                                                               HttpServletRequest httpServletRequest) {
        return ok(ticketService.create(context(httpServletRequest, tenantId), request), httpServletRequest);
    }

    @Operation(summary = "工单分页查询", description = "用户查询本人工单，客服/管理员查询权限范围内工单")
    @GetMapping
    public ApiResponse<PageResponse<TicketDtos.TicketPageItem>> page(@Parameter(description = "租户 ID", required = true) @RequestHeader("X-Tenant-Id") String tenantId,
                                                                     @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
                                                                     @Parameter(description = "页大小") @RequestParam(defaultValue = "20") int pageSize,
                                                                     @Parameter(description = "状态列表") @RequestParam(required = false) List<String> status,
                                                                     @Parameter(description = "工单号") @RequestParam(required = false) String ticketNo,
                                                                     @Parameter(description = "请求人") @RequestParam(required = false) String requesterId,
                                                                     @Parameter(description = "业务线编码") @RequestParam(required = false) String businessLineCode,
                                                                     @Parameter(description = "关键字") @RequestParam(required = false) String keyword,
                                                                     @Parameter(description = "排序") @RequestParam(required = false) String sort,
                                                                     HttpServletRequest httpServletRequest) {
        RequestContext context = context(httpServletRequest, tenantId);
        TicketDtos.TicketPageQuery query = new TicketDtos.TicketPageQuery(page, pageSize, status, ticketNo, requesterId, businessLineCode, keyword, sort);
        return ok(ticketService.page(context, query), httpServletRequest);
    }

    @Operation(summary = "工单详情", description = "返回工单核心字段、分类、状态历史和评价")
    @GetMapping("/{ticketId}")
    public ApiResponse<TicketDtos.TicketDetailResponse> detail(@Parameter(description = "租户 ID", required = true) @RequestHeader("X-Tenant-Id") String tenantId,
                                                               @Parameter(description = "工单 ID", required = true) @PathVariable String ticketId,
                                                               HttpServletRequest httpServletRequest) {
        return ok(ticketService.detail(context(httpServletRequest, tenantId), ticketId), httpServletRequest);
    }

    @Operation(summary = "用户确认解决", description = "用户确认客服处理结果，工单进入已解决")
    @PostMapping("/{ticketId}/confirm")
    public ApiResponse<TicketDtos.ConfirmTicketResponse> confirm(@Parameter(description = "租户 ID", required = true) @RequestHeader("X-Tenant-Id") String tenantId,
                                                                 @Parameter(description = "工单 ID", required = true) @PathVariable String ticketId,
                                                                 HttpServletRequest httpServletRequest) {
        return ok(ticketService.confirm(context(httpServletRequest, tenantId), ticketId), httpServletRequest);
    }

    @Operation(summary = "用户重开工单", description = "用户对结果不满意或问题复发时重新打开工单")
    @PostMapping("/{ticketId}/reopen")
    public ApiResponse<TicketDtos.ReopenTicketResponse> reopen(@Parameter(description = "租户 ID", required = true) @RequestHeader("X-Tenant-Id") String tenantId,
                                                               @Parameter(description = "工单 ID", required = true) @PathVariable String ticketId,
                                                               @Valid @RequestBody TicketDtos.ReopenTicketRequest request,
                                                               HttpServletRequest httpServletRequest) {
        return ok(ticketService.reopen(context(httpServletRequest, tenantId), ticketId, request), httpServletRequest);
    }

    @Operation(summary = "用户评价工单", description = "对已解决或已关闭工单提交 1-5 分评价")
    @PostMapping("/{ticketId}/rating")
    public ApiResponse<RatingDtos.RateTicketResponse> rate(@Parameter(description = "租户 ID", required = true) @RequestHeader("X-Tenant-Id") String tenantId,
                                                           @Parameter(description = "工单 ID", required = true) @PathVariable String ticketId,
                                                           @Valid @RequestBody RatingDtos.RateTicketRequest request,
                                                           HttpServletRequest httpServletRequest) {
        return ok(ticketService.rate(context(httpServletRequest, tenantId), ticketId, request), httpServletRequest);
    }

    @Operation(summary = "工单挂起/恢复", description = "客服切换工单挂起状态，暂停/恢复 SLA 计时")
    @PostMapping("/{ticketId}/suspend")
    public ApiResponse<TicketDtos.SuspendTicketResponse> toggleSuspend(@Parameter(description = "租户 ID", required = true) @RequestHeader("X-Tenant-Id") String tenantId,
                                                                       @Parameter(description = "工单 ID", required = true) @PathVariable String ticketId,
                                                                       @RequestBody(required = false) TicketDtos.SuspendTicketRequest request,
                                                                       HttpServletRequest httpServletRequest) {
        if (request == null) request = new TicketDtos.SuspendTicketRequest(null);
        return ok(ticketService.toggleSuspend(context(httpServletRequest, tenantId), ticketId, request), httpServletRequest);
    }
}