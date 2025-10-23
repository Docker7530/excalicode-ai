package com.excalicode.platform.web.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.excalicode.platform.common.enums.AiFunctionType;
import com.excalicode.platform.core.dto.FunctionConfigurationItemDto;
import com.excalicode.platform.core.dto.FunctionConfigurationResponse;
import com.excalicode.platform.core.entity.AiFunctionModelMapping;
import com.excalicode.platform.core.entity.FunctionPromptMapping;
import com.excalicode.platform.core.service.AiFunctionModelMappingService;
import com.excalicode.platform.core.service.AiProviderService;
import com.excalicode.platform.core.service.FunctionPromptMappingService;
import com.excalicode.platform.core.service.PromptTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 功能配置聚合接口
 *
 * 将功能类型、模型映射、提示词映射与基础资源整合到一次返回，方便前端以功能为中心进行配置。
 */
@Slf4j
@RestController
@RequestMapping("/api/function-configuration")
@RequiredArgsConstructor
public class FunctionConfigurationController {

    private final AiFunctionModelMappingService aiFunctionModelMappingService;
    private final FunctionPromptMappingService functionPromptMappingService;
    private final AiProviderService aiProviderService;
    private final PromptTemplateService promptTemplateService;

    /**
     * 返回功能配置聚合信息。
     */
    @GetMapping
    public ResponseEntity<FunctionConfigurationResponse> list() {
        log.info("查询功能配置聚合数据");

        Map<String, AiFunctionModelMapping> modelMappingMap = buildModelMappingMap();
        Map<String, FunctionPromptMapping> promptMappingMap = buildPromptMappingMap();
        List<FunctionConfigurationItemDto> functions =
                buildFunctionConfigurationItems(modelMappingMap, promptMappingMap);

        FunctionConfigurationResponse response = new FunctionConfigurationResponse();
        response.setFunctions(functions);
        response.setProviders(aiProviderService.listProvidersWithModels());
        response.setPromptTemplates(promptTemplateService.list());

        return ResponseEntity.ok(response);
    }

    private Map<String, AiFunctionModelMapping> buildModelMappingMap() {
        List<AiFunctionModelMapping> modelMappings =
                aiFunctionModelMappingService.listAllMappingsWithModel();
        Map<String, AiFunctionModelMapping> result = new LinkedHashMap<>();
        for (AiFunctionModelMapping mapping : modelMappings) {
            result.put(mapping.getFunctionType(), mapping);
        }
        return result;
    }

    private Map<String, FunctionPromptMapping> buildPromptMappingMap() {
        List<FunctionPromptMapping> promptMappings =
                functionPromptMappingService.listAllMappingsWithPrompt();
        Map<String, FunctionPromptMapping> result = new LinkedHashMap<>();
        for (FunctionPromptMapping mapping : promptMappings) {
            // 列表按优先级倒序返回，保留第一个即可代表当前配置
            result.putIfAbsent(mapping.getFunctionCode(), mapping);
        }
        return result;
    }

    private List<FunctionConfigurationItemDto> buildFunctionConfigurationItems(
            Map<String, AiFunctionModelMapping> modelMappingMap,
            Map<String, FunctionPromptMapping> promptMappingMap) {
        List<FunctionConfigurationItemDto> items = new ArrayList<>();
        Arrays.stream(AiFunctionType.values()).forEach(type -> {
            FunctionConfigurationItemDto item = new FunctionConfigurationItemDto();
            item.setFunctionCode(type.getCode());
            item.setFunctionDescription(type.getDescription());
            item.setModelMapping(modelMappingMap.get(type.getCode()));
            item.setPromptMapping(promptMappingMap.get(type.getCode()));
            items.add(item);
        });
        return items;
    }
}

