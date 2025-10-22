package com.excalicode.platform.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 数据组和数据属性 AI 响应包装类(阶段2)
 */
@Data
public class DataGroupAttributeResponse {

    @JsonProperty(required = true)
    private String dataGroup;

    @JsonProperty(required = true)
    private String dataAttributes;

}
