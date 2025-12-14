package com.excalicode.platform.core.api.system;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 系统设置 value 更新请求 */
@Data
public class SysSettingValueRequest {

  /** 配置 value（允许为空字符串，但不允许 null） */
  @NotNull(message = "配置 value 不能为空")
  private String configValue;
}
