package com.cenziang.itsmpojo.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cenziang.itsmpojo.entity.base.TenantCreatedUpdatedVersionEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 字典项实体。
 * <p>
 * 这个表保存业务线、管理单元、症状、原因、解决方法等可配置字典。
 * </p>
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("dictionary_item")
public class DictionaryItemEntity extends TenantCreatedUpdatedVersionEntity<DictionaryItemEntity> {
    /**
     * 字典项主键。
     */
    @TableId("item_id")
    private String itemId;

    /**
     * 字典类型。
     */
    private String dictType;

    /**
     * 业务编码。
     */
    private String code;

    /**
     * 显示名称。
     */
    private String name;

    /**
     * 字典说明。
     */
    private String description;

    /**
     * 父级字典项。
     */
    private String parentId;

    /**
     * 是否启用。
     */
    private Boolean enabled;

    /**
     * 排序号。
     */
    private Integer sortNo;

    /**
     * 停用原因。
     */
    private String disabledReason;

    /**
     * 停用时间。
     */
    private LocalDateTime disabledAt;

    /**
     * 停用人。
     */
    private String disabledBy;
}
