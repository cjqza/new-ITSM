package com.cenziang.itsmserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "退出登录请求")
public record AuthLogoutRequest(
        @Schema(description = "刷新令牌", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String refreshToken
) {
}