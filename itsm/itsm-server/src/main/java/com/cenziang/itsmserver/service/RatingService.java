package com.cenziang.itsmserver.service;

import com.cenziang.itsmcommon.api.BusinessException;
import com.cenziang.itsmcommon.api.ErrorCode;
import com.cenziang.itsmcommon.api.PageResponse;
import com.cenziang.itsmpojo.dto.RatingDtos;
import com.cenziang.itsmserver.application.RequestContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 工单评价统计服务。
 * <p>
 * 供数鲸看板查看各客服的工作评分，并按评分（1-5 星）筛选已评工单及其负责人。
 * </p>
 */
@Service
public class RatingService {
    private static final Set<String> WHALE_ROLES = Set.of("SUPPORT_ADMIN", "WHALE", "SUPERVISOR");

    private final JdbcTemplate jdbcTemplate;

    public RatingService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 各客服评分汇总：平均分、评价次数与 1-5 星各档数量。
     */
    @Transactional(readOnly = true)
    public List<RatingDtos.AgentRatingView> agentSummaries(RequestContext context) {
        requireWhale(context);
        return jdbcTemplate.query("""
                        SELECT COALESCE(t.assignee_id, t.resolved_by) AS agent_id,
                               u.display_name AS display_name,
                               u.department_name AS department_name,
                               COUNT(r.rating_id) AS rating_count,
                               ROUND(AVG(r.score), 2) AS avg_score,
                               SUM(r.score = 1) AS one_star,
                               SUM(r.score = 2) AS two_star,
                               SUM(r.score = 3) AS three_star,
                               SUM(r.score = 4) AS four_star,
                               SUM(r.score = 5) AS five_star
                        FROM rating r
                        JOIN ticket t ON t.tenant_id = r.tenant_id AND t.ticket_id = r.ticket_id
                        LEFT JOIN app_user u ON u.tenant_id = r.tenant_id AND u.user_id = COALESCE(t.assignee_id, t.resolved_by)
                        WHERE r.tenant_id = ?
                        GROUP BY agent_id, u.display_name, u.department_name
                        ORDER BY avg_score DESC, rating_count DESC
                        """,
                (rs, rowNum) -> new RatingDtos.AgentRatingView(
                        rs.getString("agent_id"),
                        rs.getString("display_name"),
                        rs.getString("department_name"),
                        rs.getDouble("avg_score"),
                        rs.getInt("rating_count"),
                        List.of(rs.getInt("one_star"), rs.getInt("two_star"), rs.getInt("three_star"),
                                rs.getInt("four_star"), rs.getInt("five_star"))),
                context.tenantId());
    }

    /**
     * 按评分筛选已评工单，返回工单号、标题、评分、负责人与评价内容。
     */
    @Transactional(readOnly = true)
    public PageResponse<RatingDtos.RatedTicketView> listRatedTickets(RequestContext context, Integer score, int page, int pageSize) {
        requireWhale(context);
        int p = Math.max(1, page);
        int size = Math.min(Math.max(1, pageSize), 100);
        int offset = (p - 1) * size;

        StringBuilder where = new StringBuilder(" WHERE r.tenant_id = ? ");
        List<Object> args = new ArrayList<>();
        args.add(context.tenantId());
        if (score != null) {
            where.append(" AND r.score = ? ");
            args.add(score);
        }

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM rating r JOIN ticket t ON t.tenant_id = r.tenant_id AND t.ticket_id = r.ticket_id" + where,
                Long.class,
                args.toArray());

        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(size);
        pageArgs.add(offset);
        List<RatingDtos.RatedTicketView> items = jdbcTemplate.query(
                """
                        SELECT t.ticket_id, t.ticket_no, t.title, t.status, r.score, r.comment, r.created_at AS rated_at,
                               COALESCE(t.assignee_id, t.resolved_by) AS agent_id,
                               u.display_name AS agent_display_name,
                               req.display_name AS requester_display_name
                        FROM rating r
                        JOIN ticket t ON t.tenant_id = r.tenant_id AND t.ticket_id = r.ticket_id
                        LEFT JOIN app_user u ON u.tenant_id = r.tenant_id AND u.user_id = COALESCE(t.assignee_id, t.resolved_by)
                        LEFT JOIN app_user req ON req.tenant_id = r.tenant_id AND req.user_id = r.requester_id
                        """ + where + " ORDER BY r.created_at DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> new RatingDtos.RatedTicketView(
                        rs.getString("ticket_id"),
                        rs.getString("ticket_no"),
                        rs.getString("title"),
                        rs.getString("status"),
                        rs.getInt("score"),
                        rs.getString("comment"),
                        rs.getString("agent_id"),
                        rs.getString("agent_display_name"),
                        rs.getString("requester_display_name"),
                        toLocalDateTime(rs.getTimestamp("rated_at"))),
                pageArgs.toArray());
        return PageResponse.of(items, p, size, total == null ? 0L : total);
    }

    private void requireWhale(RequestContext context) {
        boolean allowed = context.roles().stream().anyMatch(WHALE_ROLES::contains);
        if (!allowed) {
            throw new BusinessException(ErrorCode.ROLE_FORBIDDEN);
        }
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
