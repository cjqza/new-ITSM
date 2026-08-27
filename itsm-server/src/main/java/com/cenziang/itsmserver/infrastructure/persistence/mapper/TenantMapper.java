package com.cenziang.itsmserver.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cenziang.itsmpojo.entity.TenantEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 租户表 Mapper。
 */
@Mapper
public interface TenantMapper extends BaseMapper<TenantEntity> {
}
