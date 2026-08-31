package com.cenziang.itsmpojo.entity.base;

import com.baomidou.mybatisplus.annotation.Version;

/**
 * 带租户、时间戳和乐观锁版本号的持久化基类。
 *
 * @param <T> 子类自类型，保证链式 setter 返回子类
 */
public abstract class TenantCreatedUpdatedVersionEntity<T extends TenantCreatedUpdatedVersionEntity<T>> extends TenantCreatedUpdatedEntity<T> {
    /**
     * 乐观锁版本号。
     */
    @Version
    private Long version;

    /**
     * 获取乐观锁版本号。
     *
     * @return 版本号
     */
    public Long getVersion() {
        return version;
    }

    /**
     * 设置乐观锁版本号并返回子类自身。
     *
     * @param version 版本号
     * @return 子类自身
     */
    @SuppressWarnings("unchecked")
    public T setVersion(Long version) {
        this.version = version;
        return (T) this;
    }
}