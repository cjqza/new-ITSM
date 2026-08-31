package com.cenziang.itsmserver.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cenziang.itsmpojo.entity.RbacRolePermissionEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色权限关联 Mapper。
 */
@Mapper
public interface RbacRolePermissionMapper extends BaseMapper<RbacRolePermissionEntity> {
}
