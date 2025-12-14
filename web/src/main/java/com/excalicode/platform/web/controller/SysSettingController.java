package com.excalicode.platform.web.controller;

import com.excalicode.platform.core.api.system.SysSettingResponse;
import com.excalicode.platform.core.entity.SysSetting;
import com.excalicode.platform.core.service.entity.SysSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 系统设置读取接口（登录用户可读） */
@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SysSettingController {

  private final SysSettingService sysSettingService;

  /** 根据 key 获取设置（不存在返回 value=null） */
  @GetMapping("/{configKey}")
  public ResponseEntity<SysSettingResponse> getByKey(@PathVariable String configKey) {
    SysSetting setting = sysSettingService.getByKey(configKey);
    SysSettingResponse response = new SysSettingResponse();
    response.setConfigKey(configKey);
    response.setConfigValue(setting == null ? null : setting.getConfigValue());
    return ResponseEntity.ok(response);
  }
}
