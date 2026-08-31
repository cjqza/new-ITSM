package com.cenziang.itsmserver.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cenziang.itsmpojo.entity.DictionaryItemEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 字典项 Mapper。
 */
@Mapper
public interface DictionaryItemMapper extends BaseMapper<DictionaryItemEntity> {
}
