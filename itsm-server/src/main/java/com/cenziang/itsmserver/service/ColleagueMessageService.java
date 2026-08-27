package com.cenziang.itsmserver.service;

import com.cenziang.itsmcommon.api.BusinessException;
import com.cenziang.itsmcommon.api.ErrorCode;
import com.cenziang.itsmpojo.dto.ColleagueMessageDtos;
import com.cenziang.itsmserver.application.RequestContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
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

    public ColleagueMessageService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
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
        return new ColleagueMessageDtos.MessageView(id, context.userId(), request.toUserId(), request.content(), LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public List<ColleagueMessageDtos.MessageView> list(RequestContext context, String peerUserId) {
        if (peerUserId == null || peerUserId.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "peerUserId is required");
        }
        return jdbcTemplate.query(
                """
                        SELECT id, from_user_id, to_user_id, content, created_at
                        FROM colleague_message
                        WHERE tenant_id = ?
                          AND ((from_user_id = ? AND to_user_id = ?) OR (from_user_id = ? AND to_user_id = ?))
                        ORDER BY id ASC
                        """,
                (rs, rowNum) -> new ColleagueMessageDtos.MessageView(
                        rs.getLong("id"),
                        rs.getString("from_user_id"),
                        rs.getString("to_user_id"),
                        rs.getString("content"),
                        toLocalDateTime(rs.getTimestamp("created_at"))
                ),
                context.tenantId(),
                context.userId(), peerUserId,
                peerUserId, context.userId()
        );
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
