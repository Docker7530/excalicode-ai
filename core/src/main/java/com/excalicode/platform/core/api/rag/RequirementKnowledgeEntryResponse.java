package com.excalicode.platform.core.api.rag;

import com.excalicode.platform.core.entity.RequirementKnowledgeEntry;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import org.springframework.util.StringUtils;

/** 需求知识库条目响应 */
@Value
@Builder
public class RequirementKnowledgeEntryResponse {

  String documentId;
  String title;
  String content;
  List<String> tags;

  Integer vectorized;
  LocalDateTime vectorUpdatedTime;

  LocalDateTime createdTime;
  LocalDateTime updatedTime;

  public static RequirementKnowledgeEntryResponse fromEntity(RequirementKnowledgeEntry entity) {
    return RequirementKnowledgeEntryResponse.builder()
        .documentId(entity.getDocumentId())
        .title(entity.getTitle())
        .content(entity.getContent())
        .tags(splitTags(entity.getTags()))
        .vectorized(entity.getVectorized())
        .vectorUpdatedTime(entity.getVectorUpdatedTime())
        .createdTime(entity.getCreatedTime())
        .updatedTime(entity.getUpdatedTime())
        .build();
  }

  private static List<String> splitTags(String tags) {
    if (!StringUtils.hasText(tags)) {
      return List.of();
    }
    String[] parts = tags.split(",");
    List<String> result = new ArrayList<>();
    for (String part : parts) {
      if (StringUtils.hasText(part)) {
        result.add(part.trim());
      }
    }
    return result;
  }
}
