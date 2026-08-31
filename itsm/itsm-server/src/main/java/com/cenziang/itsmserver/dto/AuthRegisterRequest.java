package com.cenziang.itsmserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "注册请求")
public record AuthRegisterRequest(
        @Schema(description = "姓名", example = "张三")
        @NotBlank
        @Size(min = 2, max = 64)
        String username,

        @Schema(description = "手机号", example = "13800138000")
        @NotBlank
        @Pattern(regexp = "^\\d{11}$", message = "手机号需为11位数字")
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
