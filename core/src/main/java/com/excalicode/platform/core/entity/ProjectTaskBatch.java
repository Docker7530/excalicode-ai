package com.excalicode.platform.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 任务批次实体，代表一个大任务集合 */
@Data
@TableName("project_task_batch")
public class ProjectTaskBatch {

  /** 主键ID */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 批次/大任务标题 */
  private String title;

  /** 批次说明 */
  private String description;

  /** 创建人ID */
  private Long createdBy;

  /** 发布时间 */
  private LocalDateTime publishedTime;

  /** 创建时间 */
  private LocalDateTime createdTime;

  /** 更新时间 */
  private LocalDateTime updatedTime;

  /** 逻辑删除标记 */
  @TableLogic private Integer deleted;
}
