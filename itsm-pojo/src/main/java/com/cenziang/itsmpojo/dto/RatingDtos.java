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
}