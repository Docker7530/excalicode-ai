package com.excalicode.platform.core.dto;

import com.excalicode.platform.core.entity.AiPromptTemplate;
import com.excalicode.platform.core.entity.AiProvider;
import lombok.Data;

import java.util.List;

/**
 * 功能配置聚合响应
 * 统一返回功能列表以及可用的模型、提示词资源，减少前端多次请求。
 */
@Data
public class FunctionConfigurationResponse {

    /**
     * 功能配置项列表。
     */
    private List<FunctionConfigurationItemDto> functions;

    /**
     * 可用的厂商及模型列表。
     */
    private List<AiProvider> providers;

    /**
     * 可用的提示词模板列表。
     */
    private List<AiPromptTemplate> promptTemplates;
}

