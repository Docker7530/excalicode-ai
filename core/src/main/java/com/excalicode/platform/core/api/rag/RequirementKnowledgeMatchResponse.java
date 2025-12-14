package com.excalicode.platform.core.api.rag;

import com.excalicode.platform.core.model.rag.RequirementKnowledgeMatch;
import com.excalicode.platform.core.model.rag.RequirementKnowledgeType;
import java.util.List;
import lombok.Builder;
import lombok.Value;

/** 知识检索响应元素 */
@Value
@Builder
public class RequirementKnowledgeMatchResponse {
  String documentId;
  String title;
  String content;
  String source;
  List<String> tags;
  RequirementKnowledgeType type;
  int chunkIndex;
  Double similarityScore;
  Double rerankScore;

  /**
   * 将内部的匹配结果转换为接口响应。
   *
   * @param match 向量检索得到的片段
   * @return API 输出模型
   */
  public static RequirementKnowledgeMatchResponse fromMatch(RequirementKnowledgeMatch match) {
    return RequirementKnowledgeMatchResponse.builder()
        .documentId(match.getDocumentId())
        .title(match.getTitle())
        .content(match.getChunkContent())
        .source(match.getSource())
        .tags(match.getTags())
        .type(match.getType())
        .chunkIndex(match.getChunkIndex())
        .similarityScore(match.getSimilarityScore())
        .rerankScore(match.getRerankScore())
        .build();
  }
}
