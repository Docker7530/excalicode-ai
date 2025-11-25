package com.excalicode.platform.core.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** AI 模型实体 存储 AI 模型信息, 每个模型属于一个厂商 */
@Data
@TableName("ai_model")
public class AiModel {

  /** 主键ID, 数据库自动生成 */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 厂商ID, 外键关联 ai_provider 表 */
  @TableField("provider_id")
  private Long providerId;

  /** 模型名称 (如: gpt-4, claude-3, gemini-pro) */
  @TableField("model_name")
  private String modelName;

  /** 是否支持 JSON Schema 响应格式 (默认为 true, 兼容既有 OpenAI 模型) */
  @TableField("supports_json_schema")
  private Boolean supportsJsonSchema;

  /** 创建时间, 插入时自动填充 */
  @TableField(value = "created_time", fill = FieldFill.INSERT)
  private LocalDateTime createdTime;

  /** 更新时间, 插入和更新时自动填充 */
  @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
  private LocalDateTime updatedTime;

  /** 逻辑删除标记: 0-未删除, 1-已删除 */
  @TableLogic
  @TableField("deleted")
  private Integer deleted;

  /** 关联的厂商信息 (不存储在数据库, 用于关联查询) */
  @TableField(exist = false)
  private AiProvider provider;
}
