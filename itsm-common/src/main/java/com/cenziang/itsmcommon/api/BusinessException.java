package com.cenziang.itsmcommon.api;

/**
 * 业务异常。
 * <p>
 * 所有可预期的领域错误都转换成这个异常，再由后端统一渲染为 ApiResponse。
 * </p>
 */
public class BusinessException extends RuntimeException {
    private final String code;
    private final int httpStatus;
    private final Object details;

    /**
     * 构造业务异常。
     *
     * @param code        错误码
     * @param message     错误提示
     * @param httpStatus  HTTP 状态码
     * @param details     扩展信息
     */
    public BusinessException(String code, String message, int httpStatus, Object details) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
        this.details = details;
    }

    /**
     * 使用统一错误码构造异常。
     *
     * @param errorCode 错误码枚举
     */
    public BusinessException(ErrorCode errorCode) {
        this(errorCode.code(), errorCode.defaultMessage(), errorCode.httpStatus(), null);
    }

    /**
     * 使用统一错误码构造异常。
     *
     * @param errorCode 错误码枚举
     * @param message   自定义提示
     */
    public BusinessException(ErrorCode errorCode, String message) {
        this(errorCode.code(), message, errorCode.httpStatus(), null);
    }

    /**
     * 使用统一错误码构造异常。
     *
     * @param errorCode 错误码枚举
     * @param message   自定义提示
     * @param details   扩展信息
     */
    public BusinessException(ErrorCode errorCode, String message, Object details) {
        this(errorCode.code(), message, errorCode.httpStatus(), details);
    }

    /**
     * 获取业务错误码。
     *
     * @return 业务错误码
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取 HTTP 状态码。
     *
     * @return HTTP 状态码
     */
    public int getHttpStatus() {
        return httpStatus;
    }

    /**
     * 获取扩展信息。
     *
     * @return 扩展信息
     */
    public Object getDetails() {
        return details;
    }
}
