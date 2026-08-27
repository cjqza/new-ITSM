package com.cenziang.itsmserver.service;

import com.cenziang.itsmcommon.api.BusinessException;
import com.cenziang.itsmcommon.api.ErrorCode;
import com.cenziang.itsmpojo.dto.EmployeeDtos;
import com.cenziang.itsmserver.application.RequestContext;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 员工账号管理服务（仅管理员）。
 * <p>
 * 管理员可查看本租户员工，并修改员工账号的手机号、邮箱与部门。
 * </p>
 */
@Service
public class EmployeeService {
    private final JdbcTemplate jdbcTemplate;

    public EmployeeService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public List<EmployeeDtos.EmployeeView> listEmployees(RequestContext context) {
        requireAdmin(context);
        return jdbcTemplate.query(
                """
                        SELECT user_id, display_name, department_name, contact_phone, contact_email, enabled
                        FROM app_user
                        WHERE tenant_id = ?
                        ORDER BY department_name IS NULL, department_name, display_name
                        """,
                (rs, rowNum) -> new EmployeeDtos.EmployeeView(
                        rs.getString("user_id"),
                        rs.getString("display_name"),
                        rs.getString("department_name"),
                        rs.getString("contact_phone"),
                        rs.getString("contact_email"),
                        rs.getBoolean("enabled")
                ),
                context.tenantId()
        );
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
        return jdbcTemplate.queryForObject(
                """
                        SELECT user_id, display_name, department_name, contact_phone, contact_email, enabled
                        FROM app_user
                        WHERE tenant_id = ? AND user_id = ?
                        """,
                (rs, rowNum) -> new EmployeeDtos.EmployeeView(
                        rs.getString("user_id"),
                        rs.getString("display_name"),
                        rs.getString("department_name"),
                        rs.getString("contact_phone"),
                        rs.getString("contact_email"),
                        rs.getBoolean("enabled")
                ),
                context.tenantId(),
                userId
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
