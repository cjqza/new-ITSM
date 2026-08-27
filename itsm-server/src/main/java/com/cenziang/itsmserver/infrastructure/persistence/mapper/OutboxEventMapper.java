package com.cenziang.itsmserver.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cenziang.itsmpojo.entity.OutboxEventEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * Outbox 事件 Mapper。
 */
@Mapper
public interface OutboxEventMapper extends BaseMapper<OutboxEventEntity> {
}
