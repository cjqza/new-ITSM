package com.cenziang.itsmserver.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 历史工单清理任务。
 * <p>
 * 每天凌晨 3 点清理创建时间超过保留期（默认 30 天）的工单及其关联数据。
 * </p>
 */
@Component
public class TicketCleanupJob {
    private final JdbcTemplate jdbcTemplate;
    private final int retentionDays;

    public TicketCleanupJob(JdbcTemplate jdbcTemplate,
                            @Value("${itsm.ticket.retention-days:30}") int retentionDays) {
        this.jdbcTemplate = jdbcTemplate;
        this.retentionDays = retentionDays;
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupExpiredTickets() {
        String expiredSubquery = "SELECT ticket_id FROM ticket WHERE created_at < DATE_SUB(NOW(), INTERVAL ? DAY)";
        jdbcTemplate.update("DELETE FROM rating WHERE ticket_id IN (" + expiredSubquery + ")", retentionDays);
        jdbcTemplate.update("DELETE FROM ticket_action_log WHERE ticket_id IN (" + expiredSubquery + ")", retentionDays);
        jdbcTemplate.update("DELETE FROM ticket_status_history WHERE ticket_id IN (" + expiredSubquery + ")", retentionDays);
        jdbcTemplate.update("DELETE FROM ticket_classification WHERE ticket_id IN (" + expiredSubquery + ")", retentionDays);
        jdbcTemplate.update("DELETE FROM ticket WHERE created_at < DATE_SUB(NOW(), INTERVAL ? DAY)", retentionDays);
    }
}
