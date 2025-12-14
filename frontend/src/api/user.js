import { ENDPOINTS } from './endpoints.js';
import api from './request.js';

/**
 * 获取系统用户列表
 */
export function fetchUsers() {
  return api.get(ENDPOINTS.USER.COLLECTION);
}

/**
 * 创建系统用户
 */
export function createUser(data) {
  return api.post(ENDPOINTS.USER.COLLECTION, data);
}

/**
 * 更新系统用户
 */
export function updateUser(id, data) {
  return api.put(ENDPOINTS.USER.DETAIL(id), data);
}

/**
 * 删除系统用户
 */
export function removeUser(id) {
  return api.delete(ENDPOINTS.USER.DETAIL(id));
}
