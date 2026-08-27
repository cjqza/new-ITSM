package com.cenziang.itsmpojo.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cenziang.itsmpojo.entity.base.TenantCreatedUpdatedEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * 角色实体。
 * <p>
 * 这个表保存租户内的角色主档、启用状态和角色说明。
 * </p>
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("rbac_role")
public class RbacRoleEntity extends TenantCreatedUpdatedEntity<RbacRoleEntity> {
    /**
     * 角色主键。
     */
    @TableId("role_id")
    private String roleId;

    /**
     * 角色编码。
     */
    private String roleCode;

    /**
     * 角色名称。
     */
    private String roleName;

    /**
     * 是否启用。
     */
    private Boolean enabled;

    /**
     * 角色说明。
     */
    private String description;
}
