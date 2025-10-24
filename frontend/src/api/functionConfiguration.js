import { ENDPOINTS } from './endpoints.js';
import api from './request.js';

/**
 * 功能配置聚合 API
 */

/**
 * 获取功能配置聚合数据
 */
export function listFunctionConfigurations() {
  return api.get(ENDPOINTS.FUNCTION_CONFIGURATION.LIST);
}

/**
 * 设置功能的模型映射
 */
export function setFunctionModelMapping(data) {
  return api.post(ENDPOINTS.AI_FUNCTION.SET, data);
}

/**
 * 删除功能的模型映射
 */
export function deleteFunctionModelMapping(id) {
  return api.delete(ENDPOINTS.AI_FUNCTION.DETAIL(id));
}

/**
 * 设置功能的提示词映射
 */
export function setFunctionPromptMapping(data) {
  return api.post(ENDPOINTS.FUNCTION_PROMPT.SET, data);
}

/**
 * 删除功能的提示词映射
 */
export function deleteFunctionPromptMapping(functionCode, promptCode) {
  return api.delete(ENDPOINTS.FUNCTION_PROMPT.DELETE(functionCode, promptCode));
}
