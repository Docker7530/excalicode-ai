package com.excalicode.platform.core.api.ai;

import lombok.Data;

/** 提示词模板请求 */
@Data
public class PromptTemplateRequest {

  /** ID（更新时需要） */
  private Long id;

  /** 提示词唯一标识代码 */
  private String code;

  /** 提示词显示名称 */
  private String name;

  /** Markdown 格式的提示词内容 */
  private String content;
}
