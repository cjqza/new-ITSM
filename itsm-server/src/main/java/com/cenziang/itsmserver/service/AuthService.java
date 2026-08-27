package com.cenziang.itsmserver.service;
import com.cenziang.itsmserver.config.properties.ItsmAuthProperties;
import com.cenziang.itsmserver.domain.AuthCredential;
import com.cenziang.itsmserver.domain.AuthRefreshToken;
import com.cenziang.itsmserver.domain.AuthRole;
import com.cenziang.itsmserver.domain.AuthTenant;
import com.cenziang.itsmserver.domain.AuthUser;
import com.cenziang.itsmcommon.api.BusinessException;
import com.cenziang.itsmcommon.api.ErrorCode;
import com.cenziang.itsmserver.dto.AuthLoginRequest;
import com.cenziang.itsmserver.dto.AuthLoginResponse;
import com.cenziang.itsmserver.dto.AuthMeResponse;
import com.cenziang.itsmserver.dto.AuthRefreshRequest;
import com.cenziang.itsmserver.repository.AuthCredentialRepository;
import com.cenziang.itsmserver.repository.AuthLoginAuditRepository;
import com.cenziang.itsmserver.repository.AuthRefreshTokenRepository;
import com.cenziang.itsmserver.repository.AuthTenantRepository;
import com.cenziang.itsmserver.repository.AuthUserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {
    private final ItsmAuthProperties properties;
    private final AuthTenantRepository tenantRepository;
    private final AuthUserRepository userRepository;
    private final AuthCredentialRepository credentialRepository;
    private final AuthRefreshTokenRepository refreshTokenRepository;
    private final AuthLoginAuditRepository loginAuditRepository;
    private final AuthTokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(ItsmAuthProperties properties,
                       AuthTenantRepository tenantRepository,
                       AuthUserRepository userRepository,
                       AuthCredentialRepository credentialRepository,
                       AuthRefreshTokenRepository refreshTokenRepository,
                       AuthLoginAuditRepository loginAuditRepository,
                       AuthTokenService tokenService) {
        this.properties = properties;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.loginAuditRepository = loginAuditRepository;
        this.tokenService = tokenService;
        this.passwordEncoder = new BCryptPasswordEncoder(properties.getBcryptStrength());
    }

    @Transactional
    public AuthLoginResponse login(String tenantId, AuthLoginRequest request, String clientIp, String userAgent, String traceId) {
        AuthTenant tenant = tenantRepository.findEnabledTenantById(tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TENANT_FORBIDDEN, "tenant is disabled or not found"));

        String loginName = resolveLoginName(request);
        AuthCredential credential = resolveCredential(tenant.tenantId(), loginName, request.grantType())
                .orElseThrow(() -> {
                    loginAuditRepository.record(tenantId, null, loginName, "FAIL", "credential_not_found", clientIp, userAgent, traceId);
                    return new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIAL, "invalid account or password");
                });

        if (!"ACTIVE".equalsIgnoreCase(credential.status())) {
            loginAuditRepository.record(tenantId, credential.userId(), loginName, "FAIL", "credential_inactive", clientIp, userAgent, traceId);
            throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIAL, "invalid username or password");
        }
        if (credential.isLocked(LocalDateTime.now())) {
            loginAuditRepository.record(tenantId, credential.userId(), loginName, "FAIL", "credential_locked", clientIp, userAgent, traceId);
            throw new BusinessException(ErrorCode.AUTH_CREDENTIAL_LOCKED, "credential locked");
        }

        if (!"PASSWORD".equalsIgnoreCase(request.grantType())) {
            if ("SSO_CODE".equalsIgnoreCase(request.grantType())) {
                validateSsoCode(request.account(), request.ssoCode());
            } else {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "unsupported grant type");
            }
        } else {
            validatePassword(request.password(), credential, loginName, tenantId, clientIp, userAgent, traceId);
        }

        AuthUser user = userRepository.findUserById(tenant.tenantId(), credential.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIAL, "invalid username or password"));
        if (!user.enabled()) {
            loginAuditRepository.record(tenantId, user.userId(), request.account(), "FAIL", "user_disabled", clientIp, userAgent, traceId);
            throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIAL, "invalid username or password");
        }

        List<String> roles = userRepository.findRolesByUserId(tenant.tenantId(), user.userId())
                .stream()
                .map(AuthRole::roleCode)
                .toList();
        if (roles.isEmpty()) {
            roles = List.of("USER");
        }

        String permissionsVersion = tenantRepository.findPermissionsVersion(tenant.tenantId());
        String accessToken = tokenService.createAccessToken(user.userId(), tenant.tenantId(), roles, permissionsVersion, credential.authVersion());
        String refreshToken = tokenService.createRefreshToken();
        String refreshHash = tokenService.hashRefreshToken(refreshToken);

        refreshTokenRepository.save(new AuthRefreshToken(
                UUID.randomUUID().toString(),
                tenant.tenantId(),
                user.userId(),
                refreshHash,
                LocalDateTime.now().plusSeconds(properties.getRefreshTokenTtlSeconds()),
                null,
                null,
                credential.authVersion()
        ));
        credentialRepository.recordSuccessfulLogin(credential.credentialId(), clientIp);
        loginAuditRepository.record(tenant.tenantId(), user.userId(), loginName, "SUCCESS", null, clientIp, userAgent, traceId);

        return new AuthLoginResponse(
                accessToken,
                refreshToken,
                "Bearer",
                properties.getAccessTokenTtlSeconds(),
                new AuthLoginResponse.UserSummary(user.userId(), user.displayName(), user.departmentName()),
                new AuthLoginResponse.TenantSummary(tenant.tenantId(), tenant.tenantName()),
                roles,
                permissionsVersion
        );
    }

    @Transactional
    public AuthLoginResponse refresh(String tenantId, AuthRefreshRequest request, String clientIp, String userAgent, String traceId) {
        String tokenHash = tokenService.hashRefreshToken(request.refreshToken());
        AuthRefreshToken existing = refreshTokenRepository.findActiveByTokenHash(tokenHash)
                .orElseThrow(() -> new BusinessException(ErrorCode.TOKEN_INVALID, "refresh token invalid"));
        if (!Objects.equals(existing.tenantId(), tenantId)) {
            throw new BusinessException(ErrorCode.TENANT_FORBIDDEN, "tenant mismatch");
        }

        AuthTenant tenant = tenantRepository.findEnabledTenantById(tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TENANT_FORBIDDEN, "tenant is disabled or not found"));
        AuthCredential credential = credentialRepository.findByTenantIdAndUserId(tenant.tenantId(), existing.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TOKEN_INVALID, "refresh token invalid"));
        if (credential.authVersion() != existing.authVersion()) {
            refreshTokenRepository.revokeByHash(tokenHash, null);
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "refresh token expired");
        }
        AuthUser user = userRepository.findUserById(tenant.tenantId(), existing.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TOKEN_INVALID, "refresh token invalid"));
        List<String> roles = userRepository.findRolesByUserId(tenant.tenantId(), user.userId())
                .stream()
                .map(AuthRole::roleCode)
                .toList();
        if (roles.isEmpty()) {
            roles = List.of("USER");
        }
        String permissionsVersion = tenantRepository.findPermissionsVersion(tenant.tenantId());
        long authVersion = existing.authVersion();
        String accessToken = tokenService.createAccessToken(user.userId(), tenant.tenantId(), roles, permissionsVersion, authVersion);
        String newRefreshToken = tokenService.createRefreshToken();
        String newRefreshHash = tokenService.hashRefreshToken(newRefreshToken);

        refreshTokenRepository.revokeByHash(existing.tokenHash(), newRefreshHash);
        refreshTokenRepository.save(new AuthRefreshToken(
                UUID.randomUUID().toString(),
                tenant.tenantId(),
                user.userId(),
                newRefreshHash,
                LocalDateTime.now().plusSeconds(properties.getRefreshTokenTtlSeconds()),
                null,
                null,
                authVersion
        ));
        loginAuditRepository.record(tenant.tenantId(), user.userId(), credential.loginName(), "REFRESH", null, clientIp, userAgent, traceId);
        return new AuthLoginResponse(
                accessToken,
                newRefreshToken,
                "Bearer",
                properties.getAccessTokenTtlSeconds(),
                new AuthLoginResponse.UserSummary(user.userId(), user.displayName(), user.departmentName()),
                new AuthLoginResponse.TenantSummary(tenant.tenantId(), tenant.tenantName()),
                roles,
                permissionsVersion
        );
    }

    @Transactional
    public void logout(String tenantId, String refreshToken, String clientIp, String userAgent, String traceId) {
        String tokenHash = tokenService.hashRefreshToken(refreshToken);
        refreshTokenRepository.findActiveByTokenHash(tokenHash).ifPresent(token -> {
            if (!Objects.equals(token.tenantId(), tenantId)) {
                throw new BusinessException(ErrorCode.TENANT_FORBIDDEN, "tenant mismatch");
            }
            refreshTokenRepository.revokeByHash(tokenHash, null);
            loginAuditRepository.record(token.tenantId(), token.userId(), null, "LOGOUT", null, clientIp, userAgent, traceId);
        });
    }

    public AuthMeResponse me(String tenantId, String userId, long authVersion) {
        AuthTenant tenant = tenantRepository.findEnabledTenantById(tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TENANT_FORBIDDEN, "tenant is disabled or not found"));
        AuthCredential credential = credentialRepository.findByTenantIdAndUserId(tenant.tenantId(), userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "user not found"));
        if (credential.authVersion() != authVersion) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "token expired");
        }
        AuthUser user = userRepository.findUserById(tenant.tenantId(), userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "user not found"));
        List<String> roles = userRepository.findRolesByUserId(tenant.tenantId(), user.userId())
                .stream()
                .map(AuthRole::roleCode)
                .toList();
        if (roles.isEmpty()) {
            roles = List.of("USER");
        }
        return new AuthMeResponse(
                user.userId(),
                user.displayName(),
                user.departmentName(),
                tenant.tenantId(),
                roles,
                tenantRepository.findPermissionsVersion(tenant.tenantId())
        );
    }

    private void validatePassword(String rawPassword,
                                  AuthCredential credential,
                                  String loginName,
                                  String tenantId,
                                  String clientIp,
                                  String userAgent,
                                  String traceId) {
        boolean matches = passwordEncoder.matches(rawPassword == null ? "" : rawPassword, credential.passwordHash());
        if (!matches) {
            int failedCount = credential.failedCount() + 1;
            LocalDateTime lockedUntil = failedCount >= properties.getLoginFailureLockThreshold()
                    ? LocalDateTime.now().plusMinutes(properties.getLoginLockMinutes())
                    : null;
            credentialRepository.recordFailedLogin(credential.credentialId(), failedCount, lockedUntil);
            loginAuditRepository.record(tenantId, credential.userId(), loginName, "FAIL", "bad_password", clientIp, userAgent, traceId);
            throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIAL, "invalid username or password");
        }
    }

    private void validateSsoCode(String account, String ssoCode) {
        if (ssoCode == null || ssoCode.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "ssoCode is required");
        }
        if (!Objects.equals(ssoCode, properties.getSeed().getSsoCode())) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "invalid sso code");
        }
    }

    private Optional<AuthCredential> resolveCredential(String tenantId, String account, String grantType) {
        if ("SSO_CODE".equalsIgnoreCase(grantType)) {
            return credentialRepository.findByTenantIdAndLoginName(tenantId, account);
        }
        if (account != null && account.contains("@")) {
            return userRepository.findUserByTenantAndEmail(tenantId, account)
                    .flatMap(user -> credentialRepository.findByTenantIdAndUserId(tenantId, user.userId()));
        }
        return credentialRepository.findByTenantIdAndUserId(tenantId, account);
    }

    private String resolveLoginName(AuthLoginRequest request) {
        if ("SSO_CODE".equalsIgnoreCase(request.grantType())) {
            return (request.account() == null || request.account().isBlank())
                    ? properties.getSeed().getLoginName()
                    : request.account();
        }
        if (request.account() == null || request.account().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "account is required");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "password is required");
        }
        return request.account();
    }
}
