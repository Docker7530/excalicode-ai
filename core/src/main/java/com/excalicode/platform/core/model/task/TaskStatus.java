package com.excalicode.platform.core.model.task;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 任务状态枚举 */
@Getter
@AllArgsConstructor
public enum TaskStatus {
  NOT_STARTED("未完成"),
  COMPLETED("已完成");

  private final String label;

  public static TaskStatus fromValue(String value) {
    for (TaskStatus status : values()) {
      if (status.name().equalsIgnoreCase(value)) {
        return status;
      }
    }
    throw new IllegalArgumentException("未知任务状态: " + value);
  }
}
