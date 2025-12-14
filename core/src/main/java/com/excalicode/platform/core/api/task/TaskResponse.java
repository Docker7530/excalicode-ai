package com.excalicode.platform.core.api.task;

import com.excalicode.platform.core.model.task.TaskStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/** 任务响应对象 */
@Data
public class TaskResponse {

  private Long id;

  private Long batchId;

  private String title;

  private String description;

  private BigDecimal workloadManDay;

  private TaskStatus status;

  private String statusLabel;

  private Long assigneeId;

  private String assigneeName;

  private Long createdBy;

  private String createdByName;

  private LocalDateTime publishedTime;

  private LocalDateTime createdTime;

  private LocalDateTime updatedTime;
}
