package com.cenziang.itsmserver.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cenziang.itsmcommon.api.BusinessException;
import com.cenziang.itsmcommon.api.ErrorCode;
import com.cenziang.itsmcommon.api.PageResponse;
import com.cenziang.itsmpojo.dto.PermissionDtos;
import com.cenziang.itsmpojo.entity.AppUserRoleEntity;
import com.cenziang.itsmpojo.entity.RbacPermissionEntity;
import com.cenziang.itsmpojo.entity.RbacRoleEntity;
import com.cenziang.itsmpojo.entity.RbacRolePermissionEntity;
import com.cenziang.itsmserver.application.RequestContext;
import com.cenziang.itsmserver.infrastructure.persistence.mapper.AppUserRoleMapper;
import com.cenziang.itsmserver.infrastructure.persistence.mapper.RbacPermissionMapper;
import com.cenziang.itsmserver.infrastructure.persistence.mapper.RbacRoleMapper;
import com.cenziang.itsmserver.infrastructure.persistence.mapper.RbacRolePermissionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * 权限服务。
 * <p>
 * 提供当前用户角色权限、角色列表和角色权限查询，驱动前端三类工作台可见性。
 * </p>
 */
@Service
public class PermissionService {
    private final AppUserRoleMapper userRoleMapper;
    private final RbacRoleMapper roleMapper;
    private final RbacPermissionMapper permissionMapper;
    private final RbacRolePermissionMapper rolePermissionMapper;

    public PermissionService(AppUserRoleMapper userRoleMapper, RbacRoleMapper roleMapper,
                             RbacPermissionMapper permissionMapper, RbacRolePermissionMapper rolePermissionMapper) {
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.rolePermissionMapper = rolePermissionMapper;
    }

    /**
     * 查询当前用户角色、权限、菜单和数据范围。
     */
    @Transactional(readOnly = true)
    public PermissionDtos.PermissionMeResponse me(RequestContext context) {
        List<RbacRoleEntity> roles = findUserRoles(context.tenantId(), context.userId());
        Set<String> permissionIds = findRolePermissionIds(roles);
        List<RbacPermissionEntity> permissions = permissionIds.isEmpty() ? List.of()
                : permissionMapper.selectList(new LambdaQueryWrapper<RbacPermissionEntity>()
                        .eq(RbacPermissionEntity::getTenantId, context.tenantId())
                        .in(RbacPermissionEntity::getPermissionId, permissionIds)
                        .eq(RbacPermissionEntity::getEnabled, true));

        List<String> permissionCodes = permissions.stream().map(RbacPermissionEntity::getPermissionCode).distinct().toList();
        List<String> menus = permissions.stream().filter(p -> "MENU".equalsIgnoreCase(p.getPermissionType()))
                .map(RbacPermissionEntity::getPermissionCode).distinct().toList();
        PermissionDtos.DataScopeView dataScope = resolveDataScope(roles);
        return new PermissionDtos.PermissionMeResponse(context.userId(), context.tenantId(),
                roles.stream().map(this::toRoleSummary).toList(), permissionCodes, menus, dataScope, context.permissionsVersion());
    }

