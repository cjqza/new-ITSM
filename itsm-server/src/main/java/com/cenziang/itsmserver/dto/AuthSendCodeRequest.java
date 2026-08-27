package com.cenziang.itsmserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "发送注册验证码请求")
public record AuthSendCodeRequest(
        @Schema(description = "手机号", example = "13800138000")
        @NotBlank
        @Pattern(regexp = "^\\d{11}$", message = "手机号需为11位数字")
        String phone
) {
}
