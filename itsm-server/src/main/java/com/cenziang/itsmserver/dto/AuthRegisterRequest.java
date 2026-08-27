package com.cenziang.itsmserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "注册请求")
public record AuthRegisterRequest(
        @Schema(description = "用户名（登录账号）", example = "lisi")
        @NotBlank
        @Size(min = 3, max = 64)
        String username,

        @Schema(description = "手机号", example = "13800138000")
        @NotBlank
        @Size(max = 32)
        String phone,

        @Schema(description = "密码")
        @NotBlank
        @Size(min = 8, max = 64)
        String password,

        @Schema(description = "确认密码")
        @NotBlank
        @Size(min = 8, max = 64)
        String confirmPassword,

        @Schema(description = "验证码")
        @NotBlank
        @Size(max = 16)
        String code
) {
}
