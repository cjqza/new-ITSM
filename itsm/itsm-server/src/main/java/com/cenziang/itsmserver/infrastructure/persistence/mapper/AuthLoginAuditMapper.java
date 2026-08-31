package com.cenziang.itsmserver.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cenziang.itsmpojo.entity.AuthLoginAuditEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 登录审计 Mapper。
 */
@Mapper
public interface AuthLoginAuditMapper extends BaseMapper<AuthLoginAuditEntity> {
}
