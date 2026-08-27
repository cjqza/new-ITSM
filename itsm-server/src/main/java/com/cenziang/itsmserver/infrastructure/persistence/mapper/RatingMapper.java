package com.cenziang.itsmserver.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cenziang.itsmpojo.entity.RatingEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工单评价 Mapper。
 */
@Mapper
public interface RatingMapper extends BaseMapper<RatingEntity> {
}
