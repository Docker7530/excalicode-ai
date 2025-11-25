package com.excalicode.platform.core.service.entity;

import com.baomidou.mybatisplus.extension.service.IService;
import com.excalicode.platform.core.entity.AiPromptTemplate;
import java.util.List;

/** 提示词模板 Service 接口 */
public interface AiPromptTemplateService extends IService<AiPromptTemplate> {

  /**
   * 根据提示词代码查询模板
   *
   * @param code 提示词代码
   * @return 提示词模板, 如果不存在则返回 null
   */
  AiPromptTemplate getByCode(String code);

  /**
   * 搜索提示词模板（根据名称或描述）
   *
   * @param keyword 搜索关键词
   * @return 匹配的提示词列表
   */
  List<AiPromptTemplate> search(String keyword);

  /**
   * 创建或更新提示词模板
   *
   * @param promptTemplate 提示词模板
   * @return 是否成功
   */
  boolean saveOrUpdatePrompt(AiPromptTemplate promptTemplate);
}
