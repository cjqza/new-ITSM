package com.cenziang.itsmpojo.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import java.time.LocalDateTime;

/**
 * 租户表实体。
 * <p>
 * 这个表保存企业租户本身，是所有工单、会话、字典和权限数据的根。
 * </p>
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("tenant")
public class TenantEntity {
    /**
     * 租户主键。
     */
    @TableId("tenant_id")
    private String tenantId;

    /**
     * 租户名称。
     */
    private String tenantName;

    /**
     * 是否启用。
     */
    private Boolean enabled;

    /**
     * 权限版本。
     */
    private String permissionsVersion;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
