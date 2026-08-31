package com.cenziang.itsmpojo.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cenziang.itsmpojo.entity.base.TenantCreatedUpdatedEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * 权限实体。
 * <p>
 * 这个表保存菜单、按钮和其他 RBAC 权限点。
 * </p>
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("rbac_permission")
public class RbacPermissionEntity extends TenantCreatedUpdatedEntity<RbacPermissionEntity> {
    /**
     * 权限主键。
     */
    @TableId("permission_id")
    private String permissionId;

    /**
     * 权限编码。
     */
    private String permissionCode;

    /**
     * 权限名称。
     */
    private String permissionName;

    /**
     * 权限类型。
     */
    private String permissionType;

    /**
     * 是否启用。
     */
    private Boolean enabled;
}
