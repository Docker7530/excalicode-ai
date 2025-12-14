package com.excalicode.platform.core.api.system;

import lombok.Data;

/** 系统设置返回 */
@Data
public class SysSettingResponse {

  /** 配置 key */
  private String configKey;

  /** 配置 value */
  private String configValue;
}
