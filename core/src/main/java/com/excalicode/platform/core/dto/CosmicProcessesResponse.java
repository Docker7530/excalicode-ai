package com.excalicode.platform.core.dto;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import lombok.Data;

/**
 * COSMIC 分析 AI 响应包装类
 */
@Data
public class CosmicProcessesResponse {

    @JsonProperty(required = true)
    @Valid
    private List<CosmicProcessDto> processes;

}
