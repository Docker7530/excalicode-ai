package com.excalicode.platform.core.service.entity.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.excalicode.platform.core.config.CacheConfig;
import com.excalicode.platform.core.entity.AiFunctionPromptMapping;
import com.excalicode.platform.core.entity.AiPromptTemplate;
import com.excalicode.platform.core.mapper.AiFunctionPromptMappingMapper;
import com.excalicode.platform.core.service.entity.AiFunctionPromptMappingService;
import com.excalicode.platform.core.service.entity.AiPromptTemplateService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

/** 功能-提示词映射 Service 实现类 */
@Service
@RequiredArgsConstructor
public class AiFunctionPromptMappingServiceImpl
    extends ServiceImpl<AiFunctionPromptMappingMapper, AiFunctionPromptMapping>
    implements AiFunctionPromptMappingService {

  private final AiPromptTemplateService aiPromptTemplateService;

  @Override
  public String getPromptCodeByFunctionCode(String functionCode) {
    if (functionCode == null || functionCode.trim().isEmpty()) {
      return null;
    }

    AiFunctionPromptMapping mapping =
        this.getOne(
            new LambdaQueryWrapper<AiFunctionPromptMapping>()
                .eq(AiFunctionPromptMapping::getFunctionCode, functionCode));

    return mapping != null ? mapping.getPromptCode() : null;
  }

  @Override
  @CacheEvict(value = CacheConfig.AI_FUNCTION_CONFIGS_CACHE, allEntries = true)
  public boolean setFunctionPromptMapping(String functionCode, String promptCode) {
    if (functionCode == null
        || functionCode.trim().isEmpty()
        || promptCode == null
        || promptCode.trim().isEmpty()) {
      return false;
    }

    AiFunctionPromptMapping existingMapping =
        this.getOne(
            new LambdaQueryWrapper<AiFunctionPromptMapping>()
                .eq(AiFunctionPromptMapping::getFunctionCode, functionCode));

    if (existingMapping != null) {
      existingMapping.setPromptCode(promptCode);
      return this.updateById(existingMapping);
    }

    AiFunctionPromptMapping newMapping = new AiFunctionPromptMapping();
    newMapping.setFunctionCode(functionCode);
    newMapping.setPromptCode(promptCode);
    return this.save(newMapping);
  }

  @Override
  public List<AiFunctionPromptMapping> listAllMappingsWithPrompt() {
    List<AiFunctionPromptMapping> mappings =
        this.list(
            new LambdaQueryWrapper<AiFunctionPromptMapping>()
                .orderByAsc(AiFunctionPromptMapping::getFunctionCode));

    // 填充提示词模板信息
    for (AiFunctionPromptMapping mapping : mappings) {
      AiPromptTemplate promptTemplate = aiPromptTemplateService.getByCode(mapping.getPromptCode());
      mapping.setPromptTemplate(promptTemplate);
    }

    return mappings;
  }

  @Override
  @CacheEvict(value = CacheConfig.AI_FUNCTION_CONFIGS_CACHE, allEntries = true)
  public boolean deleteFunctionPromptMapping(String functionCode, String promptCode) {
    if (functionCode == null
        || functionCode.trim().isEmpty()
        || promptCode == null
        || promptCode.trim().isEmpty()) {
      return false;
    }

    return this.remove(
        new LambdaQueryWrapper<AiFunctionPromptMapping>()
            .eq(AiFunctionPromptMapping::getFunctionCode, functionCode)
            .eq(AiFunctionPromptMapping::getPromptCode, promptCode));
  }
}
