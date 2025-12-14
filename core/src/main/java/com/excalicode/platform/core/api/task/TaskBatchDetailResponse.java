package com.excalicode.platform.core.api.task;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/** 批次详情响应 */
@Data
public class TaskBatchDetailResponse {

  private Long id;

  private String title;

  private String description;

  private LocalDateTime publishedTime;

  private Long createdBy;

  private String createdByName;

  private int totalTasks;

  private int completedTasks;

  private BigDecimal totalWorkload;

  private List<TaskResponse> tasks;
}
