package com.cenziang.itsmserver.api;

import com.cenziang.itsmcommon.api.ApiResponse;
import com.cenziang.itsmcommon.api.PageResponse;
import com.cenziang.itsmpojo.dto.DictionaryDtos;
import com.cenziang.itsmserver.application.RequestContext;
import com.cenziang.itsmserver.service.DictionaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员字典接口。
 */
@Tag(name = "字典", description = "管理单元、症状、原因、解决方法等字典的维护")
@RestController
@RequestMapping("/api/v1/admin/dictionaries")
public class DictionaryController extends ControllerSupport {
    private final DictionaryService dictionaryService;

    public DictionaryController(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    @Operation(summary = "查询字典项", description = "按字典类型分页查询启用字典项")
    @GetMapping("/{dictType}/items")
    public ApiResponse<PageResponse<DictionaryDtos.DictionaryItemResponse>> query(@Parameter(description = "租户 ID", required = true) @RequestHeader("X-Tenant-Id") String tenantId,
                                                                                  @Parameter(description = "字典类型", required = true) @PathVariable("dictType") String dictType,
                                                                                  @Parameter(description = "父项 ID") @RequestParam(required = false) String parentId,
                                                                                  @Parameter(description = "是否仅启用") @RequestParam(defaultValue = "true") boolean enabledOnly,
                                                                                  @Parameter(description = "关键字") @RequestParam(required = false) String keyword,
                                                                                  @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
                                                                                  @Parameter(description = "页大小") @RequestParam(defaultValue = "20") int pageSize,
                                                                                  HttpServletRequest httpServletRequest) {
        RequestContext context = context(httpServletRequest, tenantId);
        DictionaryDtos.DictionaryItemQuery query = new DictionaryDtos.DictionaryItemQuery(parentId, enabledOnly, keyword, page, pageSize);
        return ok(dictionaryService.query(context, dictType, query), httpServletRequest);
    }

    @Operation(summary = "新增字典项", description = "管理员新增业务线、管理单元、症状、原因等字典项")
    @PostMapping("/{dictType}/items")
    public ApiResponse<DictionaryDtos.DictionaryItemResponse> create(@Parameter(description = "租户 ID", required = true) @RequestHeader("X-Tenant-Id") String tenantId,
                                                                     @Parameter(description = "字典类型", required = true) @PathVariable("dictType") String dictType,
                                                                     @Valid @RequestBody DictionaryDtos.DictionaryItemCreateRequest request,
                                                                     HttpServletRequest httpServletRequest) {
        return ok(dictionaryService.create(context(httpServletRequest, tenantId), dictType, request), httpServletRequest);
    }

    @Operation(summary = "更新字典项", description = "修改字典名称、排序、父项或说明")
    @PatchMapping("/items/{itemId}")
    public ApiResponse<DictionaryDtos.DictionaryItemResponse> update(@Parameter(description = "租户 ID", required = true) @RequestHeader("X-Tenant-Id") String tenantId,
                                                                     @Parameter(description = "字典项 ID", required = true) @PathVariable("itemId") String itemId,
                                                                     @Valid @RequestBody DictionaryDtos.DictionaryItemUpdateRequest request,
                                                                     HttpServletRequest httpServletRequest) {
        return ok(dictionaryService.update(context(httpServletRequest, tenantId), itemId, request), httpServletRequest);
    }

    @Operation(summary = "停用字典项", description = "停止新工单选择某个字典项，同时保留历史数据可读性")
    @PostMapping("/items/{itemId}/disable")
    public ApiResponse<DictionaryDtos.DictionaryItemResponse> disable(@Parameter(description = "租户 ID", required = true) @RequestHeader("X-Tenant-Id") String tenantId,
                                                                      @Parameter(description = "字典项 ID", required = true) @PathVariable("itemId") String itemId,
                                                                      @Valid @RequestBody DictionaryDtos.DictionaryItemDisableRequest request,
                                                                      HttpServletRequest httpServletRequest) {
        return ok(dictionaryService.disable(context(httpServletRequest, tenantId), itemId, request), httpServletRequest);
    }
}