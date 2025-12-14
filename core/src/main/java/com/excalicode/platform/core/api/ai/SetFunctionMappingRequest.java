package com.excalicode.platform.core.api.ai;

import lombok.Data;

/** 设置功能-模型映射请求 */
@Data
public class SetFunctionMappingRequest {

  /** 功能类型代码 */
  private String functionType;

  /** 模型ID */
  private Long modelId;
}
