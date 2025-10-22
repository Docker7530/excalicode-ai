package com.excalicode.platform.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 重复项修复 AI 响应包装类
 */
@Data
public class FixDuplicateResponse {

    @JsonProperty(required = true)
    private String fixed;

}
