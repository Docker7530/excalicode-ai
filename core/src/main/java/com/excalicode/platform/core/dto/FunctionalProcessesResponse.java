package com.excalicode.platform.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 功能过程拆解 AI 响应包装类
 */
@Data
public class FunctionalProcessesResponse {

    @JsonProperty(required = true)
    private List<FunctionalProcessDto> functionalProcesses;

}
