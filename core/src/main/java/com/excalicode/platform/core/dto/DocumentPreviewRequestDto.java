package com.excalicode.platform.core.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 需求文档预览请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentPreviewRequestDto {

    /**
     * 需求名称
     */
    @NotBlank(message = "需求名称不能为空")
    private String requirementName;

    /**
     * COSMIC 过程列表
     */
    @NotEmpty(message = "COSMIC过程列表不能为空")
    @Valid
    private List<CosmicProcessDto> processes;

}
