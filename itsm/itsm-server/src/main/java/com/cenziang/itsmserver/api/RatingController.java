package com.cenziang.itsmserver.api;

import com.cenziang.itsmcommon.api.ApiResponse;
import com.cenziang.itsmcommon.api.PageResponse;
import com.cenziang.itsmpojo.dto.RatingDtos;
import com.cenziang.itsmserver.service.RatingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 工单评价统计接口（数鲸看板）。
 */
@Tag(name = "工单评价统计", description = "客服评分汇总与按评分筛选已评工单")
@RestController
@RequestMapping("/api/v1/ratings")
public class RatingController extends ControllerSupport {
    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @Operation(summary = "客服评分汇总", description = "返回各客服的平均评分、评价次数与 1-5 星分布")
    @GetMapping("/agents")
    public ApiResponse<List<RatingDtos.AgentRatingView>> agentSummaries(@Parameter(description = "租户 ID", required = true) @RequestHeader("X-Tenant-Id") String tenantId,
                                                                       HttpServletRequest httpServletRequest) {
        return ok(ratingService.agentSummaries(context(httpServletRequest, tenantId)), httpServletRequest);
    }

    @Operation(summary = "按评分筛选已评工单", description = "按 1-5 星评分筛选已评工单，返回负责人与评价内容")
    @GetMapping("/tickets")
    public ApiResponse<PageResponse<RatingDtos.RatedTicketView>> listRatedTickets(@Parameter(description = "租户 ID", required = true) @RequestHeader("X-Tenant-Id") String tenantId,
                                                                                  @Parameter(description = "评分（1-5，缺省返回全部）") @RequestParam(required = false) Integer score,
                                                                                  @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
                                                                                  @Parameter(description = "页大小") @RequestParam(defaultValue = "20") int pageSize,
                                                                                  HttpServletRequest httpServletRequest) {
        return ok(ratingService.listRatedTickets(context(httpServletRequest, tenantId), score, page, pageSize), httpServletRequest);
    }
}
