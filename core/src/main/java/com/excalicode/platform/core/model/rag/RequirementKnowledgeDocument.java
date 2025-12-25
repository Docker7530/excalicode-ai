package com.excalicode.platform.core.model.rag;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;
import org.springframework.util.StringUtils;

/** RAG 知识文档定义 */
@Value
@Builder(toBuilder = true)
public class RequirementKnowledgeDocument {

  String documentId;
  String title;
  String content;
  @Builder.Default List<String> tags = List.of();

  public RequirementKnowledgeDocument normalized() {
    String finalId =
        StringUtils.hasText(this.documentId) ? this.documentId : UUID.randomUUID().toString();
    List<String> finalTags = this.tags == null ? List.of() : List.copyOf(this.tags);
    return this.toBuilder().documentId(finalId).tags(finalTags).build();
  }

  public List<String> getSafeTags() {
    return this.tags == null ? List.of() : Collections.unmodifiableList(this.tags);
  }
}
