package com.cenziang.itsmserver.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cenziang.itsmpojo.entity.UserCredentialEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户凭证 Mapper。
 */
@Mapper
public interface UserCredentialMapper extends BaseMapper<UserCredentialEntity> {
}
