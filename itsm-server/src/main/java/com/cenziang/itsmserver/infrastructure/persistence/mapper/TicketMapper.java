package com.cenziang.itsmserver.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cenziang.itsmpojo.entity.TicketEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工单主表 Mapper。
 */
@Mapper
public interface TicketMapper extends BaseMapper<TicketEntity> {
}
