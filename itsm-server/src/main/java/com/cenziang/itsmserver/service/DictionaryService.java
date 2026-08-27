package com.cenziang.itsmserver.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cenziang.itsmcommon.api.BusinessException;
import com.cenziang.itsmcommon.api.ErrorCode;
import com.cenziang.itsmcommon.api.PageResponse;
import com.cenziang.itsmpojo.dto.DictionaryDtos;
import com.cenziang.itsmpojo.entity.DictionaryItemEntity;
import com.cenziang.itsmserver.application.RequestContext;
import com.cenziang.itsmserver.infrastructure.persistence.mapper.DictionaryItemMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 字典服务。
 * <p>
 * 提供管理单元、症状、原因、解决方法等字典项的查询和软停用维护。
 * </p>
 */
@Service
public class DictionaryService {
    private static final Set<String> VALID_TYPES = Set.of(
            "BUSINESS_LINE", "MANAGEMENT_UNIT", "SYMPTOM", "REASON", "SOLUTION_METHOD", "RATING_TAG");

    private final DictionaryItemMapper mapper;

    public DictionaryService(DictionaryItemMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 查询字典项。
     */
    @Transactional(readOnly = true)
    public PageResponse<DictionaryDtos.DictionaryItemResponse> query(RequestContext context, String dictType,
                                                                     DictionaryDtos.DictionaryItemQuery query) {
        if (!VALID_TYPES.contains(dictType)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "invalid dictType");
        }
        int page = query.page() == null ? 1 : query.page();
        int pageSize = query.pageSize() == null ? 20 : query.pageSize();
        Page<DictionaryItemEntity> result = mapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<DictionaryItemEntity>()
                        .eq(DictionaryItemEntity::getTenantId, context.tenantId())
                        .eq(DictionaryItemEntity::getDictType, dictType)
                        .eq(query.enabledOnly() == null || query.enabledOnly(), DictionaryItemEntity::getEnabled, true)
                        .eq(query.parentId() != null, DictionaryItemEntity::getParentId, query.parentId())
                        .and(query.keyword() != null && !query.keyword().isBlank(), w -> w
                                .like(DictionaryItemEntity::getName, query.keyword())
                                .or().like(DictionaryItemEntity::getCode, query.keyword()))
                        .orderByAsc(DictionaryItemEntity::getSortNo));
        List<DictionaryDtos.DictionaryItemResponse> items = result.getRecords().stream().map(this::toResponse).toList();
        return PageResponse.of(items, result.getCurrent(), result.getSize(), result.getTotal());
    }

    /**
     * 新增字典项。
     */
    @Transactional
    public DictionaryDtos.DictionaryItemResponse create(RequestContext context, String dictType,
                                                        DictionaryDtos.DictionaryItemCreateRequest request) {
        if (!VALID_TYPES.contains(dictType)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "invalid dictType");
        }
        if (request.code() == null || request.code().isBlank() || request.name() == null || request.name().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "code and name are required");
        }
        Long count = mapper.selectCount(new LambdaQueryWrapper<DictionaryItemEntity>()
                .eq(DictionaryItemEntity::getTenantId, context.tenantId())
                .eq(DictionaryItemEntity::getDictType, dictType)
                .eq(DictionaryItemEntity::getCode, request.code()));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.RESOURCE_CONFLICT, "code already exists");
        }
        DictionaryItemEntity entity = new DictionaryItemEntity()
                .setItemId("dict_" + UUID.randomUUID().toString().replace("-", ""))
                .setTenantId(context.tenantId())
                .setDictType(dictType)
                .setCode(request.code())
                .setName(request.name())
                .setParentId(request.parentId())
                .setEnabled(true)
                .setSortNo(request.sort() == null ? 0 : request.sort());
        mapper.insert(entity);
        return toResponse(entity);
    }

    /**
     * 更新字典项名称、排序、父项和说明。
     */
    @Transactional
    public DictionaryDtos.DictionaryItemResponse update(RequestContext context, String itemId,
                                                        DictionaryDtos.DictionaryItemUpdateRequest request) {
        DictionaryItemEntity entity = requireItem(context.tenantId(), itemId);
        if (request.version() == null || !request.version().equals(entity.getVersion())) {
            throw new BusinessException(ErrorCode.RESOURCE_CONFLICT, "version mismatch");
        }
        entity.setName(request.name() == null ? entity.getName() : request.name())
                .setParentId(request.parentId())
                .setSortNo(request.sort() == null ? entity.getSortNo() : request.sort())
                .setDescription(request.description());
        entity.setVersion(entity.getVersion() + 1);
        mapper.updateById(entity);
        return toResponse(entity);
    }

    /**
     * 软停用字典项。
     */
    @Transactional
    public DictionaryDtos.DictionaryItemResponse disable(RequestContext context, String itemId,
                                                         DictionaryDtos.DictionaryItemDisableRequest request) {
        DictionaryItemEntity entity = requireItem(context.tenantId(), itemId);
        if (request.version() == null || !request.version().equals(entity.getVersion())) {
            throw new BusinessException(ErrorCode.RESOURCE_CONFLICT, "version mismatch");
        }
        if (Boolean.FALSE.equals(entity.getEnabled())) {
            throw new BusinessException(ErrorCode.RESOURCE_CONFLICT, "item already disabled");
        }
        Long children = mapper.selectCount(new LambdaQueryWrapper<DictionaryItemEntity>()
                .eq(DictionaryItemEntity::getTenantId, context.tenantId())
                .eq(DictionaryItemEntity::getParentId, itemId)
                .eq(DictionaryItemEntity::getEnabled, true));
        if (children != null && children > 0) {
            throw new BusinessException(ErrorCode.RESOURCE_CONFLICT, "cannot disable item with enabled children");
        }
        entity.setEnabled(false)
                .setDisabledReason(request.reason())
                .setDisabledAt(LocalDateTime.now())
                .setDisabledBy(context.userId());
        entity.setVersion(entity.getVersion() + 1);
        mapper.updateById(entity);
        return toResponse(entity);
    }

    private DictionaryItemEntity requireItem(String tenantId, String itemId) {
        DictionaryItemEntity entity = mapper.selectOne(new LambdaQueryWrapper<DictionaryItemEntity>()
                .eq(DictionaryItemEntity::getTenantId, tenantId)
                .eq(DictionaryItemEntity::getItemId, itemId));
        if (entity == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "dictionary item not found");
        }
        return entity;
    }

    private DictionaryDtos.DictionaryItemResponse toResponse(DictionaryItemEntity entity) {
        return new DictionaryDtos.DictionaryItemResponse(
                entity.getItemId(), entity.getDictType(), entity.getCode(), entity.getName(), entity.getParentId(),
                entity.getEnabled(), entity.getSortNo(), entity.getVersion(), entity.getDescription(),
                entity.getDisabledAt(), entity.getDisabledBy());
    }
}