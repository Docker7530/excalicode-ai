package com.excalicode.platform.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 需求扩写请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequirementEnhanceRequestDto {

    /**
     * 原始需求描述
     */
    @NotBlank(message = "需求描述不能为空")
    @Size(min = 5, max = 5000, message = "需求描述长度应在 5 到 5000 字符之间")
    private String originalRequirement;

}
