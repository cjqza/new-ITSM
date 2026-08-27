package com.cenziang.itsmcommon.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * 通用分页响应体。
 *
 * @param <T> 列表元素类型
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PageResponse<T>(
        List<T> items,
        long page,
        long pageSize,
        long total,
        boolean hasNext
) {
    /**
     * 构造分页响应。
     *
     * @param items    当前页数据
     * @param page     页码
     * @param pageSize 页大小
     * @param total    总数
     * @param <T>      元素类型
     * @return 分页对象
     */
    public static <T> PageResponse<T> of(List<T> items, long page, long pageSize, long total) {
        boolean hasNext = page * pageSize < total;
        return new PageResponse<>(items, page, pageSize, total, hasNext);
    }
}
