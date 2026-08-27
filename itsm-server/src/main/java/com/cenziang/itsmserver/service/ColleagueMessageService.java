package com.cenziang.itsmserver.service;

import com.cenziang.itsmcommon.api.BusinessException;
import com.cenziang.itsmcommon.api.ErrorCode;
import com.cenziang.itsmpojo.dto.ColleagueMessageDtos;
import com.cenziang.itsmserver.application.RequestContext;
import com.cenziang.itsmserver.infrastructure.audit.AuditService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 同事消息服务。
 * <p>
 * 同事之间的一对一消息持久化到数据库，双方通过轮询读取。
 * </p>
 */
@Service
public class ColleagueMessageService {
    private final JdbcTemplate jdbcTemplate;
    private final AuditService auditService;

    public ColleagueMessageService(JdbcTemplate jdbcTemplate, AuditService auditService) {
        this.jdbcTemplate = jdbcTemplate;
        this.auditService = auditService;
    }

    @Transactional
    public ColleagueMessageDtos.MessageView send(RequestContext context, ColleagueMessageDtos.SendRequest request) {
        if (request.toUserId() == null || request.toUserId().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "toUserId is required");
        }
        if (request.content() == null || request.content().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "content is required");
        }
        if (request.toUserId().equals(context.userId())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "cannot send message to yourself");
        }
        jdbcTemplate.update(
                "INSERT INTO colleague_message (tenant_id, from_user_id, to_user_id, content) VALUES (?, ?, ?, ?)",
                context.tenantId(),
                context.userId(),
                request.toUserId(),
                request.content()
        );
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        auditService.recordAudit(context.tenantId(), context.userId(), "USER", "COLLEAGUE_MESSAGE_SEND",
                "COLLEAGUE_MESSAGE", String.valueOf(id), null, "to=" + request.toUserId());
        return new ColleagueMessageDtos.MessageView(id, context.userId(), request.toUserId(), request.content(), LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public ColleagueMessageDtos.MessagePage list(RequestContext context, String peerUserId, Long beforeId, Integer limit) {
        if (peerUserId == null || peerUserId.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "peerUserId is required");
        }
        int size = limit == null || limit < 1 ? 30 : Math.min(limit, 50);
        StringBuilder sql = new StringBuilder("""
                SELECT id, from_user_id, to_user_id, content, created_at
                FROM colleague_message
                WHERE tenant_id = ?
                  AND ((from_user_id = ? AND to_user_id = ?) OR (from_user_id = ? AND to_user_id = ?))
                """);
        List<Object> args = new ArrayList<>();
        args.add(context.tenantId());
        args.add(context.userId());
        args.add(peerUserId);
        args.add(peerUserId);
        args.add(context.userId());
        if (beforeId != null) {
            sql.append(" AND id < ?");
            args.add(beforeId);
        }
        // size 是经过校验的小整数（1..50），直接内联比用 LIMIT ? 更稳妥，规避 JDBC 驱动对 LIMIT 占位符的兼容差异
        sql.append(" ORDER BY id DESC LIMIT ").append(size + 1);
        List<ColleagueMessageDtos.MessageView> desc = jdbcTemplate.query(sql.toString(),
                (rs, rowNum) -> new ColleagueMessageDtos.MessageView(
                        rs.getLong("id"),
                        rs.getString("from_user_id"),
                        rs.getString("to_user_id"),
                        rs.getString("content"),
                        toLocalDateTime(rs.getTimestamp("created_at"))
                ),
                args.toArray());
        boolean hasMore = desc.size() > size;
        if (hasMore) {
            desc = desc.subList(0, size);
        }
        List<ColleagueMessageDtos.MessageView> asc = new ArrayList<>(desc);
        Collections.reverse(asc);
        return new ColleagueMessageDtos.MessagePage(asc, hasMore);
    }

    @Transactional(readOnly = true)
    public List<ColleagueMessageDtos.ConversationView> listConversations(RequestContext context) {
        return jdbcTemplate.query(
                """
                        SELECT
                            p.peer_user_id                                                                   AS user_id,
                            u.display_name                                                                   AS display_name,
                            u.department_name                                                                AS department_name,
                            lm.content                                                                       AS last_message,
                            lm.created_at                                                                    AS last_message_at,
                            COALESCE((SELECT COUNT(*)
                                      FROM colleague_message un
                                      WHERE un.tenant_id = p.tenant_id
                                        AND un.from_user_id = p.peer_user_id
                                        AND un.to_user_id = p.me_user_id
                                        AND un.read_at IS NULL), 0)                                         AS unread_count
                        FROM (
                            SELECT DISTINCT
                                ? AS tenant_id,
                                ? AS me_user_id,
                                CASE WHEN from_user_id = ? THEN to_user_id ELSE from_user_id END AS peer_user_id
                            FROM colleague_message
                            WHERE tenant_id = ?
                              AND (from_user_id = ? OR to_user_id = ?)
                        ) p
                        LEFT JOIN app_user u
                            ON u.tenant_id = p.tenant_id AND u.user_id = p.peer_user_id
                        LEFT JOIN colleague_message lm
                            ON lm.id = (
                                SELECT MAX(id)
                                FROM colleague_message x
                                WHERE x.tenant_id = p.tenant_id
                                  AND ((x.from_user_id = p.me_user_id AND x.to_user_id = p.peer_user_id)
                                       OR (x.from_user_id = p.peer_user_id AND x.to_user_id = p.me_user_id))
                            )
                        ORDER BY lm.created_at DESC
                        """,
                (rs, rowNum) -> new ColleagueMessageDtos.ConversationView(
                        rs.getString("user_id"),
                        rs.getString("display_name"),
                        rs.getString("department_name"),
                        rs.getString("last_message"),
                        toLocalDateTime(rs.getTimestamp("last_message_at")),
                        rs.getLong("unread_count")
                ),
                context.tenantId(),
                context.userId(),
                context.userId(),
                context.tenantId(),
                context.userId(),
                context.userId()
        );
    }

    @Transactional
    public void markRead(RequestContext context, String peerUserId) {
        if (peerUserId == null || peerUserId.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "peerUserId is required");
        }
        jdbcTemplate.update(
                """
                        UPDATE colleague_message
                        SET read_at = NOW(3)
                        WHERE tenant_id = ?
                          AND from_user_id = ?
                          AND to_user_id = ?
                          AND read_at IS NULL
                        """,
                context.tenantId(),
                peerUserId,
                context.userId()
        );
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
