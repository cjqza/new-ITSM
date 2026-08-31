package com.cenziang.itsmpojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cenziang.itsmpojo.entity.base.TenantCreatedEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * 角色权限关联实体。
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("rbac_role_permission")
public class RbacRolePermissionEntity extends TenantCreatedEntity<RbacRolePermissionEntity> {
    /**
     * 角色主键。
     */
    @TableField("role_id")
    private String roleId;

    /**
     * 权限主键。
     */
    @TableField("permission_id")
    private String permissionId;
}