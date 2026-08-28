package com.cenziang.itsmserver.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cenziang.itsmpojo.entity.TicketEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 工单主表 Mapper。
 */
@Mapper
public interface TicketMapper extends BaseMapper<TicketEntity> {

    /**
     * 取当前租户最大的数字工单号，无工单时返回 999999（下一个即 1000000）。
     */
    @Select("SELECT COALESCE(MAX(CAST(ticket_no AS UNSIGNED)), 999999) FROM ticket WHERE tenant_id = #{tenantId}")
    Long selectMaxTicketNo(@Param("tenantId") String tenantId);
}
