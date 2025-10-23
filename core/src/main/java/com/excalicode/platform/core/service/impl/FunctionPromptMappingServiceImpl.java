package com.excalicode.platform.core.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.excalicode.platform.core.entity.FunctionPromptMapping;
import com.excalicode.platform.core.entity.PromptTemplate;
import com.excalicode.platform.core.mapper.FunctionPromptMappingMapper;
import com.excalicode.platform.core.service.FunctionPromptMappingService;
import com.excalicode.platform.core.service.PromptService;
import com.excalicode.platform.core.service.PromptTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 功能-提示词映射 Service 实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FunctionPromptMappingServiceImpl
        extends ServiceImpl<FunctionPromptMappingMapper, FunctionPromptMapping>
        implements FunctionPromptMappingService {

    private final PromptTemplateService promptTemplateService;

    @Autowired
    @Lazy
    private PromptService promptService;

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

        boolean result;
        if (existingMapping != null) {
            // 更新现有映射
            existingMapping.setPriority(priority);
            result = this.updateById(existingMapping);
        } else {
            // 创建新映射
            FunctionPromptMapping newMapping = new FunctionPromptMapping();
            newMapping.setFunctionCode(functionCode);
            newMapping.setPromptCode(promptCode);
            newMapping.setPriority(priority);
            result = this.save(newMapping);
        }

        // 清除 PromptService 缓存
        if (result) {
            evictPromptServiceCache(functionCode);
        }

        return result;
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
    public boolean deleteFunctionPromptMapping(String functionCode, String promptCode) {
        if (functionCode == null || functionCode.trim().isEmpty() || promptCode == null
                || promptCode.trim().isEmpty()) {
            return false;
        }

        boolean result = this.remove(new LambdaQueryWrapper<FunctionPromptMapping>()
                .eq(FunctionPromptMapping::getFunctionCode, functionCode)
                .eq(FunctionPromptMapping::getPromptCode, promptCode));

        // 清除 PromptService 缓存
        if (result) {
            evictPromptServiceCache(functionCode);
        }

        return result;
    }

    /**
     * 清除 PromptService 的缓存
     *
     * @param functionCode 功能代码
     */
    private void evictPromptServiceCache(String functionCode) {
        try {
            if (promptService != null) {
                promptService.evictCache(functionCode);
                log.info("已清除 PromptService 缓存: functionCode={}", functionCode);
            }
        } catch (Exception e) {
            log.error("清除 PromptService 缓存失败: functionCode={}", functionCode, e);
            // 不抛出异常，避免影响主流程
        }
    }
}
