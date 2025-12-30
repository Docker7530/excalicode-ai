package com.excalicode.platform.core.api.rag;

import com.excalicode.platform.core.model.rag.RequirementKnowledgeDocument;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

/** 知识文档入库请求 */
@Data
public class RequirementKnowledgeUpsertRequest {

  /** 可选的业务主键，空则由服务端自动生成 */
  private String documentId;

  /** 便于管理后台识别知识条目的名称 */
  @NotBlank(message = "标题不能为空")
  @Size(max = 128, message = "标题长度不能超过 128 字")
  private String title;

  /** 需要被切片、向量化的正文内容 */
  @NotBlank(message = "知识内容不能为空")
  private String content;

  /** 标签集合，可做过滤或提示 */
  private List<String> tags;

  /**
   * 将请求对象转换为领域模型，供服务层完成切片与入库。
   *
   * @return 规范化的知识文档
   */
  public RequirementKnowledgeDocument toDocument() {
    return RequirementKnowledgeDocument.builder()
        .documentId(documentId)
        .title(title)
        .content(content)
        .tags(tags)
        .build();
  }
}
