package com.excalicode.platform.core.dto;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用于封装 COSMIC AI 分析的结果数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResultDto {

    /**
     * COSMIC 过程列表 分析生成的所有 COSMIC 功能过程
     */
    @NotEmpty(message = "COSMIC过程列表不能为空")
    @Valid
    private List<CosmicProcessDto> processes;

}
