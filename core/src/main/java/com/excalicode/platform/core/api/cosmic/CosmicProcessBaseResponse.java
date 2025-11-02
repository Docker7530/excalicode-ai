package com.excalicode.platform.core.api.cosmic;

import com.excalicode.platform.core.model.cosmic.CosmicProcess;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import lombok.Data;

import java.util.List;

/**
 * COSMIC 阶段1分析 AI 响应包装类
 */
@Data
public class CosmicProcessBaseResponse {

    @JsonProperty(required = true)
    @Valid
    private List<CosmicProcess> processes;

}
