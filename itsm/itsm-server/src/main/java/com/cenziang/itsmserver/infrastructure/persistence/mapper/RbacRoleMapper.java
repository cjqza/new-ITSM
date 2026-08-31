package com.cenziang.itsmserver.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cenziang.itsmpojo.entity.RbacRoleEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色 Mapper。
 */
@Mapper
public interface RbacRoleMapper extends BaseMapper<RbacRoleEntity> {
}
