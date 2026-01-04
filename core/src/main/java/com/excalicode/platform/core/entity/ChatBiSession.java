package com.excalicode.platform.core.entity;

import java.time.LocalDateTime;
import lombok.Data;

/** ChatBI 会话实体 */
@Data
public class ChatBiSession {

  /** 主键ID */
  private Long id;

  /** 所属用户ID */
  private Long userId;

  /** 会话标题 */
  private String title;

  /** 最后活跃时间 */
  private LocalDateTime lastActiveTime;

  /** 创建时间 */
  private LocalDateTime createdTime;

  /** 更新时间 */
  private LocalDateTime updatedTime;

  /** 逻辑删 */
  private Integer deleted;
}
