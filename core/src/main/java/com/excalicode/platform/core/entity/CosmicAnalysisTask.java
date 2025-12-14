package com.excalicode.platform.core.entity;

import java.time.LocalDateTime;
import lombok.Data;

/** COSMIC 子过程异步任务持久化实体 */
@Data
public class CosmicAnalysisTask {

  /** 主键ID */
  private Long id;

  /** 提交人ID */
  private Long userId;

  /** 提交人用户名冗余，便于排查 */
  private String username;

  /** 任务状态：PENDING、RUNNING、SUCCEEDED、FAILED */
  private String status;

  /** 入参：功能过程列表 JSON */
  private String requestPayload;

  /** 成功结果：COSMIC 子过程 JSON */
  private String responsePayload;

  /** 错误原因（失败时填充） */
  private String errorMessage;

  /** 开始执行时间 */
  private LocalDateTime startedTime;

  /** 完成时间 */
  private LocalDateTime finishedTime;

  /** 创建时间 */
  private LocalDateTime createdTime;

  /** 更新时间 */
  private LocalDateTime updatedTime;

  /** 逻辑删除标记 */
  private Integer deleted;
}
