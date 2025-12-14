package com.excalicode.platform.core.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/** 任务实体，记录任务发布与执行情况。 */
@Data
public class ProjectTask {

  /** 主键ID */
  private Long id;

  /** 所属批次ID */
  private Long batchId;

  /** 任务标题 */
  private String title;

  /** 任务描述 */
  private String description;

  /** 计入工作量(人天) */
  private BigDecimal workloadManDay;

  /** 任务状态：NOT_STARTED, COMPLETED */
  private String status;

  /** 执行人ID */
  private Long assigneeId;

  /** 任务发布时间 */
  private LocalDateTime publishedTime;

  /** 发布人ID */
  private Long createdBy;

  /** 创建时间 */
  private LocalDateTime createdTime;

  /** 更新时间 */
  private LocalDateTime updatedTime;

  /** 逻辑删除标记 */
  private Integer deleted;
}
