package com.excalicode.platform.core.api.task;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 管理员更新任务描述请求 */
@Data
public class TaskDescriptionUpdateRequest {

  /** 新的任务描述 */
  @NotBlank(message = "任务描述不能为空")
  private String description;
}
