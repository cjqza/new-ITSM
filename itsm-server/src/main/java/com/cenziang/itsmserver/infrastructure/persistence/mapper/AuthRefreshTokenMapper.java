package com.cenziang.itsmserver.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cenziang.itsmpojo.entity.AuthRefreshTokenEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 刷新令牌 Mapper。
 */
@Mapper
public interface AuthRefreshTokenMapper extends BaseMapper<AuthRefreshTokenEntity> {
}
