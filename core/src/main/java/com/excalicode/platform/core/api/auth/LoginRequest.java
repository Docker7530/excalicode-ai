package com.excalicode.platform.core.api.auth;

import lombok.Data;

/** 登录请求 */
@Data
public class LoginRequest {

  /** 用户名 */
  private String username;

  /** 密码 */
  private String password;
}
