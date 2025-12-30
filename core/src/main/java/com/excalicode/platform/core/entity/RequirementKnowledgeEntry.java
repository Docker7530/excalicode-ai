package com.excalicode.platform.core.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import lombok.Data;

/** 需求知识库条目实体（仅存储，不自动向量化） */
@Data
public class RequirementKnowledgeEntry {

  /** 文档ID（业务主键，前端可指定；创建后不可修改） */
  @TableId(value = "document_id", type = IdType.INPUT)
  private String documentId;

  /** 标题 */
  private String title;

  /** 正文内容 */
  private String content;

  /** 标签（逗号分隔） */
  private String tags;

  /** 是否已向量化: 0-否, 1-是 */
  private Integer vectorized;

  /** 向量更新时间 */
  private LocalDateTime vectorUpdatedTime;

  /** 创建时间, 插入时自动填充 */
  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createdTime;

  /** 更新时间, 插入和更新时自动填充 */
  @TableField(fill = FieldFill.INSERT_UPDATE)
  private LocalDateTime updatedTime;

  /** 逻辑删除标记: 0-未删除, 1-已删除 */
  private Integer deleted;
}
