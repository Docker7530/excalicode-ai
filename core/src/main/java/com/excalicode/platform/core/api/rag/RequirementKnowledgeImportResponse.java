package com.excalicode.platform.core.api.rag;

import java.util.List;
import lombok.Builder;
import lombok.Value;

/** Excel 导入结果 */
@Value
@Builder
public class RequirementKnowledgeImportResponse {

  /** 有内容的行数（空行不计入） */
  int totalRows;

  /** 成功入库条数 */
  int successCount;

  /** 失败条数 */
  int failedCount;

  /** 跳过的空行数 */
  int skippedCount;

  /** 失败详情（只返回前 N 条） */
  List<RequirementKnowledgeImportError> errors;
}
