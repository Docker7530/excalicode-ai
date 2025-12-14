package com.excalicode.platform.core.api.cosmic;

import com.excalicode.platform.core.model.cosmic.CosmicAnalysisTaskStatus;
import com.excalicode.platform.core.model.cosmic.CosmicProcess;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 子过程异步任务返回结构 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CosmicAnalysisTaskResponse {

  /** 任务ID */
  private Long taskId;

  /** 任务状态 */
  private CosmicAnalysisTaskStatus status;

  /** 状态标签（中文） */
  private String statusLabel;

  /** 失败原因（失败时存在） */
  private String errorMessage;

  /** 结果子过程数量（成功时填充） */
  private Integer processCount;

  /** 提交的功能过程（便于识别任务） */
  private List<FunctionalProcess> functionalProcesses;

  /** 成功结果：COSMIC 子过程 */
  private List<CosmicProcess> processes;

  /** 创建时间 */
  private LocalDateTime createdTime;

  /** 开始时间 */
  private LocalDateTime startedTime;

  /** 完成时间 */
  private LocalDateTime finishedTime;
}
