package com.cenziang.itsmpojo.entity.base;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;

import java.time.LocalDateTime;

/**
 * 带租户、创建时间和更新时间的持久化基类。
 *
 * @param <T> 子类自类型，保证链式 setter 返回子类
 */
public abstract class TenantCreatedUpdatedEntity<T extends TenantCreatedUpdatedEntity<T>> extends TenantScopedEntity<T> {
    /**
     * 创建时间。
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

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

    /**
     * 获取更新时间。
     *
     * @return 更新时间
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 设置更新时间并返回子类自身。
     *
     * @param updatedAt 更新时间
     * @return 子类自身
     */
    @SuppressWarnings("unchecked")
    public T setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return (T) this;
    }
}