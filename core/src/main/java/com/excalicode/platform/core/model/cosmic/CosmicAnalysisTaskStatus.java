package com.excalicode.platform.core.model.cosmic;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** COSMIC 子过程异步任务状态 */
@Getter
@AllArgsConstructor
public enum CosmicAnalysisTaskStatus {
  PENDING("待处理"),
  RUNNING("生成中"),
  SUCCEEDED("已完成"),
  FAILED("失败");

  private final String label;

  public static CosmicAnalysisTaskStatus fromValue(String value) {
    for (CosmicAnalysisTaskStatus status : values()) {
      if (status.name().equalsIgnoreCase(value)) {
        return status;
      }
    }
    throw new IllegalArgumentException("未知的子过程任务状态: " + value);
  }
}
