package com.excalicode.platform.core.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * COSMIC 分析请求 承载用户确认后的功能过程列表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CosmicAnalysisRequestDto {

    /**
     * 功能过程列表
     */
    @NotEmpty(message = "功能过程列表不能为空")
    @Valid
    private List<FunctionalProcessDto> functionalProcesses;

}
