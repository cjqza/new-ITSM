package com.cenziang.itsmserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "刷新令牌请求")
public record AuthRefreshRequest(
        @Schema(description = "刷新令牌", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String refreshToken
) {
}