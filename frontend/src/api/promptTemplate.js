import { ENDPOINTS } from './endpoints.js';
import api from './request.js';

/**
 * 提示词模板管理 API
 */

/**
 * 获取所有提示词模板
 */
export function listPromptTemplates() {
  return api.get(ENDPOINTS.PROMPT_TEMPLATE.LIST);
}

/**
 * 搜索提示词模板
 */
export function searchPromptTemplates(keyword) {
  return api.get(ENDPOINTS.PROMPT_TEMPLATE.SEARCH, { params: { keyword } });
}

/**
 * 根据 ID 获取提示词模板详情
 */
export function getPromptTemplateById(id) {
  return api.get(ENDPOINTS.PROMPT_TEMPLATE.DETAIL(id));
}

/**
 * 根据 code 获取提示词模板
 */
export function getPromptTemplateByCode(code) {
  return api.get(ENDPOINTS.PROMPT_TEMPLATE.BY_CODE(code));
}

/**
 * 创建提示词模板
 */
export function createPromptTemplate(data) {
  return api.post(ENDPOINTS.PROMPT_TEMPLATE.CREATE, data);
}

/**
 * 更新提示词模板
 */
export function updatePromptTemplate(id, data) {
  return api.put(ENDPOINTS.PROMPT_TEMPLATE.UPDATE(id), data);
}

/**
 * 删除提示词模板
 */
export function deletePromptTemplate(id) {
  return api.delete(ENDPOINTS.PROMPT_TEMPLATE.DETAIL(id));
}
