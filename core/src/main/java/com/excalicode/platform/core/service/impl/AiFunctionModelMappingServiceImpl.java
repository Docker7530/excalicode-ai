package com.excalicode.platform.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.excalicode.platform.common.enums.AiFunctionType;
import com.excalicode.platform.core.config.CacheConfig;
import com.excalicode.platform.core.entity.AiFunctionModelMapping;
import com.excalicode.platform.core.entity.AiModel;
import com.excalicode.platform.core.entity.AiProvider;
import com.excalicode.platform.core.mapper.AiFunctionModelMappingMapper;
import com.excalicode.platform.core.service.AiFunctionModelMappingService;
import com.excalicode.platform.core.service.AiModelService;
import com.excalicode.platform.core.service.AiProviderService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.List;

/**
 * AI 功能-模型映射 Service 实现类
 */
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

        AiFunctionModelMapping mapping = this.getOne(new LambdaQueryWrapper<AiFunctionModelMapping>().eq(
                AiFunctionModelMapping::getFunctionType,
                functionType.getCode()));

        return mapping != null ? mapping.getModelId() : null;
    }

    @Override
    @CacheEvict(value = CacheConfig.AI_FUNCTION_CONFIGS_CACHE, allEntries = true)
    public boolean setFunctionModelMapping(AiFunctionType functionType, Long modelId) {
        if (functionType == null || modelId == null) {
            return false;
        }

        // 查找现有映射
        AiFunctionModelMapping existingMapping = this.getOne(new LambdaQueryWrapper<AiFunctionModelMapping>().eq(
                AiFunctionModelMapping::getFunctionType,
                functionType.getCode()));

        if (existingMapping != null) {
            // 更新现有映射
            existingMapping.setModelId(modelId);
            return this.updateById(existingMapping);
        } else {
            // 创建新映射
            AiFunctionModelMapping newMapping = new AiFunctionModelMapping();
            newMapping.setFunctionType(functionType.getCode());
            newMapping.setModelId(modelId);
            return this.save(newMapping);
        }
    }

    @Override
    public List<AiFunctionModelMapping> listAllMappingsWithModel() {
        List<AiFunctionModelMapping> mappings = this.list();

        // 填充模型和厂商信息
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
