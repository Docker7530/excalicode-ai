package com.excalicode.platform.core.dto;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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
     * 子过程步骤列表
     */
    @NotEmpty(message = "子过程列表不能为空")
    @Valid
    @JsonProperty(required = true)
    @JsonPropertyDescription("该功能过程对应的子过程步骤集合")
    private List<CosmicProcessStepDto> processSteps;

}
