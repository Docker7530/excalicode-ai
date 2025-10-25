package com.excalicode.platform.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * COSMIC 阶段1返回的子过程基础信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CosmicProcessStepBaseDto {

    /**
     * 子过程描述
     */
    @NotBlank(message = "子过程描述不能为空")
    @JsonProperty(required = true)
    @JsonPropertyDescription("详细描述该功能过程下的具体操作步骤")
    private String subProcessDesc;

    /**
     * 数据移动类型
     */
    @NotBlank(message = "数据移动类型不能为空")
    @JsonProperty(required = true)
    @JsonPropertyDescription("COSMIC分析中的数据移动分类：E(输入)、R(读取)、W(写入)、X(输出)")
    private String dataMovementType;
}
