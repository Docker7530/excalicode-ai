import { ENDPOINTS } from './endpoints.js';
import api from './request.js';

/**
 * AI 厂商管理 API
 */

/**
 * 查询所有厂商 (包含关联的模型)
 */
export function listProviders() {
  return api.get(ENDPOINTS.AI_PROVIDER.LIST);
}

/**
 * 新增厂商
 */
export function createProvider(data) {
  return api.post(ENDPOINTS.AI_PROVIDER.CREATE, data);
}

/**
 * 更新厂商
 */
export function updateProvider(id, data) {
  return api.put(ENDPOINTS.AI_PROVIDER.DETAIL(id), data);
}

/**
 * 删除厂商
 */
export function deleteProvider(id) {
  return api.delete(ENDPOINTS.AI_PROVIDER.DETAIL(id));
}

/**
 * 新增模型
 */
export function createModel(data) {
  return api.post(ENDPOINTS.AI_MODEL.CREATE, data);
}

/**
 * 更新模型
 */
export function updateModel(id, data) {
  return api.put(ENDPOINTS.AI_MODEL.DETAIL(id), data);
}

/**
 * 删除模型
 */
export function deleteModel(id) {
  return api.delete(ENDPOINTS.AI_MODEL.DETAIL(id));
}
