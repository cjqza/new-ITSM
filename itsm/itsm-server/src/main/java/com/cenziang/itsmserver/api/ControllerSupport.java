package com.cenziang.itsmserver.api;

import com.cenziang.itsmcommon.api.ApiResponse;
import com.cenziang.itsmserver.application.RequestContext;
import com.cenziang.itsmserver.application.RequestContextHolder;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 控制器公共支撑。
 * <p>
 * 统一 traceId 读取、请求上下文解析和成功响应包装。
 * </p>
 */
public abstract class ControllerSupport {

    protected String traceId(HttpServletRequest request) {
        return request.getHeader("X-Trace-Id");
    }

    protected RequestContext context(HttpServletRequest request, String tenantId) {
        return RequestContextHolder.resolve(request, tenantId);
    }

    protected <T> ApiResponse<T> ok(T data, HttpServletRequest request) {
        return ApiResponse.success(data, traceId(request));
    }
}