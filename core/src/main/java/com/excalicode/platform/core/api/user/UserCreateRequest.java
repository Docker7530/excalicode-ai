package com.excalicode.platform.core.api.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 管理员创建系统用户请求 */
@Data
public class UserCreateRequest {

  /** 用户名 */
  @NotBlank(message = "用户名不能为空")
  @Size(min = 3, max = 50, message = "用户名长度需在3-50个字符之间")
  private String username;

  /** 初始密码 */
  @NotBlank(message = "密码不能为空")
  @Size(min = 6, max = 64, message = "密码长度需在6-64个字符之间")
  private String password;

  /** 角色 */
  @NotBlank(message = "角色不能为空")
  @Pattern(regexp = "ADMIN|USER", message = "角色必须是 ADMIN 或 USER")
  private String role;
}
