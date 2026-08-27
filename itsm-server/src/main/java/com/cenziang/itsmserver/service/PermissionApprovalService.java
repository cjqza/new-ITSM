package com.cenziang.itsmserver.service;

import com.cenziang.itsmcommon.api.BusinessException;
import com.cenziang.itsmcommon.api.ErrorCode;
import com.cenziang.itsmpojo.dto.PermissionRequestDtos;
import com.cenziang.itsmserver.application.RequestContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 权限申请与审批服务。
 * <p>
 * 普通用户可申请 ITSM 权限或管理员权限，管理员在审批页查看并批准/驳回；
 * 批准后按申请类型授予 SUPPORT_AGENT（ITSM）或 SUPPORT_ADMIN（管理员）角色。
 * </p>
 */
@Service
public class PermissionApprovalService {
    private static final Set<String> REQUEST_TYPES = Set.of(
            PermissionRequestDtos.ITSM_ACCESS, PermissionRequestDtos.ADMIN);

    private final JdbcTemplate jdbcTemplate;

    public PermissionApprovalService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 提交权限申请。
     */
    @Transactional
    public PermissionRequestDtos.PermissionRequestResponse submit(RequestContext context,
                                                                  PermissionRequestDtos.SubmitPermissionRequest request) {
        if (request.requestType() == null || !REQUEST_TYPES.contains(request.requestType())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "invalid requestType");
        }
        String targetRole = targetRole(request.requestType());
        if (hasRole(context.tenantId(), context.userId(), targetRole)) {
            throw new BusinessException(ErrorCode.RESOURCE_CONFLICT, "已具备该权限，无需重复申请");
        }
        Integer pending = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM permission_request WHERE tenant_id = ? AND requester_id = ? AND request_type = ? AND status = 'PENDING'",
                Integer.class,
                context.tenantId(),
                context.userId(),
                request.requestType()
        );
        if (pending != null && pending > 0) {
            throw new BusinessException(ErrorCode.RESOURCE_CONFLICT, "存在待审批的同类申请");
        }

        String requestId = "prq_" + UUID.randomUUID().toString().replace("-", "");
        jdbcTemplate.update(
                "INSERT INTO permission_request (request_id, tenant_id, requester_id, request_type, status, reason) VALUES (?, ?, ?, ?, 'PENDING', ?)",
                requestId,
                context.tenantId(),
                context.userId(),
                request.requestType(),
                request.reason()
        );
        return new PermissionRequestDtos.PermissionRequestResponse(requestId, "PENDING");
    }

    /**
     * 管理员查询待审批列表。
     */
    @Transactional(readOnly = true)
    public List<PermissionRequestDtos.PermissionRequestView> listPending(RequestContext context) {
        requireAdmin(context);
        return jdbcTemplate.query(
                """
                        SELECT pr.request_id, pr.requester_id, u.display_name, pr.request_type, pr.status, pr.reason, pr.created_at
                        FROM permission_request pr
                        LEFT JOIN app_user u ON u.tenant_id = pr.tenant_id AND u.user_id = pr.requester_id
                        WHERE pr.tenant_id = ? AND pr.status = 'PENDING'
                        ORDER BY pr.created_at ASC
                        """,
                (rs, rowNum) -> new PermissionRequestDtos.PermissionRequestView(
                        rs.getString("request_id"),
                        rs.getString("requester_id"),
                        rs.getString("display_name"),
                        rs.getString("request_type"),
                        rs.getString("status"),
                        rs.getString("reason"),
                        toLocalDateTime(rs.getTimestamp("created_at"))
                ),
                context.tenantId()
        );
    }

    /**
     * 查询我的申请记录（用于前端展示当前申请状态）。
     */
    @Transactional(readOnly = true)
    public List<PermissionRequestDtos.PermissionRequestView> listMy(RequestContext context) {
        return jdbcTemplate.query(
                """
                        SELECT pr.request_id, pr.requester_id, u.display_name, pr.request_type, pr.status, pr.reason, pr.created_at
                        FROM permission_request pr
                        LEFT JOIN app_user u ON u.tenant_id = pr.tenant_id AND u.user_id = pr.requester_id
                        WHERE pr.tenant_id = ? AND pr.requester_id = ?
                        ORDER BY pr.created_at DESC
                        """,
                (rs, rowNum) -> new PermissionRequestDtos.PermissionRequestView(
                        rs.getString("request_id"),
                        rs.getString("requester_id"),
                        rs.getString("display_name"),
                        rs.getString("request_type"),
                        rs.getString("status"),
                        rs.getString("reason"),
                        toLocalDateTime(rs.getTimestamp("created_at"))
                ),
                context.tenantId(),
                context.userId()
        );
    }

    /**
     * 管理员批准申请并授予角色。
     */
    @Transactional
    public PermissionRequestDtos.PermissionRequestResponse approve(RequestContext context, String requestId) {
        requireAdmin(context);
        PendingRequest pending = requirePending(context.tenantId(), requestId);
        String targetRole = targetRole(pending.requestType());
        ensureRoleAndAssign(context.tenantId(), pending.requesterId(), targetRole);
        jdbcTemplate.update(
                "UPDATE permission_request SET status = 'APPROVED', approver_id = ?, decided_at = CURRENT_TIMESTAMP(3) WHERE request_id = ? AND tenant_id = ?",
                context.userId(),
                requestId,
                context.tenantId()
        );
        return new PermissionRequestDtos.PermissionRequestResponse(requestId, "APPROVED");
    }

    /**
     * 管理员驳回申请。
     */
    @Transactional
    public PermissionRequestDtos.PermissionRequestResponse reject(RequestContext context, String requestId) {
        requireAdmin(context);
        requirePending(context.tenantId(), requestId);
        jdbcTemplate.update(
                "UPDATE permission_request SET status = 'REJECTED', approver_id = ?, decided_at = CURRENT_TIMESTAMP(3) WHERE request_id = ? AND tenant_id = ?",
                context.userId(),
                requestId,
                context.tenantId()
        );
        return new PermissionRequestDtos.PermissionRequestResponse(requestId, "REJECTED");
    }

    private void requireAdmin(RequestContext context) {
        if (context.roles() == null || !context.roles().contains("SUPPORT_ADMIN")) {
            throw new BusinessException(ErrorCode.ROLE_FORBIDDEN);
        }
    }

    private String targetRole(String requestType) {
        return PermissionRequestDtos.ADMIN.equals(requestType) ? "SUPPORT_ADMIN" : "SUPPORT_AGENT";
    }

    private boolean hasRole(String tenantId, String userId, String roleCode) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM app_user_role ur
                        INNER JOIN rbac_role r ON r.role_id = ur.role_id AND r.tenant_id = ur.tenant_id
                        WHERE ur.tenant_id = ? AND ur.user_id = ? AND r.role_code = ? AND r.enabled = 1
                        """,
                Integer.class,
                tenantId,
                userId,
                roleCode
        );
        return count != null && count > 0;
    }

    private void ensureRoleAndAssign(String tenantId, String userId, String roleCode) {
        String roleId = jdbcTemplate.query(
                        "SELECT role_id FROM rbac_role WHERE tenant_id = ? AND role_code = ? LIMIT 1",
                        (rs, rowNum) -> rs.getString("role_id"),
                        tenantId,
                        roleCode
                ).stream()
                .findFirst()
                .orElseGet(() -> {
                    String id = UUID.randomUUID().toString();
                    jdbcTemplate.update(
                            "INSERT INTO rbac_role (role_id, tenant_id, role_code, role_name, enabled, description) VALUES (?, ?, ?, ?, 1, 'approved role')",
                            id,
                            tenantId,
                            roleCode,
                            roleCode
                    );
                    return id;
                });
        jdbcTemplate.update(
                "INSERT INTO app_user_role (user_id, role_id, tenant_id) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE tenant_id = VALUES(tenant_id)",
                userId,
                roleId,
                tenantId
        );
    }

    private PendingRequest requirePending(String tenantId, String requestId) {
        List<PendingRequest> rows = jdbcTemplate.query(
                "SELECT request_id, requester_id, request_type FROM permission_request WHERE tenant_id = ? AND request_id = ? AND status = 'PENDING' LIMIT 1",
                (rs, rowNum) -> new PendingRequest(
                        rs.getString("request_id"),
                        rs.getString("requester_id"),
                        rs.getString("request_type")
                ),
                tenantId,
                requestId
        );
        if (rows.isEmpty()) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "待审批申请不存在");
        }
        return rows.get(0);
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private record PendingRequest(String requestId, String requesterId, String requestType) {
    }
}
