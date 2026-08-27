package com.cenziang.itsmserver.service;

import com.cenziang.itsmcommon.api.BusinessException;
import com.cenziang.itsmcommon.api.ErrorCode;
import com.cenziang.itsmserver.config.properties.ItsmAuthProperties;
import com.cenziang.itsmserver.config.properties.ItsmRegisterProperties;
import com.cenziang.itsmserver.domain.AuthTenant;
import com.cenziang.itsmserver.dto.AuthRegisterRequest;
import com.cenziang.itsmserver.dto.AuthRegisterResponse;
import com.cenziang.itsmserver.dto.AuthSendCodeResponse;
import com.cenziang.itsmserver.repository.AuthCredentialRepository;
import com.cenziang.itsmserver.repository.AuthTenantRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

/**
 * 注册与验证码服务。
 * <p>
 * 一期落地账号自助注册：手机号验证码（Redis 短期存储，默认 60 秒）、
 * 两次密码一致性校验，以及按 IP 的注册请求限流（默认每分钟 5 次）。
 * </p>
 */
@Service
public class AuthRegisterService {
    private static final String CODE_KEY_PREFIX = "itsm:register:code:";
    private static final String RATE_KEY_PREFIX = "itsm:register:rate:";

    private final StringRedisTemplate redisTemplate;
    private final ItsmRegisterProperties properties;
    private final PasswordEncoder passwordEncoder;
    private final AuthTenantRepository tenantRepository;
    private final AuthCredentialRepository credentialRepository;
    private final JdbcTemplate jdbcTemplate;
    private final UserAccountGenerator accountGenerator;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthRegisterService(StringRedisTemplate redisTemplate,
                               ItsmRegisterProperties properties,
                               ItsmAuthProperties authProperties,
                               AuthTenantRepository tenantRepository,
                               AuthCredentialRepository credentialRepository,
                               JdbcTemplate jdbcTemplate,
                               UserAccountGenerator accountGenerator) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.passwordEncoder = new BCryptPasswordEncoder(authProperties.getBcryptStrength());
        this.tenantRepository = tenantRepository;
        this.credentialRepository = credentialRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.accountGenerator = accountGenerator;
    }

    /**
     * 向指定手机号发送注册验证码。
     * <p>
     * 开发模式默认在响应中回传验证码，生产环境需关闭
     * {@code itsm.register.expose-verification-code}。
     * </p>
     */
    public AuthSendCodeResponse sendCode(String tenantId, String phone) {
        requireEnabledTenant(tenantId);
        String code = generateCode(properties.getCodeLength());
        saveCode(tenantId, phone, code);
        return new AuthSendCodeResponse(
                phone,
                properties.isExposeVerificationCode() ? code : null,
                properties.getCodeTtlSeconds()
        );
    }

    /**
     * 注册新用户并分配默认角色。
     */
    @Transactional
    public AuthRegisterResponse register(String tenantId, AuthRegisterRequest request, String clientIp) {
        checkRateLimit(clientIp);
        requireEnabledTenant(tenantId);
        validatePassword(request);
        ensureUsernameAvailable(tenantId, request.username());
        ensurePhoneAvailable(tenantId, request.phone());
        validateAndConsumeCode(tenantId, request.phone(), request.code());

        String userId = accountGenerator.nextUserId(tenantId);
        String email = accountGenerator.generateEmail(tenantId, request.username());
        try {
            insertUser(userId, tenantId, request.username(), request.phone(), email);
            insertCredential(userId, tenantId, request.username(), request.password());
            assignDefaultRole(userId, tenantId);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.RESOURCE_CONFLICT, "用户名或手机号已被占用");
        }

        return new AuthRegisterResponse(userId, tenantId, request.username(), request.username(), email);
    }

    private void requireEnabledTenant(String tenantId) {
        AuthTenant tenant = tenantRepository.findEnabledTenantById(tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TENANT_FORBIDDEN, "tenant is disabled or not found"));
        if (!tenant.enabled()) {
            throw new BusinessException(ErrorCode.TENANT_FORBIDDEN, "tenant is disabled or not found");
        }
    }

    private void validatePassword(AuthRegisterRequest request) {
        if (!Objects.equals(request.password(), request.confirmPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }
    }

    private void validateAndConsumeCode(String tenantId, String phone, String code) {
        String key = codeKey(tenantId, phone);
        String stored = getFromRedis(key);
        if (stored == null || stored.isBlank()) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_INVALID, "请先获取验证码");
        }
        if (!stored.equals(code)) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_INVALID, "验证码错误");
        }
        deleteFromRedis(key);
    }

    private void ensureUsernameAvailable(String tenantId, String username) {
        if (credentialRepository.findByTenantIdAndLoginName(tenantId, username).isPresent()) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }
    }

    private void ensurePhoneAvailable(String tenantId, String phone) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM app_user WHERE tenant_id = ? AND contact_phone = ?",
                Integer.class,
                tenantId,
                phone
        );
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.PHONE_EXISTS);
        }
    }

    private void insertUser(String userId, String tenantId, String username, String phone, String email) {
        jdbcTemplate.update(
                """
                        INSERT INTO app_user (user_id, tenant_id, display_name, department_name, contact_phone, contact_email, enabled)
                        VALUES (?, ?, ?, NULL, ?, ?, 1)
                        """,
                userId,
                tenantId,
                username,
                phone,
                email
        );
    }

    private void insertCredential(String userId, String tenantId, String username, String rawPassword) {
        jdbcTemplate.update(
                """
                        INSERT INTO user_credential (
                            credential_id, tenant_id, user_id, login_name, password_hash, password_algo,
                            password_version, auth_version, failed_count, locked_until, last_login_at, last_login_ip, status
                        ) VALUES (?, ?, ?, ?, ?, 'bcrypt', 1, 1, 0, NULL, NULL, NULL, 'ACTIVE')
                        """,
                UUID.randomUUID().toString(),
                tenantId,
                userId,
                username,
                passwordEncoder.encode(rawPassword)
        );
    }

    private void assignDefaultRole(String userId, String tenantId) {
        String roleCode = properties.getDefaultRoleCode();
        String roleId = jdbcTemplate.query(
                        "SELECT role_id FROM rbac_role WHERE tenant_id = ? AND role_code = ? LIMIT 1",
                        (rs, rowNum) -> rs.getString("role_id"),
                        tenantId,
                        roleCode
                ).stream()
                .findFirst()
                .orElseGet(() -> createDefaultRole(tenantId, roleCode));
        jdbcTemplate.update(
                "INSERT INTO app_user_role (user_id, role_id, tenant_id) VALUES (?, ?, ?)",
                userId,
                roleId,
                tenantId
        );
    }

    private String createDefaultRole(String tenantId, String roleCode) {
        String roleId = UUID.randomUUID().toString();
        jdbcTemplate.update(
                """
                        INSERT INTO rbac_role (role_id, tenant_id, role_code, role_name, enabled, description)
                        VALUES (?, ?, ?, ?, 1, 'default user role')
                        """,
                roleId,
                tenantId,
                roleCode,
                roleCode
        );
        return roleId;
    }

    private void checkRateLimit(String clientIp) {
        String key = RATE_KEY_PREFIX + clientIp;
        Long count = incrementFromRedis(key);
        if (count != null && count == 1L) {
            expireFromRedis(key, Duration.ofSeconds(properties.getRateLimitWindowSeconds()));
        }
        if (count != null && count > properties.getRateLimitMaxPerIp()) {
            throw new BusinessException(ErrorCode.REGISTER_RATE_LIMITED);
        }
    }

    private String generateCode(int length) {
        int bound = (int) Math.pow(10, length);
        return String.format("%0" + length + "d", secureRandom.nextInt(bound));
    }

    private void saveCode(String tenantId, String phone, String code) {
        setToRedis(codeKey(tenantId, phone), code, Duration.ofSeconds(properties.getCodeTtlSeconds()));
    }

    private String codeKey(String tenantId, String phone) {
        return CODE_KEY_PREFIX + tenantId + ":" + phone;
    }

    private void setToRedis(String key, String value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl);
        } catch (DataAccessException exception) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "验证码服务暂不可用，请检查 Redis 是否已启动");
        }
    }

    private String getFromRedis(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (DataAccessException exception) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "验证码服务暂不可用，请检查 Redis 是否已启动");
        }
    }

    private void deleteFromRedis(String key) {
        try {
            redisTemplate.delete(key);
        } catch (DataAccessException exception) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "验证码服务暂不可用，请检查 Redis 是否已启动");
        }
    }

    private Long incrementFromRedis(String key) {
        try {
            return redisTemplate.opsForValue().increment(key);
        } catch (DataAccessException exception) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "限流服务暂不可用，请检查 Redis 是否已启动");
        }
    }

    private void expireFromRedis(String key, Duration ttl) {
        try {
            redisTemplate.expire(key, ttl);
        } catch (DataAccessException exception) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "限流服务暂不可用，请检查 Redis 是否已启动");
        }
    }
}
