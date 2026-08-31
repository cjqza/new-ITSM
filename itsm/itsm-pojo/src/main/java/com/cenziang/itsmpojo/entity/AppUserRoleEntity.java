package com.cenziang.itsmpojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cenziang.itsmpojo.entity.base.TenantCreatedEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * 用户与角色关联实体。
 * <p>
 * 这个表是 RBAC 关系的一部分，决定一个用户在当前租户下拥有哪些角色。
 * </p>
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("app_user_role")
public class AppUserRoleEntity extends TenantCreatedEntity<AppUserRoleEntity> {
    /**
     * 用户主键。
     */
    @TableField("user_id")
    private String userId;

    /**
     * 角色主键。
     */
    private String roleId;
}