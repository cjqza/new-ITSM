package com.cenziang.itsmserver.service;

import com.cenziang.itsmserver.config.properties.ItsmAuthProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "itsm.auth.seed", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AuthSeedInitializer implements CommandLineRunner {
    private final JdbcTemplate jdbcTemplate;
    private final ItsmAuthProperties properties;
    private final BCryptPasswordEncoder passwordEncoder;
    private final UserAccountGenerator accountGenerator;

    public AuthSeedInitializer(JdbcTemplate jdbcTemplate, ItsmAuthProperties properties, UserAccountGenerator accountGenerator) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
        this.passwordEncoder = new BCryptPasswordEncoder(properties.getBcryptStrength());
        this.accountGenerator = accountGenerator;
    }

    @Override
    @Transactional
    public void run(String... args) {
        jdbcTemplate.update(
                """
                        INSERT INTO tenant (tenant_id, tenant_name, enabled, permissions_version)
                        VALUES (?, ?, 1, 'perm_20260825_01')
                        ON DUPLICATE KEY UPDATE tenant_name = VALUES(tenant_name), enabled = VALUES(enabled), permissions_version = VALUES(permissions_version)
                        """,
                properties.getSeed().getTenantId(),
                properties.getSeed().getTenantName()
        );

        String seedEmail = accountGenerator.emailPrefix(properties.getSeed().getDisplayName()) + "@cza.com";
        jdbcTemplate.update(
                """
                        INSERT INTO app_user (user_id, tenant_id, display_name, department_name, contact_phone, contact_email, enabled)
                        VALUES (?, ?, ?, ?, NULL, ?, 1)
                        ON DUPLICATE KEY UPDATE display_name = VALUES(display_name), department_name = VALUES(department_name), contact_email = VALUES(contact_email), enabled = VALUES(enabled)
                        """,
                properties.getSeed().getUserId(),
                properties.getSeed().getTenantId(),
                properties.getSeed().getDisplayName(),
                properties.getSeed().getDepartmentName(),
                seedEmail
        );

        jdbcTemplate.update(
                """
                        INSERT INTO user_credential (
                            credential_id, tenant_id, user_id, login_name, password_hash, password_algo,
                            password_version, auth_version, failed_count, locked_until, last_login_at, last_login_ip, status
                        ) VALUES (?, ?, ?, ?, ?, 'bcrypt', 1, 1, 0, NULL, NULL, NULL, 'ACTIVE')
                        ON DUPLICATE KEY UPDATE login_name = VALUES(login_name), password_hash = VALUES(password_hash), status = VALUES(status)
                        """,
                UUID.randomUUID().toString(),
                properties.getSeed().getTenantId(),
                properties.getSeed().getUserId(),
                properties.getSeed().getLoginName(),
                passwordEncoder.encode(properties.getSeed().getPassword())
        );

        jdbcTemplate.update(
                """
                        INSERT INTO rbac_role (role_id, tenant_id, role_code, role_name, enabled, description)
                        VALUES (?, ?, 'USER', '普通用户', 1, 'seed role')
                        ON DUPLICATE KEY UPDATE role_name = VALUES(role_name), enabled = VALUES(enabled)
                        """,
                UUID.randomUUID().toString(),
                properties.getSeed().getTenantId()
        );

        jdbcTemplate.update(
                """
                        INSERT INTO app_user_role (user_id, role_id, tenant_id)
                        SELECT ?, role_id, tenant_id
                        FROM rbac_role
                        WHERE tenant_id = ? AND role_code = 'USER'
                        ON DUPLICATE KEY UPDATE tenant_id = VALUES(tenant_id)
                        """,
                properties.getSeed().getUserId(),
                properties.getSeed().getTenantId()
        );

        Arrays.stream(properties.getSeed().getRoles())
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .filter(role -> !"USER".equalsIgnoreCase(role))
                .forEach(role -> jdbcTemplate.update(
                        """
                                INSERT INTO rbac_role (role_id, tenant_id, role_code, role_name, enabled, description)
                                VALUES (?, ?, ?, ?, 1, 'seed role')
                                ON DUPLICATE KEY UPDATE role_name = VALUES(role_name), enabled = VALUES(enabled)
                                """,
                        UUID.randomUUID().toString(),
                        properties.getSeed().getTenantId(),
                        role,
                        role
                ));

        seedSupportAgent();
        seedAdminAgent();
        seedDictionaries();
        seedDepartments();
        seedEmployees();
    }

    /**
     * 落一个客服种子账号，便于直接联调客服工单系统。
     */
    private void seedSupportAgent() {
        String tenantId = properties.getSeed().getTenantId();
        String email = accountGenerator.emailPrefix("客服一") + "@cza.com";
        jdbcTemplate.update(
                """
                        INSERT INTO app_user (user_id, tenant_id, display_name, department_name, contact_phone, contact_email, enabled)
                        VALUES ('000002', ?, '客服一', '一线支持组', NULL, ?, 1)
                        ON DUPLICATE KEY UPDATE display_name = VALUES(display_name), contact_email = VALUES(contact_email), enabled = VALUES(enabled)
                        """,
                tenantId,
                email
        );

        jdbcTemplate.update(
                """
                        INSERT INTO user_credential (
                            credential_id, tenant_id, user_id, login_name, password_hash, password_algo,
                            password_version, auth_version, failed_count, locked_until, last_login_at, last_login_ip, status
                        ) VALUES (?, ?, '000002', 'support01', ?, 'bcrypt', 1, 1, 0, NULL, NULL, NULL, 'ACTIVE')
                        ON DUPLICATE KEY UPDATE password_hash = VALUES(password_hash), status = VALUES(status)
                        """,
                UUID.randomUUID().toString(),
                tenantId,
                passwordEncoder.encode("P@ssw0rd123")
        );

        jdbcTemplate.update(
                """
                        INSERT INTO rbac_role (role_id, tenant_id, role_code, role_name, enabled, description)
                        VALUES (?, ?, 'SUPPORT_AGENT', '普通客服', 1, 'seed support role')
                        ON DUPLICATE KEY UPDATE role_name = VALUES(role_name), enabled = VALUES(enabled)
                        """,
                UUID.randomUUID().toString(),
                tenantId
        );

        jdbcTemplate.update(
                """
                        INSERT INTO app_user_role (user_id, role_id, tenant_id)
                        SELECT '000002', role_id, tenant_id
                        FROM rbac_role
                        WHERE tenant_id = ? AND role_code = 'SUPPORT_AGENT'
                        ON DUPLICATE KEY UPDATE tenant_id = VALUES(tenant_id)
                        """,
                tenantId
        );
    }

    /**
     * 落一个管理员种子账号，便于联调配置管理页。
     */
    private void seedAdminAgent() {
        String tenantId = properties.getSeed().getTenantId();
        String email = accountGenerator.emailPrefix("管理员") + "@cza.com";
        jdbcTemplate.update(
                """
                        INSERT INTO app_user (user_id, tenant_id, display_name, department_name, contact_phone, contact_email, enabled)
                        VALUES ('000001', ?, '管理员', '客服管理部', NULL, ?, 1)
                        ON DUPLICATE KEY UPDATE display_name = VALUES(display_name), contact_email = VALUES(contact_email), enabled = VALUES(enabled)
                        """,
                tenantId,
                email
        );

        jdbcTemplate.update(
                """
                        INSERT INTO user_credential (
                            credential_id, tenant_id, user_id, login_name, password_hash, password_algo,
                            password_version, auth_version, failed_count, locked_until, last_login_at, last_login_ip, status
                        ) VALUES (?, ?, '000001', 'admin01', ?, 'bcrypt', 1, 1, 0, NULL, NULL, NULL, 'ACTIVE')
                        ON DUPLICATE KEY UPDATE password_hash = VALUES(password_hash), status = VALUES(status)
                        """,
                UUID.randomUUID().toString(),
                tenantId,
                passwordEncoder.encode("P@ssw0rd123")
        );

        jdbcTemplate.update(
                """
                        INSERT INTO rbac_role (role_id, tenant_id, role_code, role_name, enabled, description)
                        VALUES (?, ?, 'SUPPORT_ADMIN', '管理员客服', 1, 'seed admin role')
                        ON DUPLICATE KEY UPDATE role_name = VALUES(role_name), enabled = VALUES(enabled)
                        """,
                UUID.randomUUID().toString(),
                tenantId
        );

        jdbcTemplate.update(
                """
                        INSERT INTO app_user_role (user_id, role_id, tenant_id)
                        SELECT '000001', role_id, tenant_id
                        FROM rbac_role
                        WHERE tenant_id = ? AND role_code = 'SUPPORT_ADMIN'
                        ON DUPLICATE KEY UPDATE tenant_id = VALUES(tenant_id)
                        """,
                tenantId
        );
    }

    /**
     * 落一组分类字典种子数据，便于客服联调「保存分类 / 提交解决」链路。
     */
    private void seedDictionaries() {
        String tenantId = properties.getSeed().getTenantId();
        seedDictItem(tenantId, "dict_unit_account", "MANAGEMENT_UNIT", "ACCOUNT", "账号", null, 10);
        seedDictItem(tenantId, "dict_unit_network", "MANAGEMENT_UNIT", "NETWORK", "网络", null, 20);
        seedDictItem(tenantId, "dict_unit_mail", "MANAGEMENT_UNIT", "MAIL", "邮件", null, 30);
        seedDictItem(tenantId, "dict_unit_device", "MANAGEMENT_UNIT", "DEVICE", "终端设备", null, 40);
        seedDictItem(tenantId, "dict_symptom_login_failed", "SYMPTOM", "LOGIN_FAILED", "登录失败", "dict_unit_account", 10);
        seedDictItem(tenantId, "dict_symptom_vpn_failed", "SYMPTOM", "VPN_FAILED", "VPN 连接失败", "dict_unit_network", 20);
        seedDictItem(tenantId, "dict_symptom_mail_rule", "SYMPTOM", "MAIL_RULE", "邮件提醒未收到", "dict_unit_mail", 30);
        seedDictItem(tenantId, "dict_symptom_printer", "SYMPTOM", "PRINTER", "打印异常", "dict_unit_device", 40);
        seedDictItem(tenantId, "dict_reason_permission", "REASON", "PERMISSION_MISSING", "权限缺失", "dict_symptom_login_failed", 10);
        seedDictItem(tenantId, "dict_reason_cert", "REASON", "CERT_FAILED", "证书失效", "dict_symptom_vpn_failed", 20);
        seedDictItem(tenantId, "dict_reason_filter", "REASON", "MAIL_FILTER", "过滤规则", "dict_symptom_mail_rule", 30);
        seedDictItem(tenantId, "dict_reason_driver", "REASON", "DRIVER_ERROR", "驱动异常", "dict_symptom_printer", 40);
        seedDictItem(tenantId, "dict_solution_reset_permission", "SOLUTION_METHOD", "RESET_PERMISSION", "补充账号权限", "dict_reason_permission", 10);
        seedDictItem(tenantId, "dict_solution_reinstall", "SOLUTION_METHOD", "REINSTALL", "重新安装客户端", "dict_reason_driver", 20);
        seedDictItem(tenantId, "dict_solution_refresh", "SOLUTION_METHOD", "REFRESH_CACHE", "刷新缓存", "dict_reason_filter", 30);
        seedDictItem(tenantId, "dict_solution_rebuild_rule", "SOLUTION_METHOD", "REBUILD_RULE", "重建提醒规则", "dict_reason_filter", 40);
    }

    private void seedDictItem(String tenantId, String itemId, String dictType, String code, String name, String parentId, int sortNo) {
        jdbcTemplate.update(
                """
                        INSERT INTO dictionary_item (item_id, tenant_id, dict_type, code, name, description, parent_id, enabled, sort_no, version)
                        VALUES (?, ?, ?, ?, ?, NULL, ?, 1, ?, 1)
                        ON DUPLICATE KEY UPDATE name = VALUES(name), parent_id = VALUES(parent_id), enabled = 1, sort_no = VALUES(sort_no)
                        """,
                itemId,
                tenantId,
                dictType,
                code,
                name,
                parentId,
                sortNo
        );
    }

    /**
     * 落一组部门种子数据，便于部门管理 CRUD 联调。
     */
    private void seedDepartments() {
        String tenantId = properties.getSeed().getTenantId();
        seedDepartment(tenantId, "dept_admin", "客服管理部", "客服团队管理部门");
        seedDepartment(tenantId, "dept_support", "一线支持组", "一线工单支持");
        seedDepartment(tenantId, "dept_tech_support", "技术支持部", "技术支持与二线处理");
        seedDepartment(tenantId, "dept_rd", "研发部", "产品研发");
        seedDepartment(tenantId, "dept_mkt", "市场部", "市场推广");
        seedDepartment(tenantId, "dept_hr", "人力资源部", "人事与行政");
        seedDepartment(tenantId, "dept_fin", "财务部", "财务核算");
        seedDepartment(tenantId, "dept_ops", "运维部", "系统运维");
    }

    private void seedDepartment(String tenantId, String departmentId, String name, String description) {
        jdbcTemplate.update(
                """
                        INSERT INTO department (department_id, tenant_id, name, description, enabled)
                        VALUES (?, ?, ?, ?, 1)
                        ON DUPLICATE KEY UPDATE name = VALUES(name), description = VALUES(description), enabled = 1
                        """,
                departmentId,
                tenantId,
                name,
                description
        );
    }

    /**
     * 补齐到 20 名员工：1 个管理员 + 5 个客服 + 14 个普通用户。
     * <p>
     * 已有账号：000001 管理员、000002 客服一、000003 张三；本方法再补 4 个客服与 13 个普通用户。
     * </p>
     */
    private void seedEmployees() {
        String tenantId = properties.getSeed().getTenantId();
        seedEmployee(tenantId, "000004", "客服二", "一线支持组", "SUPPORT_AGENT");
        seedEmployee(tenantId, "000005", "客服三", "一线支持组", "SUPPORT_AGENT");
        seedEmployee(tenantId, "000006", "客服四", "一线支持组", "SUPPORT_AGENT");
        seedEmployee(tenantId, "000007", "客服五", "一线支持组", "SUPPORT_AGENT");

        String[][] users = {
                {"000008", "刘伟", "研发部"},
                {"000009", "陈静", "市场部"},
                {"000010", "杨帆", "人力资源部"},
                {"000011", "黄磊", "财务部"},
                {"000012", "周涛", "运维部"},
                {"000013", "吴敏", "研发部"},
                {"000014", "徐强", "市场部"},
                {"000015", "孙丽", "人力资源部"},
                {"000016", "马超", "财务部"},
                {"000017", "朱琳", "运维部"},
                {"000018", "胡军", "研发部"},
                {"000019", "郭芳", "市场部"},
                {"000020", "何平", "人力资源部"}
        };
        for (String[] user : users) {
            seedEmployee(tenantId, user[0], user[1], user[2], "USER");
        }
    }

    private void seedEmployee(String tenantId, String userId, String displayName, String departmentName, String roleCode) {
        String email = accountGenerator.generateEmail(tenantId, displayName);
        String phone = String.format("138%08d", Integer.parseInt(userId));
        jdbcTemplate.update(
                """
                        INSERT INTO app_user (user_id, tenant_id, display_name, department_name, contact_phone, contact_email, enabled)
                        VALUES (?, ?, ?, ?, ?, ?, 1)
                        ON DUPLICATE KEY UPDATE display_name = VALUES(display_name), department_name = VALUES(department_name), contact_phone = VALUES(contact_phone), contact_email = VALUES(contact_email), enabled = VALUES(enabled)
                        """,
                userId,
                tenantId,
                displayName,
                departmentName,
                phone,
                email
        );

        jdbcTemplate.update(
                """
                        INSERT INTO user_credential (
                            credential_id, tenant_id, user_id, login_name, password_hash, password_algo,
                            password_version, auth_version, failed_count, locked_until, last_login_at, last_login_ip, status
                        ) VALUES (?, ?, ?, ?, ?, 'bcrypt', 1, 1, 0, NULL, NULL, NULL, 'ACTIVE')
                        ON DUPLICATE KEY UPDATE login_name = VALUES(login_name), password_hash = VALUES(password_hash), status = VALUES(status)
                        """,
                UUID.randomUUID().toString(),
                tenantId,
                userId,
                userId,
                passwordEncoder.encode("P@ssw0rd123")
        );

        jdbcTemplate.update(
                """
                        INSERT INTO app_user_role (user_id, role_id, tenant_id)
                        SELECT ?, role_id, tenant_id
                        FROM rbac_role
                        WHERE tenant_id = ? AND role_code = ?
                        ON DUPLICATE KEY UPDATE tenant_id = VALUES(tenant_id)
                        """,
                userId,
                tenantId,
                roleCode
        );
    }
}
