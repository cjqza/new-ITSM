package com.cenziang.itsmserver.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cenziang.itsmpojo.entity.AppUserEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户主数据 Mapper。
 */
@Mapper
public interface AppUserMapper extends BaseMapper<AppUserEntity> {
}
