package com.excalicode.platform.core.api.rag;

import java.util.List;
import lombok.Builder;
import lombok.Value;

/** 批量向量化执行结果 */
@Value
@Builder
public class RequirementKnowledgeBatchVectorizeResponse {

  /** 本次发现的“未向量化”条目数 */
  int targetCount;

  /** 成功向量化条数 */
  int successCount;

  /** 失败条数 */
  int failedCount;

  /** 失败详情（只返回前 N 条） */
  List<RequirementKnowledgeVectorizeError> errors;
}
