package com.excalicode.platform.core.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 功能过程表导出请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessTableExportRequestDto {

    /**
     * COSMIC 过程列表
     */
    @NotEmpty(message = "COSMIC过程列表不能为空")
    @Valid
    private List<CosmicProcessDto> processes;

}
