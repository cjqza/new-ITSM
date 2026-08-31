package com.cenziang.itsmpojo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工单评价相关 DTO。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class RatingDtos {
    private RatingDtos() {
    }

    @Schema(description = "评价请求")
    public record RateTicketRequest(
            @Schema(description = "分数") Integer score,
            @Schema(description = "标签") List<String> tags,
            @Schema(description = "评价内容") String comment
    ) {
    }

    @Schema(description = "评价响应")
    public record RateTicketResponse(
            @Schema(description = "工单主键") String ticketId,
            @Schema(description = "评价主键") String ratingId,
            @Schema(description = "分数") Integer score,
            @Schema(description = "提交时间") LocalDateTime submittedAt
    ) {
    }

    @Schema(description = "客服评分统计视图")
    public record AgentRatingView(
            @Schema(description = "客服用户主键") String agentUserId,
            @Schema(description = "客服展示名称") String agentDisplayName,
            @Schema(description = "部门名称") String departmentName,
            @Schema(description = "平均评分") Double avgScore,
            @Schema(description = "评价次数") Integer ratingCount,
            @Schema(description = "1-5 星各档数量") List<Integer> starCounts
    ) {
    }

    @Schema(description = "按评分筛选的已评工单视图")
    public record RatedTicketView(
            @Schema(description = "工单主键") String ticketId,
            @Schema(description = "工单号") String ticketNo,
            @Schema(description = "标题") String title,
            @Schema(description = "状态") String status,
            @Schema(description = "评分") Integer score,
            @Schema(description = "评价内容") String comment,
            @Schema(description = "负责人用户主键") String assigneeUserId,
            @Schema(description = "负责人展示名称") String assigneeDisplayName,
            @Schema(description = "请求人展示名称") String requesterDisplayName,
            @Schema(description = "评价时间") LocalDateTime ratedAt
    ) {
    }
}