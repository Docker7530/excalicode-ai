package com.excalicode.platform.core.api.rag;

import lombok.Builder;
import lombok.Value;

/** 批量向量化失败详情 */
@Value
@Builder
public class RequirementKnowledgeVectorizeError {

  /** 文档ID */
  String documentId;

  /** 标题（可能为空） */
  String title;

  /** 失败原因 */
  String message;
}
