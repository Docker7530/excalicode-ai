package com.excalicode.platform.core.api.rag;

import lombok.Builder;
import lombok.Value;

/** Excel 导入错误信息 */
@Value
@Builder
public class RequirementKnowledgeImportError {

  /** Excel 行号（从 1 开始） */
  int rowIndex;

  /** 失败原因 */
  String message;
}
