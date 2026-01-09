import { ENDPOINTS } from './endpoints.js';
import api, { request } from './request.js';

/**
 * 导入任务草稿
 */
export function importTaskDrafts(file) {
  const formData = new FormData();
  formData.append('file', file);
  return request.post(ENDPOINTS.TASK.ADMIN_IMPORT, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
}

/**
 * 发布任务批次
 */
export function publishTaskBatch(payload) {
  return api.post(ENDPOINTS.TASK.ADMIN_PUBLISH, payload);
}

/**
 * 管理员批次列表
 */
export function fetchTaskBatches() {
  return api.get(ENDPOINTS.TASK.ADMIN_BATCHES);
}

/**
 * 管理员批次详情
 */
export function fetchTaskBatchDetail(batchId) {
  return api.get(ENDPOINTS.TASK.ADMIN_BATCH_DETAIL(batchId));
}

/**
 * 更新子任务执行人
 */
export function updateTaskAssignee(batchId, taskId, assigneeId) {
  return api.put(ENDPOINTS.TASK.ADMIN_UPDATE_ASSIGNEE(batchId, taskId), {
    assigneeId,
  });
}

/**
 * 更新子任务描述
 */
export function updateTaskDescription(batchId, taskId, description) {
  return api.put(ENDPOINTS.TASK.ADMIN_UPDATE_DESCRIPTION(batchId, taskId), {
    description,
  });
}

/**
 * 获取可分配用户列表
 */
export function fetchAssignableUsers() {
  return api.get(ENDPOINTS.TASK.ADMIN_ASSIGNEES);
}

/**
 * 当前用户参与的大任务
 */
export function fetchMyTaskBatches() {
  return api.get(ENDPOINTS.TASK.MY_BATCHES);
}

/**
 * 当前用户某批次的任务详情
 */
export function fetchMyTaskBatchDetail(batchId) {
  return api.get(ENDPOINTS.TASK.MY_BATCH_DETAIL(batchId));
}

/**
 * 更新任务状态
 */
export function updateTaskStatus(taskId, status) {
  return api.patch(ENDPOINTS.TASK.UPDATE_STATUS(taskId), { status });
}
