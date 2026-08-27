package com.cenziang.itsmpojo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 字典相关 DTO。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class DictionaryDtos {
    private DictionaryDtos() {
    }

    @Schema(description = "字典分页查询参数")
    public record DictionaryItemQuery(
            @Schema(description = "父级主键") String parentId,
            @Schema(description = "是否仅启用") Boolean enabledOnly,
            @Schema(description = "关键字") String keyword,
            @Schema(description = "页码") Integer page,
            @Schema(description = "页大小") Integer pageSize
    ) {
    }

    @Schema(description = "新增字典项请求")
    public record DictionaryItemCreateRequest(
            @Schema(description = "业务编码") String code,
            @Schema(description = "显示名称") String name,
            @Schema(description = "父级主键") String parentId,
            @Schema(description = "排序号") Integer sort
    ) {
    }

    @Schema(description = "更新字典项请求")
    public record DictionaryItemUpdateRequest(
            @Schema(description = "显示名称") String name,
            @Schema(description = "父级主键") String parentId,
            @Schema(description = "排序号") Integer sort,
            @Schema(description = "版本号") Long version,
            @Schema(description = "备注说明") String description
    ) {
    }

    @Schema(description = "停用字典项请求")
    public record DictionaryItemDisableRequest(
            @Schema(description = "停用原因") String reason,
            @Schema(description = "版本号") Long version
    ) {
    }

    @Schema(description = "字典项响应")
    public record DictionaryItemResponse(
            @Schema(description = "字典项主键") String itemId,
            @Schema(description = "字典类型") String dictType,
            @Schema(description = "业务编码") String code,
            @Schema(description = "显示名称") String name,
            @Schema(description = "父级主键") String parentId,
            @Schema(description = "是否启用") Boolean enabled,
            @Schema(description = "排序号") Integer sort,
            @Schema(description = "版本号") Long version,
            @Schema(description = "说明") String description,
            @Schema(description = "停用时间") LocalDateTime disabledAt,
            @Schema(description = "停用人") String disabledBy
    ) {
    }
}