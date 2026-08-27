package com.cenziang.itsmserver.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cenziang.itsmpojo.entity.AgentDecisionEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * Agent 决策 Mapper。
 */
@Mapper
public interface AgentDecisionMapper extends BaseMapper<AgentDecisionEntity> {
}
