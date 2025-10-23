import { ENDPOINTS } from './endpoints.js';
import api from './request.js';

/**
 * 功能-提示词映射管理 API
 */

/**
 * 获取所有功能类型枚举
 */
export function getFunctionTypes() {
  return api.get(ENDPOINTS.FUNCTION_PROMPT.FUNCTION_TYPES);
}

/**
 * 获取所有映射关系 (包含提示词模板信息)
 */
export function listMappings() {
  return api.get(ENDPOINTS.FUNCTION_PROMPT.LIST);
}

/**
 * 根据功能代码查询提示词代码
 */
export function getPromptCodeByFunctionCode(functionCode) {
  return api.get(ENDPOINTS.FUNCTION_PROMPT.DETAIL(functionCode));
}

/**
 * 设置或更新功能的提示词映射
 */
export function setMapping(data) {
  return api.post(ENDPOINTS.FUNCTION_PROMPT.SET, data);
}

/**
 * 删除功能-提示词映射
 */
export function deleteMapping(functionCode, promptCode) {
  return api.delete(ENDPOINTS.FUNCTION_PROMPT.DELETE(functionCode, promptCode));
}

/**
 * 手动清除 Prompt 缓存
 */
export function clearCache() {
  return api.post(ENDPOINTS.FUNCTION_PROMPT.CLEAR_CACHE);
}
