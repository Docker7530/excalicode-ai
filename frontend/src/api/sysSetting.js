/**
 * 系统设置 API（key-value）
 */
import api from './request.js';
import { ENDPOINTS } from './endpoints.js';

/**
 * 获取设置（登录用户可读）
 */
export const getSettingByKey = (configKey) =>
  api.get(ENDPOINTS.SYS_SETTING.DETAIL(configKey));

/**
 * 管理员：查询全部设置
 */
export const listAdminSettings = () =>
  api.get(ENDPOINTS.SYS_SETTING.ADMIN_LIST);

/**
 * 管理员：创建或更新某个 key 的 value
 */
export const upsertAdminSetting = (configKey, configValue) =>
  api.put(ENDPOINTS.SYS_SETTING.ADMIN_UPSERT(configKey), { configValue });

/**
 * 管理员：删除某个 key
 */
export const deleteAdminSetting = (configKey) =>
  api.delete(ENDPOINTS.SYS_SETTING.ADMIN_DELETE(configKey));
