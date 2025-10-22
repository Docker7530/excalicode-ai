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

    /**
     * 生成版本标识 用于区分是V1还是V2方法生成的
     * <ul>
     * <li>v1: 一次性生成(稳定版本)</li>
     * <li>v2: 两阶段方法(Alpha版本)</li>
     * </ul>
     */
    @Builder.Default
    private String version = "v1";

}
