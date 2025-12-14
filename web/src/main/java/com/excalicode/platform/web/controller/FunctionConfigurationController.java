package com.excalicode.platform.web.controller;

import com.excalicode.platform.core.api.ai.FunctionConfigurationItem;
import com.excalicode.platform.core.api.ai.FunctionConfigurationResponse;
import com.excalicode.platform.core.entity.AiFunctionModelMapping;
import com.excalicode.platform.core.entity.AiFunctionPromptMapping;
import com.excalicode.platform.core.enums.AiFunctionType;
import com.excalicode.platform.core.service.entity.AiFunctionModelMappingService;
import com.excalicode.platform.core.service.entity.AiFunctionPromptMappingService;
import com.excalicode.platform.core.service.entity.AiPromptTemplateService;
import com.excalicode.platform.core.service.entity.AiProviderService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 功能配置聚合接口 将功能类型、模型映射、提示词映射与基础资源整合到一次返回，方便前端以功能为中心进行配置。 */
@Slf4j
@RestController
@RequestMapping("/api/function-configuration")
@RequiredArgsConstructor
public class FunctionConfigurationController {

  private final AiFunctionModelMappingService aiFunctionModelMappingService;
  private final AiFunctionPromptMappingService aiFunctionPromptMappingService;
  private final AiProviderService aiProviderService;
  private final AiPromptTemplateService aiPromptTemplateService;

  /** 返回功能配置聚合信息。 */
  @GetMapping
  public ResponseEntity<FunctionConfigurationResponse> list() {
    log.info("查询功能配置聚合数据");

    Map<String, AiFunctionModelMapping> modelMappingMap = buildModelMappingMap();
    Map<String, AiFunctionPromptMapping> promptMappingMap = buildPromptMappingMap();
    List<FunctionConfigurationItem> functions =
        buildFunctionConfigurationItems(modelMappingMap, promptMappingMap);

    FunctionConfigurationResponse response = new FunctionConfigurationResponse();
    response.setFunctions(functions);
    response.setProviders(aiProviderService.listProvidersWithModels());
    response.setPromptTemplates(aiPromptTemplateService.list());

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

  private Map<String, AiFunctionPromptMapping> buildPromptMappingMap() {
    List<AiFunctionPromptMapping> promptMappings =
        aiFunctionPromptMappingService.listAllMappingsWithPrompt();
    Map<String, AiFunctionPromptMapping> result = new LinkedHashMap<>();
    for (AiFunctionPromptMapping mapping : promptMappings) {
      result.put(mapping.getFunctionCode(), mapping);
    }
    return result;
  }

  private List<FunctionConfigurationItem> buildFunctionConfigurationItems(
      Map<String, AiFunctionModelMapping> modelMappingMap,
      Map<String, AiFunctionPromptMapping> promptMappingMap) {
    List<FunctionConfigurationItem> items = new ArrayList<>();
    Arrays.stream(AiFunctionType.values())
        .forEach(
            type -> {
              FunctionConfigurationItem item = new FunctionConfigurationItem();
              item.setFunctionCode(type.getCode());
              item.setFunctionDescription(type.getDescription());
              item.setModelMapping(modelMappingMap.get(type.getCode()));
              item.setPromptMapping(promptMappingMap.get(type.getCode()));
              items.add(item);
            });
    return items;
  }
}
