package com.cenziang.itsmpojo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 同事消息相关 DTO。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ColleagueMessageDtos {
    private ColleagueMessageDtos() {
    }

    @Schema(description = "发送同事消息请求")
    public record SendRequest(
            @Schema(description = "接收方用户主键") String toUserId,
            @Schema(description = "消息内容") String content
    ) {
    }

    @Schema(description = "同事消息视图")
    public record MessageView(
            @Schema(description = "消息主键") Long id,
            @Schema(description = "发送方用户主键") String fromUserId,
            @Schema(description = "接收方用户主键") String toUserId,
            @Schema(description = "消息内容") String content,
            @Schema(description = "发送时间") LocalDateTime createdAt
    ) {
    }
}
