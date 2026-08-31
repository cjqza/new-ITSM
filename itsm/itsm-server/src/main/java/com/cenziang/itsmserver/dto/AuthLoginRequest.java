package com.cenziang.itsmserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "登录请求")
public record AuthLoginRequest(
        @Schema(description = "授权类型：PASSWORD 或 SSO_CODE", example = "PASSWORD")
        @NotBlank
        @Pattern(regexp = "PASSWORD|SSO_CODE", message = "grantType must be PASSWORD or SSO_CODE")
        String grantType,

        @Schema(description = "登录账号")
        @Size(max = 100)
        String account,

        @Schema(description = "密码")
        @Size(max = 255)
        String password,

        @Schema(description = "SSO 一次性授权码")
        @Size(max = 255)
        String ssoCode,

        @Schema(description = "SSO 回调地址")
        @Size(max = 255)
        String redirectUri
) {
}