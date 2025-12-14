package com.excalicode.platform.core.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import java.time.LocalDateTime;
import lombok.Data;

/** AI 功能-提示词映射实体 存储功能与提示词模板的绑定关系，实现功能级别的提示词切换 */
@Data
public class AiFunctionPromptMapping {

  /** 主键ID, 数据库自动生成 */
  private Long id;

  /** 功能标识（对应 AiFunctionType 枚举的 code） */
  private String functionCode;

  /** 提示词代码（对应 ai_prompt_template 表的 code 字段） */
  private String promptCode;

  /** 创建时间, 插入时自动填充 */
  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createdTime;

  /** 更新时间, 插入和更新时自动填充 */
  @TableField(fill = FieldFill.INSERT_UPDATE)
  private LocalDateTime updatedTime;

  /** 逻辑删除标记: 0-未删除, 1-已删除 */
  private Integer deleted;

  /** 关联的提示词模板信息 (不存储在数据库, 用于关联查询) */
  @TableField(exist = false)
  private AiPromptTemplate promptTemplate;
}
