package com.excalicode.platform.core.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import java.time.LocalDateTime;
import lombok.Data;

/** AI 功能-模型映射实体 存储功能类型与 AI 模型的绑定关系，实现功能级别的模型切换 */
@Data
public class AiFunctionModelMapping {

  /** 主键ID, 数据库自动生成 */
  private Long id;

  /** 功能类型代码（对应 AiFunctionType 枚举的 code） */
  private String functionType;

  /** 模型ID, 外键关联 ai_model 表 */
  private Long modelId;

  /** 创建时间, 插入时自动填充 */
  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createdTime;

  /** 更新时间, 插入和更新时自动填充 */
  @TableField(fill = FieldFill.INSERT_UPDATE)
  private LocalDateTime updatedTime;

  /** 逻辑删除标记: 0-未删除, 1-已删除 */
  private Integer deleted;

  /** 关联的模型信息 (不存储在数据库, 用于关联查询) */
  @TableField(exist = false)
  private AiModel model;
}
