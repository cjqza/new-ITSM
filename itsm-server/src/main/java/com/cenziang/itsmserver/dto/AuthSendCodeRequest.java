package com.cenziang.itsmserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "发送注册验证码请求")
public record AuthSendCodeRequest(
        @Schema(description = "手机号", example = "13800138000")
        @NotBlank
        @Size(max = 32)
        String phone
) {
}
