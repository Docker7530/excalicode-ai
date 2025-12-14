package com.excalicode.platform.core.api.rag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/** 检索 RAG 知识的请求 */
@Data
public class RequirementKnowledgeSearchRequest {

  /** 查询内容，作为相似度检索的请求 */
  @NotBlank(message = "查询内容不能为空")
  private String query;

  /** 返回的最大片段数。 */
  @Positive(message = "topK 需大于 0")
  private Integer topK;

  /** 相似度阈值，高了可以剔除噪音 */
  private Double minScore;
}
