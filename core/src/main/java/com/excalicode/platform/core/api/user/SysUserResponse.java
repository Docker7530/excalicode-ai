package com.excalicode.platform.core.api.user;

import java.time.LocalDateTime;
import lombok.Data;

/** 系统用户响应 */
@Data
public class SysUserResponse {

  /** 用户ID */
  private Long id;

  /** 用户名 */
  private String username;

  /** 角色 */
  private String role;

  /** 创建时间 */
  private LocalDateTime createdTime;

  /** 更新时间 */
  private LocalDateTime updatedTime;
}
