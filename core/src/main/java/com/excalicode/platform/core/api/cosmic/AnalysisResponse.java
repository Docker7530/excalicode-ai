package com.excalicode.platform.core.api.cosmic;

import com.excalicode.platform.core.model.cosmic.CosmicProcess;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 用于封装 COSMIC AI 分析的结果数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResponse {

    /**
     * COSMIC 过程列表 分析生成的所有 COSMIC 功能过程
     */
    private List<CosmicProcess> processes;

}
