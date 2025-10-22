package com.excalicode.platform.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * COSMIC 分析 AI 响应包装类
 */
@Data
public class CosmicProcessesResponse {

    @JsonProperty(required = true)
    private List<CosmicProcessDto> processes;

}
