package com.excalicode.platform.web.controller;

import com.excalicode.platform.core.api.system.SysSettingResponse;
import com.excalicode.platform.core.api.system.SysSettingValueRequest;
import com.excalicode.platform.core.entity.SysSetting;
import com.excalicode.platform.core.exception.BusinessException;
import com.excalicode.platform.core.service.entity.SysSettingService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 管理员系统设置接口（key-value 配置） */
@Validated
@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
public class SysSettingAdminController {

  private final SysSettingService sysSettingService;

  /** 查询全部系统设置 */
  @GetMapping
  public ResponseEntity<List<SysSettingResponse>> list() {
    List<SysSettingResponse> settings =
        sysSettingService.lambdaQuery().orderByDesc(SysSetting::getUpdatedTime).list().stream()
            .map(this::toResponse)
            .toList();
    return ResponseEntity.ok(settings);
  }

  /** 创建或更新某个 key 的 value */
  @PutMapping("/{configKey}")
  public ResponseEntity<SysSettingResponse> upsert(
      @PathVariable String configKey, @RequestBody @Valid SysSettingValueRequest request) {
    if (configKey == null || configKey.isBlank()) {
      throw new BusinessException("配置 key 不能为空");
    }

    boolean success = sysSettingService.upsert(configKey, request.getConfigValue());
    if (!success) {
      return ResponseEntity.internalServerError().build();
    }

    SysSetting setting = sysSettingService.getByKey(configKey);
    return ResponseEntity.ok(setting == null ? emptyResponse(configKey) : toResponse(setting));
  }

  /** 删除某个 key（逻辑删除） */
  @DeleteMapping("/{configKey}")
  public ResponseEntity<Void> delete(@PathVariable String configKey) {
    if (configKey == null || configKey.isBlank()) {
      throw new BusinessException("配置 key 不能为空");
    }

    boolean removed = sysSettingService.removeByKey(configKey);
    return removed ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
  }

  private SysSettingResponse toResponse(SysSetting setting) {
    SysSettingResponse response = new SysSettingResponse();
    response.setConfigKey(setting.getConfigKey());
    response.setConfigValue(setting.getConfigValue());
    return response;
  }

  private SysSettingResponse emptyResponse(String configKey) {
    SysSettingResponse response = new SysSettingResponse();
    response.setConfigKey(configKey);
    response.setConfigValue(null);
    return response;
  }
}
