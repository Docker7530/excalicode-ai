package com.excalicode.platform.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 功能过程，用于前端展示和编辑的功能过程数据结构
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FunctionalProcessDto {

    /**
     * 功能过程描述 简洁的功能过程描述，格式：主体 + 动作 + 客体
     */
    @JsonProperty(required = true)
    @JsonPropertyDescription("简洁的功能过程描述，格式：主体 + 动作 + 客体")
    private String description;

}
