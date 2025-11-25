package com.excalicode.platform.core.api.task;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/** 任务批次摘要信息 */
@Data
public class TaskBatchSummaryResponse {

  private Long id;

  private String title;

  private String description;

  private LocalDateTime publishedTime;

  private Long createdBy;

  private String createdByName;

  private int totalTasks;

  private int completedTasks;

  private BigDecimal totalWorkload;
}
