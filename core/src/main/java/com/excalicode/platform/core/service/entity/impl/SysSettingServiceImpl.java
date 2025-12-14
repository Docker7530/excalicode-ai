package com.excalicode.platform.core.service.entity.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.excalicode.platform.core.config.CacheConfig;
import com.excalicode.platform.core.entity.SysSetting;
import com.excalicode.platform.core.mapper.SysSettingMapper;
import com.excalicode.platform.core.service.entity.SysSettingService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/** 系统设置 Service 实现类 */
@Service
public class SysSettingServiceImpl extends ServiceImpl<SysSettingMapper, SysSetting>
    implements SysSettingService {

  @Override
  @Cacheable(value = CacheConfig.SYS_SETTINGS_CACHE, key = "#configKey")
  public SysSetting getByKey(String configKey) {
    if (configKey == null || configKey.trim().isEmpty()) {
      return null;
    }

    return this.getOne(
        new LambdaQueryWrapper<SysSetting>().eq(SysSetting::getConfigKey, configKey.trim()));
  }

  @Override
  @CacheEvict(value = CacheConfig.SYS_SETTINGS_CACHE, allEntries = true)
  public boolean upsert(String configKey, String configValue) {
    if (configKey == null || configKey.trim().isEmpty()) {
      return false;
    }

    String normalizedKey = configKey.trim();
    String normalizedValue = configValue == null ? "" : configValue;

    SysSetting existing = this.getByKey(normalizedKey);
    if (existing != null) {
      existing.setConfigValue(normalizedValue);
      return this.updateById(existing);
    }

    SysSetting setting = new SysSetting();
    setting.setConfigKey(normalizedKey);
    setting.setConfigValue(normalizedValue);
    return this.save(setting);
  }

  @Override
  @CacheEvict(value = CacheConfig.SYS_SETTINGS_CACHE, allEntries = true)
  public boolean removeByKey(String configKey) {
    if (configKey == null || configKey.trim().isEmpty()) {
      return false;
    }

    return this.remove(
        new LambdaQueryWrapper<SysSetting>().eq(SysSetting::getConfigKey, configKey));
  }
}
