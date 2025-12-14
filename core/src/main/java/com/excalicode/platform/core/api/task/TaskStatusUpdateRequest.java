package com.excalicode.platform.core.api.task;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 用户更新任务状态请求 */
@Data
public class TaskStatusUpdateRequest {

  /** 任务状态：PENDING 或 COMPLETED */
  @NotBlank(message = "任务状态不能为空")
  private String status;
}
