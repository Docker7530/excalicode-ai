package com.excalicode.platform.core.api.ai;

import com.excalicode.platform.core.entity.AiFunctionModelMapping;
import com.excalicode.platform.core.entity.AiFunctionPromptMapping;
import lombok.Data;

/** 功能配置项 聚合单个功能的模型映射与提示词映射信息，支持前端以功能维度展示与编辑。 */
@Data
public class FunctionConfigurationItem {

  /** 功能类型代码，对应 AiFunctionType 的 code。 */
  private String functionCode;

  /** 功能描述，给前端做展示。 */
  private String functionDescription;

  /** 绑定的模型映射信息，可能为空。 */
  private AiFunctionModelMapping modelMapping;

  /** 绑定的提示词映射信息，可能为空。 */
  private AiFunctionPromptMapping promptMapping;
}
