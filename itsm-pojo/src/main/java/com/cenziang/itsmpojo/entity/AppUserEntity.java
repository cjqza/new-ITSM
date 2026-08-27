package com.cenziang.itsmpojo.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cenziang.itsmpojo.entity.base.TenantCreatedUpdatedEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * 用户主数据实体。
 * <p>
 * 这个表保存姓名、部门、联系方式等身份主档信息，不保存登录凭证。
 * </p>
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("app_user")
public class AppUserEntity extends TenantCreatedUpdatedEntity<AppUserEntity> {
    /**
     * 用户主键。
     */
    @TableId("user_id")
    private String userId;

    /**
     * 展示名称。
     */
    private String displayName;

    /**
     * 部门名称。
     */
    private String departmentName;

    /**
     * 联系电话。
     */
    private String contactPhone;

    /**
     * 联系邮箱。
     */
    private String contactEmail;

    /**
     * 是否启用。
     */
    private Boolean enabled;
}
