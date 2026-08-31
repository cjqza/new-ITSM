package com.cenziang.itsmserver.api;

import com.cenziang.itsmcommon.api.BusinessException;
import com.cenziang.itsmcommon.api.ErrorCode;
import com.cenziang.itsmcommon.api.ApiResponse;
import com.cenziang.itsmserver.dto.AuthLoginRequest;
import com.cenziang.itsmserver.dto.AuthLoginResponse;
import com.cenziang.itsmserver.dto.AuthLogoutRequest;
import com.cenziang.itsmserver.dto.AuthMeResponse;
import com.cenziang.itsmserver.dto.AuthRefreshRequest;
import com.cenziang.itsmserver.service.AuthService;
import com.cenziang.itsmserver.service.AuthTokenService;
import io.jsonwebtoken.JwtException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口。
 */
@Tag(name = "认证", description = "登录、当前用户、刷新令牌与退出")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    private final AuthTokenService tokenService;

    public AuthController(AuthService authService, AuthTokenService tokenService) {
        this.authService = authService;
        this.tokenService = tokenService;
    }

    @Operation(summary = "企业身份登录", description = "使用账号密码或 SSO 授权码交换访问令牌")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthLoginResponse>> login(@Parameter(description = "租户 ID", required = true) @RequestHeader("X-Tenant-Id") String tenantId,
                                                                 @Valid @RequestBody AuthLoginRequest request,
                                                                 HttpServletRequest httpServletRequest) {
        AuthLoginResponse response = authService.login(
                tenantId,
                request,
                resolveClientIp(httpServletRequest),
                httpServletRequest.getHeader("User-Agent"),
                httpServletRequest.getHeader("X-Trace-Id")
        );
        return ResponseEntity.ok(ApiResponse.success(response, httpServletRequest.getHeader("X-Trace-Id")));
    }

    @Operation(summary = "查询当前登录用户", description = "通过令牌解析当前用户身份与角色")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthMeResponse>> me(@Parameter(description = "租户 ID", required = true) @RequestHeader("X-Tenant-Id") String tenantId,
                                                          @Parameter(description = "Bearer 访问令牌", required = true) @RequestHeader("Authorization") String authorization,
                                                          HttpServletRequest httpServletRequest) {
        AuthTokenService.TokenClaims claims = parseAccessToken(authorization);
        if (!"access".equalsIgnoreCase(claims.tokenType())) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "token type invalid");
        }
        if (!tenantId.equals(claims.tenantId())) {
            throw new BusinessException(ErrorCode.TENANT_FORBIDDEN, "tenant mismatch");
        }
        AuthMeResponse response = authService.me(tenantId, claims.userId(), claims.authVersion());
        return ResponseEntity.ok(ApiResponse.success(response, httpServletRequest.getHeader("X-Trace-Id")));
    }

    @Operation(summary = "刷新令牌", description = "使用刷新令牌换取新的访问令牌并轮换刷新令牌")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthLoginResponse>> refresh(@Parameter(description = "租户 ID", required = true) @RequestHeader("X-Tenant-Id") String tenantId,
                                                                  @Valid @RequestBody AuthRefreshRequest request,
                                                                  HttpServletRequest httpServletRequest) {
        AuthLoginResponse response = authService.refresh(
                tenantId,
                request,
                resolveClientIp(httpServletRequest),
                httpServletRequest.getHeader("User-Agent"),
                httpServletRequest.getHeader("X-Trace-Id")
        );
        return ResponseEntity.ok(ApiResponse.success(response, httpServletRequest.getHeader("X-Trace-Id")));
    }

    @Operation(summary = "退出登录", description = "撤销当前刷新令牌")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Parameter(description = "租户 ID", required = true) @RequestHeader("X-Tenant-Id") String tenantId,
                                                    @Valid @RequestBody AuthLogoutRequest request,
                                                    HttpServletRequest httpServletRequest) {
        authService.logout(
                tenantId,
                request.refreshToken(),
                resolveClientIp(httpServletRequest),
                httpServletRequest.getHeader("User-Agent"),
                httpServletRequest.getHeader("X-Trace-Id")
        );
        return ResponseEntity.ok(ApiResponse.success(null, httpServletRequest.getHeader("X-Trace-Id")));
    }

    private AuthTokenService.TokenClaims parseAccessToken(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            throw new BusinessException(ErrorCode.AUTH_REQUIRED, "authorization header required");
        }
        if (!authorization.startsWith("Bearer ")) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "invalid authorization header");
        }
        try {
            return tokenService.parseAndValidate(authorization.substring(7).trim());
        } catch (JwtException exception) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "token invalid");
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}