package com.cenziang.itsmserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "发送注册验证码响应")
public record AuthSendCodeResponse(
        @Schema(description = "手机号") String phone,
        @Schema(description = "验证码（仅开发模式返回，生产环境为 null）") String code,
        @Schema(description = "验证码有效期（秒）") long expiresIn
) {
}
