package com.cenziang.itsmserver.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cenziang.itsmpojo.entity.TicketActionLogEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工单动作流水 Mapper。
 */
@Mapper
public interface TicketActionLogMapper extends BaseMapper<TicketActionLogEntity> {
}
