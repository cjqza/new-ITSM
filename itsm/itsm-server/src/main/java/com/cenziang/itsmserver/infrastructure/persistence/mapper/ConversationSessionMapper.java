package com.cenziang.itsmserver.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cenziang.itsmpojo.entity.ConversationSessionEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会话 Mapper。
 */
@Mapper
public interface ConversationSessionMapper extends BaseMapper<ConversationSessionEntity> {
}
