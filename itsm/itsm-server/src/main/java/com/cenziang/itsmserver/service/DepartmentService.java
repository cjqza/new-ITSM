package com.cenziang.itsmserver.service;

import com.cenziang.itsmcommon.api.BusinessException;
import com.cenziang.itsmcommon.api.ErrorCode;
import com.cenziang.itsmpojo.dto.DepartmentDtos;
import com.cenziang.itsmserver.application.RequestContext;
import com.cenziang.itsmserver.infrastructure.audit.AuditService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 部门管理服务（仅管理员）。
 * <p>
 * 管理员可对部门做增删改查；部门名称在租户内唯一，员工表 department_name 以部门名称为关联值。
 * </p>
 */
@Service
public class DepartmentService {
    private final JdbcTemplate jdbcTemplate;
    private final AuditService auditService;

    public DepartmentService(JdbcTemplate jdbcTemplate, AuditService auditService) {
        this.jdbcTemplate = jdbcTemplate;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<DepartmentDtos.DepartmentView> list(RequestContext context) {
        requireAdmin(context);
        return jdbcTemplate.query(
                """
                        SELECT department_id, name, description, enabled
                        FROM department
                        WHERE tenant_id = ?
                        ORDER BY enabled DESC, name
                        """,
                (rs, rowNum) -> new DepartmentDtos.DepartmentView(
                        rs.getString("department_id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getBoolean("enabled")
                ),
                context.tenantId()
        );
    }

    @Transactional
    public DepartmentDtos.DepartmentView create(RequestContext context, DepartmentDtos.DepartmentCreateRequest request) {
        requireAdmin(context);
        String name = requireName(request.name());
        String departmentId = "dept_" + UUID.randomUUID().toString().replace("-", "");
        try {
            jdbcTemplate.update(
                    """
                            INSERT INTO department (department_id, tenant_id, name, description, enabled)
                            VALUES (?, ?, ?, ?, 1)
                            """,
                    departmentId,
                    context.tenantId(),
                    name,
                    blankToNull(request.description())
            );
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.RESOURCE_CONFLICT, "部门名称已存在");
        }
        auditService.recordAudit(context.tenantId(), context.userId(), "ADMIN", "DEPARTMENT_CREATE",
                "DEPARTMENT", departmentId, null, name);
        return jdbcTemplate.queryForObject(
                """
                        SELECT department_id, name, description, enabled
                        FROM department
                        WHERE tenant_id = ? AND department_id = ?
                        """,
                (rs, rowNum) -> new DepartmentDtos.DepartmentView(
                        rs.getString("department_id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getBoolean("enabled")
                ),
                context.tenantId(),
                departmentId
        );
    }

    @Transactional
    public DepartmentDtos.DepartmentView update(RequestContext context, String departmentId, DepartmentDtos.DepartmentUpdateRequest request) {
        requireAdmin(context);
        String oldName = jdbcTemplate.queryForObject(
                "SELECT name FROM department WHERE tenant_id = ? AND department_id = ?",
                String.class,
                context.tenantId(),
                departmentId
        );
        if (oldName == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "department not found");
        }
        String newName = request.name() == null || request.name().isBlank() ? oldName : request.name().trim();
        boolean enabled = request.enabled() == null ? true : request.enabled();

        try {
            int updated = jdbcTemplate.update(
                    """
                            UPDATE department
                            SET name = ?, description = ?, enabled = ?, updated_at = CURRENT_TIMESTAMP(3)
                            WHERE tenant_id = ? AND department_id = ?
                            """,
                    newName,
                    request.description() == null ? null : request.description().trim(),
                    enabled,
                    context.tenantId(),
                    departmentId
            );
            if (updated == 0) {
                throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "department not found");
            }
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.RESOURCE_CONFLICT, "部门名称已存在");
        }

        // 部门改名时同步员工表里的部门名称，保证关联一致
        if (!oldName.equals(newName)) {
            jdbcTemplate.update(
                    "UPDATE app_user SET department_name = ? WHERE tenant_id = ? AND department_name = ?",
                    newName,
                    context.tenantId(),
                    oldName
            );
        }

        auditService.recordAudit(context.tenantId(), context.userId(), "ADMIN", "DEPARTMENT_UPDATE",
                "DEPARTMENT", departmentId, null, oldName + " -> " + newName);

        return jdbcTemplate.queryForObject(
                """
                        SELECT department_id, name, description, enabled
                        FROM department
                        WHERE tenant_id = ? AND department_id = ?
                        """,
                (rs, rowNum) -> new DepartmentDtos.DepartmentView(
                        rs.getString("department_id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getBoolean("enabled")
                ),
                context.tenantId(),
                departmentId
        );
    }

    @Transactional
    public void delete(RequestContext context, String departmentId) {
        requireAdmin(context);
        String name = jdbcTemplate.queryForObject(
                "SELECT name FROM department WHERE tenant_id = ? AND department_id = ?",
                String.class,
                context.tenantId(),
                departmentId
        );
        if (name == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "department not found");
        }
        Integer employeeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM app_user WHERE tenant_id = ? AND department_name = ?",
                Integer.class,
                context.tenantId(),
                name
        );
        if (employeeCount != null && employeeCount > 0) {
            throw new BusinessException(ErrorCode.RESOURCE_CONFLICT, "该部门下仍有员工，请先调整员工部门");
        }
        jdbcTemplate.update(
                "DELETE FROM department WHERE tenant_id = ? AND department_id = ?",
                context.tenantId(),
                departmentId
        );
        auditService.recordAudit(context.tenantId(), context.userId(), "ADMIN", "DEPARTMENT_DELETE",
                "DEPARTMENT", departmentId, null, name);
    }

    private String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "部门名称不能为空");
        }
        return name.trim();
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
