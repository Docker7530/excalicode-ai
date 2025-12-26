package com.excalicode.platform.core.api.rag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

/** 更新需求知识库条目请求 */
@Data
public class RequirementKnowledgeEntryUpdateRequest {

  @NotBlank(message = "标题不能为空")
  @Size(max = 128, message = "标题长度不能超过 128 字")
  private String title;

  @NotBlank(message = "知识内容不能为空")
  private String content;

  private List<String> tags;
}
