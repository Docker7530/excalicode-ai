package com.excalicode.platform.core.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.excalicode.platform.core.entity.PromptTemplate;
import com.excalicode.platform.core.mapper.PromptTemplateMapper;
import com.excalicode.platform.core.service.PromptService;
import com.excalicode.platform.core.service.PromptTemplateService;
import lombok.extern.slf4j.Slf4j;

/**
 * 提示词模板 Service 实现类
 */
@Slf4j
@Service
public class PromptTemplateServiceImpl extends ServiceImpl<PromptTemplateMapper, PromptTemplate>
        implements PromptTemplateService {

    @Autowired
    @Lazy
    private PromptService promptService;

    @Override
    @Cacheable(value = "prompts", key = "#code")
    public PromptTemplate getByCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }

        return this
                .getOne(new LambdaQueryWrapper<PromptTemplate>().eq(PromptTemplate::getCode, code));
    }

    @Override
    public List<PromptTemplate> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return this.list();
        }

        return this.list(
                new LambdaQueryWrapper<PromptTemplate>().like(PromptTemplate::getName, keyword).or()
                        .like(PromptTemplate::getDescription, keyword).or()
                        .like(PromptTemplate::getCode, keyword)
                        .orderByDesc(PromptTemplate::getUpdatedTime));
    }

    @Override
    @CacheEvict(value = "prompts", key = "#promptTemplate.code")
    public boolean saveOrUpdatePrompt(PromptTemplate promptTemplate) {
        if (promptTemplate == null) {
            return false;
        }

        // 如果有 ID，则更新；否则检查 code 是否已存在
        if (promptTemplate.getId() != null) {
            boolean updated = this.updateById(promptTemplate);
            if (updated) {
                // 清除 PromptService 的缓存
                evictPromptServiceCache(promptTemplate.getCode());
            }
            return updated;
        }

        // 检查 code 是否已存在
        PromptTemplate existing = this.getByCode(promptTemplate.getCode());
        if (existing != null) {
            // 更新现有记录
            promptTemplate.setId(existing.getId());
            boolean updated = this.updateById(promptTemplate);
            if (updated) {
                // 清除 PromptService 的缓存
                evictPromptServiceCache(promptTemplate.getCode());
            }
            return updated;
        } else {
            // 创建新记录
            return this.save(promptTemplate);
        }
    }

    @Override
    @CacheEvict(value = "prompts", allEntries = true)
    public boolean removeById(java.io.Serializable id) {
        // 查询 code 以便清除 PromptService 缓存
        PromptTemplate template = this.getById(id);
        boolean removed = super.removeById(id);

        if (removed && template != null) {
            // 清除 PromptService 的缓存
            evictPromptServiceCache(template.getCode());
        }

        return removed;
    }

    /**
     * 清除 PromptService 的缓存
     *
     * @param promptCode 提示词代码
     */
    private void evictPromptServiceCache(String promptCode) {
        try {
            if (promptService != null) {
                promptService.evictCacheByPromptCode(promptCode);
                log.info("已清除 PromptService 缓存: promptCode={}", promptCode);
            }
        } catch (Exception e) {
            log.error("清除 PromptService 缓存失败: promptCode={}", promptCode, e);
            // 不抛出异常，避免影响主流程
        }
    }
}