    /**
     * 管理员查询角色分页。
     */
    @Transactional(readOnly = true)
    public PageResponse<PermissionDtos.RoleSummary> listRoles(RequestContext context, PermissionDtos.RolePageQuery query) {
        int page = query.page() == null ? 1 : query.page();
        int pageSize = query.pageSize() == null ? 20 : query.pageSize();
        Page<RbacRoleEntity> result = roleMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<RbacRoleEntity>()
                        .eq(RbacRoleEntity::getTenantId, context.tenantId())
                        .eq(query.enabledOnly() == null || query.enabledOnly(), RbacRoleEntity::getEnabled, true)
                        .and(query.keyword() != null && !query.keyword().isBlank(), w -> w
                                .like(RbacRoleEntity::getRoleName, query.keyword())
                                .or().like(RbacRoleEntity::getRoleCode, query.keyword()))
                        .orderByAsc(RbacRoleEntity::getRoleCode));
        List<PermissionDtos.RoleSummary> roles = result.getRecords().stream().map(this::toRoleSummary).toList();
        return PageResponse.of(roles, result.getCurrent(), result.getSize(), result.getTotal());
    }

    /**
     * 查询角色权限明细。
     */
    @Transactional(readOnly = true)
    public PermissionDtos.RolePermissionResponse rolePermissions(RequestContext context, String roleId) {
        RbacRoleEntity role = roleMapper.selectOne(new LambdaQueryWrapper<RbacRoleEntity>()
                .eq(RbacRoleEntity::getTenantId, context.tenantId())
                .eq(RbacRoleEntity::getRoleId, roleId));
        if (role == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "role not found");
        }
        List<RbacRolePermissionEntity> links = rolePermissionMapper.selectList(new LambdaQueryWrapper<RbacRolePermissionEntity>()
                .eq(RbacRolePermissionEntity::getTenantId, context.tenantId())
                .eq(RbacRolePermissionEntity::getRoleId, roleId));
        List<String> permissionIds = links.stream().map(RbacRolePermissionEntity::getPermissionId).toList();
        List<RbacPermissionEntity> permissions = permissionIds.isEmpty() ? List.of()
                : permissionMapper.selectList(new LambdaQueryWrapper<RbacPermissionEntity>()
                        .eq(RbacPermissionEntity::getTenantId, context.tenantId())
                        .in(RbacPermissionEntity::getPermissionId, permissionIds)
                        .eq(RbacPermissionEntity::getEnabled, true));
        List<String> menus = permissions.stream().filter(p -> "MENU".equalsIgnoreCase(p.getPermissionType()))
                .map(RbacPermissionEntity::getPermissionCode).distinct().toList();
        return new PermissionDtos.RolePermissionResponse(roleId, role.getRoleCode(),
                permissions.stream().map(p -> new PermissionDtos.PermissionItem(p.getPermissionCode(), p.getPermissionName(), p.getPermissionType())).toList(),
                menus, resolveDataScope(List.of(role)));
    }

    private List<RbacRoleEntity> findUserRoles(String tenantId, String userId) {
        List<AppUserRoleEntity> links = userRoleMapper.selectList(new LambdaQueryWrapper<AppUserRoleEntity>()
                .eq(AppUserRoleEntity::getTenantId, tenantId)
                .eq(AppUserRoleEntity::getUserId, userId));
        if (links.isEmpty()) {
            return List.of();
        }
        List<String> roleIds = links.stream().map(AppUserRoleEntity::getRoleId).toList();
        return roleMapper.selectList(new LambdaQueryWrapper<RbacRoleEntity>()
                .eq(RbacRoleEntity::getTenantId, tenantId)
                .in(RbacRoleEntity::getRoleId, roleIds)
                .eq(RbacRoleEntity::getEnabled, true));
    }

    private Set<String> findRolePermissionIds(List<RbacRoleEntity> roles) {
        if (roles.isEmpty()) {
            return Set.of();
        }
        List<String> roleIds = roles.stream().map(RbacRoleEntity::getRoleId).toList();
        return rolePermissionMapper.selectList(new LambdaQueryWrapper<RbacRolePermissionEntity>()
                        .eq(RbacRolePermissionEntity::getTenantId, roles.get(0).getTenantId())
                        .in(RbacRolePermissionEntity::getRoleId, roleIds))
                .stream().map(RbacRolePermissionEntity::getPermissionId).collect(java.util.stream.Collectors.toSet());
    }

    private PermissionDtos.RoleSummary toRoleSummary(RbacRoleEntity role) {
        Long count = rolePermissionMapper.selectCount(new LambdaQueryWrapper<RbacRolePermissionEntity>()
                .eq(RbacRolePermissionEntity::getTenantId, role.getTenantId())
                .eq(RbacRolePermissionEntity::getRoleId, role.getRoleId()));
        return new PermissionDtos.RoleSummary(role.getRoleId(), role.getRoleCode(), role.getRoleName(),
                role.getEnabled(), role.getDescription(), count == null ? 0 : count.intValue());
    }

    private PermissionDtos.DataScopeView resolveDataScope(List<RbacRoleEntity> roles) {
        if (roles.stream().anyMatch(r -> "SUPPORT_ADMIN".equalsIgnoreCase(r.getRoleCode()) || "SUPERVISOR".equalsIgnoreCase(r.getRoleCode()))) {
            return new PermissionDtos.DataScopeView("TENANT", null, null);
        }
        if (roles.stream().anyMatch(r -> "SUPPORT_AGENT".equalsIgnoreCase(r.getRoleCode()))) {
            return new PermissionDtos.DataScopeView("BUSINESS_LINE", List.of("IT_SUPPORT"), null);
        }
        return new PermissionDtos.DataScopeView("SELF", null, null);
    }
}