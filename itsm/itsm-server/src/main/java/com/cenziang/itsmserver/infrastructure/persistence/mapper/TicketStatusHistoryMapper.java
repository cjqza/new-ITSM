package com.cenziang.itsmserver.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cenziang.itsmpojo.entity.TicketStatusHistoryEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工单状态历史 Mapper。
 */
@Mapper
public interface TicketStatusHistoryMapper extends BaseMapper<TicketStatusHistoryEntity> {
}
