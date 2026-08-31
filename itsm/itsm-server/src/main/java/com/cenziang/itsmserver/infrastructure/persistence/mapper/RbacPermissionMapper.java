package com.cenziang.itsmserver.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cenziang.itsmpojo.entity.RbacPermissionEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 权限 Mapper。
 */
@Mapper
public interface RbacPermissionMapper extends BaseMapper<RbacPermissionEntity> {
}
