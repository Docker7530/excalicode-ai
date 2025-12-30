package com.excalicode.platform.core.model.rag;

import java.util.List;
import lombok.Builder;
import lombok.Value;

/** 检索到的知识片段 */
@Value
@Builder(toBuilder = true)
public class RequirementKnowledgeMatch {
  /** 文档ID */
  String documentId;

  /** 文档标题 */
  String title;

  /** 命中的内容片段 */
  String chunkContent;

  /** 关联标签 */
  List<String> tags;

  /** 片段在文档中的序号 */
  int chunkIndex;

  /** 向量相似度分数 */
  Double similarityScore;

  /** 重排后的分数 */
  Double rerankScore;
}
