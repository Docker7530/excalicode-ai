package com.excalicode.platform.core.api.task;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 管理员更新任务执行人请求 */
@Data
public class TaskAssignmentRequest {

  /** 新的执行人ID */
  @NotNull(message = "执行人不能为空")
  private Long assigneeId;
}
