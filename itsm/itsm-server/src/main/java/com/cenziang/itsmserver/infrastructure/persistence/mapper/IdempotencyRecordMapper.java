package com.cenziang.itsmserver.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cenziang.itsmpojo.entity.IdempotencyRecordEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 幂等记录 Mapper。
 */
@Mapper
public interface IdempotencyRecordMapper extends BaseMapper<IdempotencyRecordEntity> {
}
