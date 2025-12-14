package com.excalicode.platform.core.api.ai;

import lombok.Data;

/** 设置功能-提示词映射请求 */
@Data
public class SetFunctionPromptMappingRequest {

  /** 功能代码 */
  private String functionCode;

  /** 提示词代码 */
  private String promptCode;
}
