package com.cenziang.itsmserver.service;

import com.cenziang.itsmcommon.api.BusinessException;
import com.cenziang.itsmcommon.api.ErrorCode;
import com.cenziang.itsmcommon.api.PageResponse;
import com.cenziang.itsmpojo.dto.EmployeeDtos;
import com.cenziang.itsmserver.application.RequestContext;
import com.cenziang.itsmserver.infrastructure.audit.AuditService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 员工账号管理服务（仅管理员）。
 * <p>
 * 管理员可分页、按关键字/部门/角色条件查询本租户员工，并修改员工的手机号、邮箱与部门。
 * </p>
 */
@Service
public class EmployeeService {
    private final JdbcTemplate jdbcTemplate;
    private final AuditService auditService;

    public EmployeeService(JdbcTemplate jdbcTemplate, AuditService auditService) {
        this.jdbcTemplate = jdbcTemplate;
        this.auditService = auditService;
    }

    private static final String SELECT_EMPLOYEE = """
            SELECT u.user_id, u.display_name, u.department_name, u.contact_phone, u.contact_email, u.enabled,
                   (SELECT GROUP_CONCAT(r2.role_code ORDER BY r2.role_code)
                    FROM app_user_role aur2
                    JOIN rbac_role r2 ON r2.role_id = aur2.role_id AND r2.tenant_id = aur2.tenant_id
                    WHERE aur2.user_id = u.user_id AND aur2.tenant_id = u.tenant_id) AS roles
            FROM app_user u
            """;

    @Transactional(readOnly = true)
    public PageResponse<EmployeeDtos.EmployeeView> listEmployees(RequestContext context,
                                                                 int page,
                                                                 int pageSize,
                                                                 String keyword,
                                                                 String departmentName,
                                                                 String role) {
        requireAdmin(context);
        int p = Math.max(1, page);
        int size = Math.min(Math.max(1, pageSize), 100);
        int offset = (p - 1) * size;

        StringBuilder where = new StringBuilder(" WHERE u.tenant_id = ? ");
        List<Object> filterArgs = new ArrayList<>();
        filterArgs.add(context.tenantId());

        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (u.display_name LIKE ? OR u.user_id LIKE ? OR u.contact_email LIKE ? OR u.contact_phone LIKE ?) ");
            String like = "%" + keyword.trim() + "%";
            filterArgs.add(like);
            filterArgs.add(like);
            filterArgs.add(like);
            filterArgs.add(like);
        }
        if (departmentName != null && !departmentName.isBlank()) {
            where.append(" AND u.department_name = ? ");
            filterArgs.add(departmentName.trim());
        }
        if (role != null && !role.isBlank()) {
            where.append("""
                     AND EXISTS (
                         SELECT 1 FROM app_user_role aur
                         JOIN rbac_role r ON r.role_id = aur.role_id AND r.tenant_id = aur.tenant_id
                         WHERE aur.user_id = u.user_id AND aur.tenant_id = u.tenant_id AND r.role_code = ?
                     )
                    """);
            filterArgs.add(role.trim());
        }

        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM app_user u" + where, Long.class, filterArgs.toArray());

        List<Object> pageArgs = new ArrayList<>(filterArgs);
        pageArgs.add(size);
        pageArgs.add(offset);
        List<EmployeeDtos.EmployeeView> items = jdbcTemplate.query(
                SELECT_EMPLOYEE + where + " ORDER BY u.department_name IS NULL, u.department_name, u.user_id LIMIT ? OFFSET ?",
                this::mapEmployee,
                pageArgs.toArray()
        );

        return PageResponse.of(items, p, size, total == null ? 0L : total);
    }

    @Transactional
    public EmployeeDtos.EmployeeView update(RequestContext context, String userId, EmployeeDtos.EmployeeUpdateRequest request) {
        requireAdmin(context);
        int updated;
        try {
            updated = jdbcTemplate.update(
                    """
                            UPDATE app_user
                            SET department_name = ?, contact_phone = ?, contact_email = ?, updated_at = CURRENT_TIMESTAMP(3)
                            WHERE tenant_id = ? AND user_id = ?
                            """,
                    blankToNull(request.departmentName()),
                    blankToNull(request.phone()),
                    blankToNull(request.email()),
                    context.tenantId(),
                    userId
            );
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.RESOURCE_CONFLICT, "手机号已被其他员工占用");
        }
        if (updated == 0) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "employee not found");
        }
        auditService.recordAudit(context.tenantId(), context.userId(), "ADMIN", "EMPLOYEE_UPDATE",
                "APP_USER", userId, null, "department=" + request.departmentName());
        return jdbcTemplate.queryForObject(
                SELECT_EMPLOYEE + " WHERE u.tenant_id = ? AND u.user_id = ?",
                this::mapEmployee,
                context.tenantId(),
                userId
        );
    }

    private EmployeeDtos.EmployeeView mapEmployee(ResultSet rs, int rowNum) throws SQLException {
        String roles = rs.getString("roles");
        List<String> roleList = (roles == null || roles.isBlank())
                ? List.of()
                : Arrays.stream(roles.split(","))
                        .map(String::trim)
                        .filter(role -> !role.isEmpty())
                        .toList();
        return new EmployeeDtos.EmployeeView(
                rs.getString("user_id"),
                rs.getString("display_name"),
                rs.getString("department_name"),
                rs.getString("contact_phone"),
                rs.getString("contact_email"),
                rs.getBoolean("enabled"),
                roleList
        );
    }

    private void requireAdmin(RequestContext context) {
        if (context.roles() == null || !context.roles().contains("SUPPORT_ADMIN")) {
            throw new BusinessException(ErrorCode.ROLE_FORBIDDEN);
        }
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
