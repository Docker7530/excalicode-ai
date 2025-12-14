package com.excalicode.platform.core.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import java.time.LocalDateTime;
import lombok.Data;

/** 提示词模板实体 存储 Markdown 格式的提示词模板 */
@Data
public class AiPromptTemplate {

  /** 主键ID, 数据库自动生成 */
  private Long id;

  /** 提示词唯一标识代码（如 "REQUIREMENT_DOC_GENERATOR"） 用于代码中引用，保持人类可读性和配置可移植性 */
  private String code;

  /** 提示词显示名称 */
  private String name;

  /** Markdown 格式的提示词内容 使用 TEXT 类型存储，支持长文本 */
  private String content;

  /** 创建时间, 插入时自动填充 */
  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createdTime;

  /** 更新时间, 插入和更新时自动填充 */
  @TableField(fill = FieldFill.INSERT_UPDATE)
  private LocalDateTime updatedTime;

  /** 逻辑删除标记: 0-未删除, 1-已删除 */
  private Integer deleted;
}
