package com.cenziang.itsmserver.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cenziang.itsmpojo.entity.TicketClassificationEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工单分类 Mapper。
 */
@Mapper
public interface TicketClassificationMapper extends BaseMapper<TicketClassificationEntity> {
}
