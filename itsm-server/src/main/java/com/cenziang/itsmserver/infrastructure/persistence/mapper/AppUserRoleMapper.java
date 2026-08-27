package com.cenziang.itsmserver.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cenziang.itsmpojo.entity.AppUserRoleEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户角色关联 Mapper。
 */
@Mapper
public interface AppUserRoleMapper extends BaseMapper<AppUserRoleEntity> {
}
