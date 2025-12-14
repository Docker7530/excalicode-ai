package com.excalicode.platform.core.entity;

import java.time.LocalDateTime;
import lombok.Data;

/** 系统用户实体类 */
@Data
public class SysUser {

  /** 主键ID */
  private Long id;

  /** 用户名 */
  private String username;

  /** 密码(BCrypt加密) */
  private String password;

  /** 角色: ADMIN-管理员, USER-普通用户 */
  private String role;

  /** 创建时间 */
  private LocalDateTime createdTime;

  /** 更新时间 */
  private LocalDateTime updatedTime;

  /** 逻辑删除: 0-未删除, 1-已删除 */
  private Integer deleted;
}
