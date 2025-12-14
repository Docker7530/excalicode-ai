package com.excalicode.platform.core.service.entity.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.excalicode.platform.core.config.CacheConfig;
import com.excalicode.platform.core.entity.AiFunctionModelMapping;
import com.excalicode.platform.core.entity.AiModel;
import com.excalicode.platform.core.entity.AiProvider;
import com.excalicode.platform.core.enums.AiFunctionType;
import com.excalicode.platform.core.mapper.AiFunctionModelMappingMapper;
import com.excalicode.platform.core.service.entity.AiFunctionModelMappingService;
import com.excalicode.platform.core.service.entity.AiModelService;
import com.excalicode.platform.core.service.entity.AiProviderService;
import java.io.Serializable;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

/** AI 功能-模型映射 Service 实现类 */
@Service
@RequiredArgsConstructor
public class AiFunctionModelMappingServiceImpl
    extends ServiceImpl<AiFunctionModelMappingMapper, AiFunctionModelMapping>
    implements AiFunctionModelMappingService {

  private final AiModelService aiModelService;
  private final AiProviderService aiProviderService;

  @Override
  public Long getModelIdByFunctionType(AiFunctionType functionType) {
    if (functionType == null) {
      return null;
    }

    AiFunctionModelMapping mapping =
        this.getOne(
            new LambdaQueryWrapper<AiFunctionModelMapping>()
                .eq(AiFunctionModelMapping::getFunctionType, functionType.getCode()));

    return mapping != null ? mapping.getModelId() : null;
  }

  @Override
  @CacheEvict(value = CacheConfig.AI_FUNCTION_CONFIGS_CACHE, allEntries = true)
  public boolean setFunctionModelMapping(AiFunctionType functionType, Long modelId) {
    if (functionType == null || modelId == null) {
      return false;
    }

    AiFunctionModelMapping existingMapping =
        this.getOne(
            new LambdaQueryWrapper<AiFunctionModelMapping>()
                .eq(AiFunctionModelMapping::getFunctionType, functionType.getCode()));

    if (existingMapping != null) {
      existingMapping.setModelId(modelId);
      return this.updateById(existingMapping);
    } else {
      AiFunctionModelMapping newMapping = new AiFunctionModelMapping();
      newMapping.setFunctionType(functionType.getCode());
      newMapping.setModelId(modelId);
      return this.save(newMapping);
    }
  }

  @Override
  public List<AiFunctionModelMapping> listAllMappingsWithModel() {
    List<AiFunctionModelMapping> mappings = this.list();

    for (AiFunctionModelMapping mapping : mappings) {
      AiModel model = aiModelService.getById(mapping.getModelId());
      if (model != null) {
        AiProvider provider = aiProviderService.getById(model.getProviderId());
        model.setProvider(provider);
        mapping.setModel(model);
      }
    }

    return mappings;
  }

  @Override
  @CacheEvict(value = CacheConfig.AI_FUNCTION_CONFIGS_CACHE, allEntries = true)
  public boolean removeById(Serializable id) {
    return super.removeById(id);
  }
}
