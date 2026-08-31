package com.cenziang.itsmpojo.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cenziang.itsmpojo.entity.base.TenantCreatedEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Agent 决策实体。
 * <p>
 * 这个表保存 Agent 对会话的结构化判断、置信度和建议分流信息。
 * </p>
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("agent_decision")
public class AgentDecisionEntity extends TenantCreatedEntity<AgentDecisionEntity> {
    /**
     * 决策主键。
     */
    @TableId("decision_id")
    private String decisionId;

    /**
     * 所属会话。
     */
    private String sessionId;

    /**
     * 决策类型。
     */
    private String decision;

    /**
     * 置信度。
     */
    private java.math.BigDecimal confidence;

    /**
     * 业务线编码。
     */
    private String businessLineCode;

    /**
     * 决策摘要。
     */
    private String summary;

    /**
     * 转人工原因。
     */
    private String handoffReason;

    /**
     * 推荐管理单元。
     */
    private String suggestedManagementUnitId;

    /**
     * 推荐症状。
     */
    private String suggestedSymptomId;
}
