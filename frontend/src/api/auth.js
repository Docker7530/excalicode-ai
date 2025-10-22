import { ENDPOINTS } from './endpoints.js';
import api from './request.js';

/**
 * 用户登录
 * @param {Object} data - 登录数据 { username, password }
 * @returns {Promise} 登录响应
 */
export function login(data) {
  return api.post(ENDPOINTS.AUTH.LOGIN, data);
}
