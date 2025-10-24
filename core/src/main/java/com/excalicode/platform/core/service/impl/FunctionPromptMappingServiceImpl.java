package com.excalicode.platform.core.service.impl;

import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.excalicode.platform.core.config.CacheConfig;
import com.excalicode.platform.core.entity.FunctionPromptMapping;
import com.excalicode.platform.core.entity.PromptTemplate;
import com.excalicode.platform.core.mapper.FunctionPromptMappingMapper;
import com.excalicode.platform.core.service.FunctionPromptMappingService;
import com.excalicode.platform.core.service.PromptTemplateService;
import lombok.RequiredArgsConstructor;

/**
 * 功能-提示词映射 Service 实现类
 */
@Service
@RequiredArgsConstructor
public class FunctionPromptMappingServiceImpl
        extends ServiceImpl<FunctionPromptMappingMapper, FunctionPromptMapping>
        implements FunctionPromptMappingService {

    private final PromptTemplateService promptTemplateService;

    @Override
    public String getPromptCodeByFunctionCode(String functionCode) {
        if (functionCode == null || functionCode.trim().isEmpty()) {
            return null;
        }

        FunctionPromptMapping mapping = this.getOne(new LambdaQueryWrapper<FunctionPromptMapping>()
                .eq(FunctionPromptMapping::getFunctionCode, functionCode)
                .orderByDesc(FunctionPromptMapping::getPriority).last("LIMIT 1"));

        return mapping != null ? mapping.getPromptCode() : null;
    }

    @Override
    @CacheEvict(value = CacheConfig.AI_FUNCTION_CONFIGS_CACHE, allEntries = true)
    public boolean setFunctionPromptMapping(String functionCode, String promptCode,
            Integer priority) {
        if (functionCode == null || functionCode.trim().isEmpty() || promptCode == null
                || promptCode.trim().isEmpty()) {
            return false;
        }

        // 默认优先级为 0
        if (priority == null) {
            priority = 0;
        }

        // 查找现有映射
        FunctionPromptMapping existingMapping =
                this.getOne(new LambdaQueryWrapper<FunctionPromptMapping>()
                        .eq(FunctionPromptMapping::getFunctionCode, functionCode)
                        .eq(FunctionPromptMapping::getPromptCode, promptCode));

        if (existingMapping != null) {
            // 更新现有映射
            existingMapping.setPriority(priority);
            return this.updateById(existingMapping);
        } else {
            // 创建新映射
            FunctionPromptMapping newMapping = new FunctionPromptMapping();
            newMapping.setFunctionCode(functionCode);
            newMapping.setPromptCode(promptCode);
            newMapping.setPriority(priority);
            return this.save(newMapping);
        }
    }

    @Override
    public List<FunctionPromptMapping> listAllMappingsWithPrompt() {
        List<FunctionPromptMapping> mappings =
                this.list(new LambdaQueryWrapper<FunctionPromptMapping>()
                        .orderByAsc(FunctionPromptMapping::getFunctionCode)
                        .orderByDesc(FunctionPromptMapping::getPriority));

        // 填充提示词模板信息
        for (FunctionPromptMapping mapping : mappings) {
            PromptTemplate promptTemplate =
                    promptTemplateService.getByCode(mapping.getPromptCode());
            mapping.setPromptTemplate(promptTemplate);
        }

        return mappings;
    }

    @Override
    @CacheEvict(value = CacheConfig.AI_FUNCTION_CONFIGS_CACHE, allEntries = true)
    public boolean deleteFunctionPromptMapping(String functionCode, String promptCode) {
        if (functionCode == null || functionCode.trim().isEmpty() || promptCode == null
                || promptCode.trim().isEmpty()) {
            return false;
        }

        return this.remove(new LambdaQueryWrapper<FunctionPromptMapping>()
                .eq(FunctionPromptMapping::getFunctionCode, functionCode)
                .eq(FunctionPromptMapping::getPromptCode, promptCode));
    }
}
