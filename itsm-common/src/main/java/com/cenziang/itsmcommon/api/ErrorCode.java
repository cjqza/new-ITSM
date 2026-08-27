package com.cenziang.itsmcommon.api;

/**
 * 一期统一业务错误码。
 * <p>
 * 这些错误码对齐接口文档中的统一异常约定，供所有接口共享。
 * </p>
 */
public enum ErrorCode {
    /**
     * 参数校验错误。
     */
    VALIDATION_ERROR(400, "VALIDATION_ERROR", "参数校验失败"),

    /**
     * 认证失败。
     */
    AUTH_REQUIRED(401, "AUTH_REQUIRED", "未登录或令牌无效"),

    /** 登录凭证错误。 */
    AUTH_INVALID_CREDENTIAL(401, "AUTH_INVALID_CREDENTIAL", "用户名或密码错误"),

    /** 登录凭证已锁定。 */
    AUTH_CREDENTIAL_LOCKED(423, "AUTH_CREDENTIAL_LOCKED", "登录凭证已锁定"),

    /** 令牌无效或已过期。 */
    TOKEN_INVALID(401, "TOKEN_INVALID", "令牌无效或已过期"),

    /** 缺少租户上下文。 */
    TENANT_REQUIRED(400, "TENANT_REQUIRED", "缺少租户上下文"),

    /** 缺少必需请求头。 */
    MISSING_HEADER(400, "MISSING_HEADER", "缺少必需请求头"),

    /**
     * 当前租户不可访问。
     */
    TENANT_FORBIDDEN(403, "TENANT_FORBIDDEN", "租户无权访问该资源"),

    /**
     * 当前角色无权限。
     */
    ROLE_FORBIDDEN(403, "ROLE_FORBIDDEN", "当前角色无权执行该操作"),

    /**
     * 当前数据范围无权限。
     */
    DATA_SCOPE_FORBIDDEN(403, "DATA_SCOPE_FORBIDDEN", "当前数据范围无权访问该资源"),

    /**
     * 资源不存在。
     */
    RESOURCE_NOT_FOUND(404, "RESOURCE_NOT_FOUND", "资源不存在"),

    /**
     * 幂等冲突。
     */
    IDEMPOTENCY_CONFLICT(409, "IDEMPOTENCY_CONFLICT", "幂等键对应的请求体不一致"),

    /**
     * 资源冲突。
     */
    RESOURCE_CONFLICT(409, "RESOURCE_CONFLICT", "资源已被其他请求修改"),

    /**
     * 非法状态迁移。
     */
    ILLEGAL_STATE_TRANSITION(409, "ILLEGAL_STATE_TRANSITION", "当前状态不允许执行该操作"),

    /**
     * 字典项已停用。
     */
    DICTIONARY_ITEM_DISABLED(409, "DICTIONARY_ITEM_DISABLED", "字典项已停用"),

    /**
     * Agent 不可用。
     */
    AGENT_UNAVAILABLE(503, "AGENT_UNAVAILABLE", "Agent 当前不可用"),

    /**
     * 服务不可用。
     */
    SERVICE_UNAVAILABLE(503, "SERVICE_UNAVAILABLE", "服务暂时不可用"),

    /**
     * 注册请求过于频繁。
     */
    REGISTER_RATE_LIMITED(429, "REGISTER_RATE_LIMITED", "注册请求过于频繁，请稍后再试"),

    /**
     * 验证码错误或已过期。
     */
    VERIFICATION_CODE_INVALID(400, "VERIFICATION_CODE_INVALID", "验证码错误或已过期"),

    /**
     * 两次输入的密码不一致。
     */
    PASSWORD_MISMATCH(400, "PASSWORD_MISMATCH", "两次输入的密码不一致"),

    /**
     * 用户名已存在。
     */
    USERNAME_EXISTS(409, "USERNAME_EXISTS", "用户名已存在"),

    /**
     * 手机号已注册。
     */
    PHONE_EXISTS(409, "PHONE_EXISTS", "手机号已注册"),

    /**
     * 系统内部错误。
     */
    INTERNAL_ERROR(500, "INTERNAL_ERROR", "系统内部错误");

    private final int httpStatus;
    private final String code;
    private final String defaultMessage;

    ErrorCode(int httpStatus, String code, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    /**
     * 获取 HTTP 状态码。
     *
     * @return HTTP 状态码
     */
    public int httpStatus() {
        return httpStatus;
    }

    /**
     * 获取业务错误码。
     *
     * @return 业务错误码
     */
    public String code() {
        return code;
    }

    /**
     * 获取默认错误提示。
     *
     * @return 错误提示
     */
    public String defaultMessage() {
        return defaultMessage;
    }
}
