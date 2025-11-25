package com.excalicode.platform.core.service.entity.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.excalicode.platform.core.config.CacheConfig;
import com.excalicode.platform.core.entity.AiPromptTemplate;
import com.excalicode.platform.core.mapper.AiPromptTemplateMapper;
import com.excalicode.platform.core.service.entity.AiPromptTemplateService;
import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/** 提示词模板 Service 实现类 */
@Service
public class AiPromptTemplateServiceImpl
    extends ServiceImpl<AiPromptTemplateMapper, AiPromptTemplate>
    implements AiPromptTemplateService {

  @Override
  @Cacheable(value = CacheConfig.PROMPTS_CACHE, key = "#code")
  public AiPromptTemplate getByCode(String code) {
    if (code == null || code.trim().isEmpty()) {
      return null;
    }

    return this.getOne(
        new LambdaQueryWrapper<AiPromptTemplate>().eq(AiPromptTemplate::getCode, code));
  }

  @Override
  public List<AiPromptTemplate> search(String keyword) {
    if (keyword == null || keyword.trim().isEmpty()) {
      return this.list();
    }

    return this.list(
        new LambdaQueryWrapper<AiPromptTemplate>()
            .like(AiPromptTemplate::getName, keyword)
            .or()
            .like(AiPromptTemplate::getCode, keyword)
            .orderByDesc(AiPromptTemplate::getUpdatedTime));
  }

  @Override
  @CacheEvict(
      value = {CacheConfig.PROMPTS_CACHE, CacheConfig.AI_FUNCTION_CONFIGS_CACHE},
      allEntries = true)
  public boolean saveOrUpdatePrompt(AiPromptTemplate promptTemplate) {
    if (promptTemplate == null) {
      return false;
    }

    if (promptTemplate.getId() != null) {
      return this.updateById(promptTemplate);
    }

    AiPromptTemplate existing = this.getByCode(promptTemplate.getCode());
    if (existing != null) {
      promptTemplate.setId(existing.getId());
      return this.updateById(promptTemplate);
    } else {
      return this.save(promptTemplate);
    }
  }

  @Override
  @CacheEvict(
      value = {CacheConfig.PROMPTS_CACHE, CacheConfig.AI_FUNCTION_CONFIGS_CACHE},
      allEntries = true)
  public boolean removeById(java.io.Serializable id) {
    return super.removeById(id);
  }
}
