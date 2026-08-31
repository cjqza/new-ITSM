package com.cenziang.itsmserver.service;

import com.cenziang.itsmpojo.dto.ContactDtos;
import com.cenziang.itsmserver.application.RequestContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 企业联系人服务。
 * <p>
 * 联系人直接来源于本租户 app_user，因此公司内新注册用户会自动出现在联系人列表；
 * 按部门分组排序，同部门联系人相邻展示。
 * </p>
 */
@Service
public class ContactService {
    private final JdbcTemplate jdbcTemplate;

    public ContactService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public List<ContactDtos.ContactView> listContacts(RequestContext context, String keyword) {
        StringBuilder sql = new StringBuilder("""
                SELECT user_id, display_name, department_name, contact_email, contact_phone, enabled
                FROM app_user
                WHERE tenant_id = ? AND user_id != ?
                """);
        List<Object> args = new ArrayList<>();
        args.add(context.tenantId());
        args.add(context.userId());
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (display_name LIKE ? OR department_name LIKE ? OR contact_email LIKE ?)");
            String like = "%" + keyword + "%";
            args.add(like);
            args.add(like);
            args.add(like);
        }
        sql.append(" ORDER BY department_name IS NULL, department_name, display_name");
        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> new ContactDtos.ContactView(
                rs.getString("user_id"),
                rs.getString("user_id"),
                rs.getString("display_name"),
                rs.getString("department_name"),
                rs.getString("contact_email"),
                rs.getString("contact_phone"),
                rs.getBoolean("enabled")
        ), args.toArray());
    }
}
