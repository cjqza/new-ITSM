package com.cenziang.itsmpojo.entity.base;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;

import java.time.LocalDateTime;

/**
 * 带租户和创建时间的持久化基类。
 *
 * @param <T> 子类自类型，保证链式 setter 返回子类
 */
public abstract class TenantCreatedEntity<T extends TenantCreatedEntity<T>> extends TenantScopedEntity<T> {
    /**
     * 创建时间。
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 获取创建时间。
     *
     * @return 创建时间
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 设置创建时间并返回子类自身。
     *
     * @param createdAt 创建时间
     * @return 子类自身
     */
    @SuppressWarnings("unchecked")
    public T setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return (T) this;
    }
}