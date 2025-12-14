package com.excalicode.platform.core.api.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 登录态校验响应 */
@Data
@AllArgsConstructor
public class SessionValidationResponse {

  /** 用户名 */
  private String username;

  /** 角色 */
  private String role;
}
