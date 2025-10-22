package com.excalicode.platform.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * COSMIC过程领域模型 表示COSMIC分析方法中的功能过程实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CosmicProcessDto {

    /**
     * 触发事件 描述启动该功能过程的触发条件或事件
     */
    @NotBlank(message = "触发事件不能为空")
    @JsonProperty(required = true)
    @JsonPropertyDescription("描述启动该功能过程的触发条件或事件")
    private String triggerEvent;

    /**
     * 功能过程名称 描述该功能过程的主要业务功能
     */
    @NotBlank(message = "功能过程名称不能为空")
    @JsonProperty(required = true)
    @JsonPropertyDescription("描述该功能过程的主要业务功能")
    private String functionalProcess;

    /**
     * 子过程描述 详细描述该功能过程下的具体操作步骤
     */
    @JsonProperty(required = true)
    @JsonPropertyDescription("详细描述该功能过程下的具体操作步骤")
    private String subProcessDesc;

    /**
     * 数据移动类型 COSMIC分析中的数据移动分类：E(输入)、R(读取)、W(写入)、X(输出)
     */
    @NotBlank(message = "数据移动类型不能为空")
    @JsonProperty(required = true)
    @JsonPropertyDescription("COSMIC分析中的数据移动分类：E(输入)、R(读取)、W(写入)、X(输出)")
    private String dataMovementType;

    /**
     * 数据组 该过程操作的数据组或数据实体
     */
    @NotBlank(message = "数据组不能为空")
    @JsonProperty(required = true)
    @JsonPropertyDescription("该过程操作的数据组或数据实体")
    private String dataGroup;

    /**
     * 数据属性 涉及的具体数据字段或属性列表
     */
    @JsonPropertyDescription("涉及的具体数据字段或属性列表")
    private String dataAttributes;

}
