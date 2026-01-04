package com.excalicode.platform.core.entity;

import java.time.LocalDateTime;
import lombok.Data;

/** ChatBI 消息实体 */
@Data
public class ChatBiMessage {

  /** 主键ID */
  private Long id;

  /** 会话ID */
  private Long sessionId;

  /** 消息角色: USER, ASSISTANT, SYSTEM */
  private String role;

  /** 消息内容 */
  private String content;

  /** 查询计划(JSON) */
  private String planJson;

  /** 执行SQL（先记录下） */
  private String executedSql;

  /** 查询结果(JSON) */
  private String resultJson;

  /** 失败原因 */
  private String errorMessage;

  /** 创建时间 */
  private LocalDateTime createdTime;

  /** 更新时间 */
  private LocalDateTime updatedTime;

  /** 逻辑删 */
  private Integer deleted;
}
