package com.cenziang.itsmcommon.api;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 统一接口返回体。
 * <p>
 * 所有后端接口都通过这个壳子返回 code、message、data、traceId 和 details，
 * 方便前端、测试和联调团队统一解析。
 * </p>
 *
 * @param <T> 业务数据类型
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        String code,
        String message,
        T data,
        String traceId,
        Object details
) {
    /**
     * 构造成功响应。
     *
     * @param data    业务数据
     * @param traceId 链路 ID
     * @param <T>     数据类型
     * @return 成功响应
     */
    public static <T> ApiResponse<T> success(T data, String traceId) {
        return new ApiResponse<>("SUCCESS", "success", data, traceId, null);
    }

    /**
     * 构造成功响应。
     *
     * @param data 业务数据
     * @param <T>  数据类型
     * @return 成功响应
     */
    public static <T> ApiResponse<T> success(T data) {
        return success(data, null);
    }

    /**
     * 构造失败响应。
     *
     * @param code    业务错误码
     * @param message 错误提示
     * @param traceId 链路 ID
     * @param details 扩展细节
     * @param <T>     数据类型
     * @return 失败响应
     */
    public static <T> ApiResponse<T> failure(String code, String message, String traceId, Object details) {
        return new ApiResponse<>(code, message, null, traceId, details);
    }

    /**
     * 构造失败响应。
     *
     * @param code    业务错误码
     * @param message 错误提示
     * @param traceId 链路 ID
     * @param <T>     数据类型
     * @return 失败响应
     */
    public static <T> ApiResponse<T> failure(String code, String message, String traceId) {
        return failure(code, message, traceId, null);
    }
}
