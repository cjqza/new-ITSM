package com.cenziang.itsmserver.api;

import com.cenziang.itsmcommon.api.ApiResponse;
import com.cenziang.itsmserver.dto.AuthRegisterRequest;
import com.cenziang.itsmserver.dto.AuthRegisterResponse;
import com.cenziang.itsmserver.dto.AuthSendCodeRequest;
import com.cenziang.itsmserver.dto.AuthSendCodeResponse;
import com.cenziang.itsmserver.service.AuthRegisterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 注册接口。
 */
@Tag(name = "注册", description = "发送验证码与账号注册")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthRegisterController {
    private final AuthRegisterService registerService;

    public AuthRegisterController(AuthRegisterService registerService) {
        this.registerService = registerService;
    }

    @Operation(summary = "发送注册验证码", description = "向手机号发送 6 位验证码，有效期 60 秒；开发模式会在响应中回传验证码")
    @PostMapping("/register/send-code")
    public ResponseEntity<ApiResponse<AuthSendCodeResponse>> sendCode(
            @Parameter(description = "租户 ID", required = true) @RequestHeader("X-Tenant-Id") String tenantId,
            @Valid @RequestBody AuthSendCodeRequest request,
            HttpServletRequest httpServletRequest) {
        AuthSendCodeResponse response = registerService.sendCode(tenantId, request.phone());
        return ResponseEntity.ok(ApiResponse.success(response, httpServletRequest.getHeader("X-Trace-Id")));
    }

    @Operation(summary = "注册账号", description = "校验验证码、两次密码一致性后创建用户并分配默认角色")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthRegisterResponse>> register(
            @Parameter(description = "租户 ID", required = true) @RequestHeader("X-Tenant-Id") String tenantId,
            @Valid @RequestBody AuthRegisterRequest request,
            HttpServletRequest httpServletRequest) {
        AuthRegisterResponse response = registerService.register(tenantId, request, resolveClientIp(httpServletRequest));
        return ResponseEntity.ok(ApiResponse.success(response, httpServletRequest.getHeader("X-Trace-Id")));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
