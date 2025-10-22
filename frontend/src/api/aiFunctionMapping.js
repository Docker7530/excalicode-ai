import { ENDPOINTS } from './endpoints.js';
import api from './request.js';

/**
 * AI 功能-模型映射管理 API
 */

/**
 * 获取所有功能类型枚举
 */
export function getFunctionTypes() {
  return api.get(ENDPOINTS.AI_FUNCTION.FUNCTION_TYPES);
}

/**
 * 获取所有映射关系 (包含模型和厂商信息)
 */
export function listMappings() {
  return api.get(ENDPOINTS.AI_FUNCTION.LIST);
}

/**
 * 设置或更新功能类型的模型映射
 */
export function setMapping(data) {
  return api.post(ENDPOINTS.AI_FUNCTION.SET, data);
}

/**
 * 删除功能映射
 */
export function deleteMapping(id) {
  return api.delete(ENDPOINTS.AI_FUNCTION.DETAIL(id));
}

/**
 * 手动清除 ChatModel 缓存
 */
export function clearCache() {
  return api.post(ENDPOINTS.AI_FUNCTION.CLEAR_CACHE);
}
