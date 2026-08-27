package com.cenziang.itsmpojo.entity.base;

import com.baomidou.mybatisplus.annotation.TableField;

/**
 * 带租户标识的持久化基类。
 * <p>
 * 一期所有核心业务表都必须带 tenant_id，因此统一抽出到这个基类中。
 * </p>
 *
 * @param <T> 子类自类型，保证链式 setter 返回子类
 */
public abstract class TenantScopedEntity<T extends TenantScopedEntity<T>> {
    /**
     * 租户标识。
     */
    @TableField("tenant_id")
    private String tenantId;

    /**
     * 获取租户标识。
     *
     * @return 租户标识
     */
    public String getTenantId() {
        return tenantId;
    }

    /**
     * 设置租户标识并返回子类自身。
     *
     * @param tenantId 租户标识
     * @return 子类自身
     */
    @SuppressWarnings("unchecked")
    public T setTenantId(String tenantId) {
        this.tenantId = tenantId;
        return (T) this;
    }
}