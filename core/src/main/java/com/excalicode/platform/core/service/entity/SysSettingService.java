package com.excalicode.platform.core.service.entity;

import com.baomidou.mybatisplus.extension.service.IService;
import com.excalicode.platform.core.entity.SysSetting;

/** 系统设置 Service 接口 */
public interface SysSettingService extends IService<SysSetting> {

  /**
   * 根据配置 key 获取设置
   *
   * @param configKey 配置 key
   * @return 设置, 不存在则返回 null
   */
  SysSetting getByKey(String configKey);

  /**
   * 写入或更新配置
   *
   * @param configKey 配置 key
   * @param configValue 配置 value
   * @return 是否成功
   */
  boolean upsert(String configKey, String configValue);

  /**
   * 根据配置 key 删除设置（逻辑删除）
   *
   * @param configKey 配置 key
   * @return 是否成功
   */
  boolean removeByKey(String configKey);
}
