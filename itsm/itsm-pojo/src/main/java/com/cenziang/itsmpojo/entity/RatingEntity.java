package com.cenziang.itsmpojo.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cenziang.itsmpojo.entity.base.TenantCreatedUpdatedEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * 工单评价实体。
 * <p>
 * 这个表保存用户满意度、标签和文字评价。
 * </p>
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("rating")
public class RatingEntity extends TenantCreatedUpdatedEntity<RatingEntity> {
    /**
     * 评价主键。
     */
    @TableId("rating_id")
    private String ratingId;

    /**
     * 工单主键。
     */
    private String ticketId;

    /**
     * 请求人。
     */
    private String requesterId;

    /**
     * 评分。
     */
    private Integer score;

    /**
     * 标签 JSON。
     */
    private String tagsJson;

    /**
     * 评价内容。
     */
    private String comment;
}
