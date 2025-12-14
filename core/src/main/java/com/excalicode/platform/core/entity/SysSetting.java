package com.excalicode.platform.core.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import java.time.LocalDateTime;
import lombok.Data;

/** 系统设置实体 用于存储 key-value 配置 */
@Data
public class SysSetting {

  /** 主键ID, 数据库自动生成 */
  private Long id;

  /** 配置 key（唯一） */
  private String configKey;

  /** 配置 value（可存 Markdown 等长文本） */
  private String configValue;

  /** 创建时间, 插入时自动填充 */
  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createdTime;

  /** 更新时间, 插入和更新时自动填充 */
  @TableField(fill = FieldFill.INSERT_UPDATE)
  private LocalDateTime updatedTime;

  /** 逻辑删除标记: 0-未删除, 1-已删除 */
  private Integer deleted;
}
