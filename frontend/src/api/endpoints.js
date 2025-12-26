/**
 * API 路径配置
 * 统一集中管理，确保所有请求入口保持一致
 */

const normalizeBaseURL = (url = '/web') => {
  const trimmed = (url || '').trim() || '/web';
  return trimmed.endsWith('/') ? trimmed.slice(0, -1) : trimmed;
};

const DEFAULT_API_BASE_URL = '/web';

export const API_BASE_URL = normalizeBaseURL(
  import.meta?.env?.VITE_API_BASE_URL || DEFAULT_API_BASE_URL,
);

export const ENDPOINTS = Object.freeze({
  AUTH: {
    LOGIN: '/api/auth/login',
    SESSION: '/api/auth/session',
  },

  USER: {
    COLLECTION: '/api/admin/users',
    DETAIL: (id) => `/api/admin/users/${id}`,
  },

  AI_PROVIDER: {
    LIST: '/api/ai-provider/list',
    CREATE: '/api/ai-provider',
    DETAIL: (id) => `/api/ai-provider/${id}`,
  },

  AI_MODEL: {
    CREATE: '/api/ai-model',
    DETAIL: (id) => `/api/ai-model/${id}`,
  },

  AI_FUNCTION: {
    LIST: '/api/ai-function/list',
    SET: '/api/ai-function/set',
    DETAIL: (id) => `/api/ai-function/${id}`,
  },

  REQUIREMENT: {
    ENHANCE: '/api/requirement/enhance',
    BREAKDOWN: '/api/process/breakdown',
    ANALYZE: '/api/cosmic/analyze',
    ANALYZE_TASK: '/api/cosmic/analyze/task',
    ANALYZE_TASKS: '/api/cosmic/analyze/tasks',
    ANALYZE_TASK_DETAIL: (taskId) => `/api/cosmic/analyze/tasks/${taskId}`,
    IMPORT_PROCESSES: '/api/cosmic/process/import',
    IMPORT_COSMIC_PROCESSES: '/api/cosmic/subprocess/import',
    EXPORT_TABLE: '/api/cosmic/table/export',
    GENERATE_DOCUMENT: '/api/cosmic/documents/preview',
    EXPORT_DOCUMENT: '/api/cosmic/documents/export',
    SEQUENCE_DIAGRAM: '/api/cosmic/sequence-diagram',
  },

  PROMPT_TEMPLATE: {
    LIST: '/api/prompt-templates',
    SEARCH: '/api/prompt-templates/search',
    DETAIL: (id) => `/api/prompt-templates/${id}`,
    BY_CODE: (code) => `/api/prompt-templates/code/${code}`,
    CREATE: '/api/prompt-templates',
    UPDATE: (id) => `/api/prompt-templates/${id}`,
  },

  FUNCTION_PROMPT: {
    LIST: '/api/function-prompts',
    DETAIL: (functionCode) => `/api/function-prompts/${functionCode}`,
    SET: '/api/function-prompts',
    DELETE: (functionCode, promptCode) =>
      `/api/function-prompts/${functionCode}/${promptCode}`,
  },

  FUNCTION_CONFIGURATION: {
    LIST: '/api/function-configuration',
  },

  CACHE_MANAGE: {
    STATS: '/api/cache-manage/stats',
    DETAIL: (cacheName) => `/api/cache-manage/detail/${cacheName}`,
    CLEAR: (cacheName) => `/api/cache-manage/${cacheName}`,
    EVICT_KEY: (cacheName, key) => `/api/cache-manage/${cacheName}/key/${key}`,
  },

  TASK: {
    ADMIN_IMPORT: '/api/admin/task-batches/import',
    ADMIN_PUBLISH: '/api/admin/task-batches',
    ADMIN_BATCHES: '/api/admin/task-batches',
    ADMIN_BATCH_DETAIL: (batchId) => `/api/admin/task-batches/${batchId}`,
    ADMIN_UPDATE_ASSIGNEE: (batchId, taskId) =>
      `/api/admin/task-batches/${batchId}/tasks/${taskId}/assignee`,
    ADMIN_ASSIGNEES: '/api/admin/task-assignees',
    MY_BATCHES: '/api/tasks/my',
    MY_BATCH_DETAIL: (batchId) => `/api/tasks/my/${batchId}`,
    UPDATE_STATUS: (taskId) => `/api/tasks/${taskId}/status`,
  },

  REQUIREMENT_KNOWLEDGE: {
    UPSERT: '/api/requirement/knowledge/documents',
    SEARCH: '/api/requirement/knowledge/search',
  },

  SYS_SETTING: {
    DETAIL: (configKey) => `/api/settings/${encodeURIComponent(configKey)}`,
    ADMIN_LIST: '/api/admin/settings',
    ADMIN_UPSERT: (configKey) =>
      `/api/admin/settings/${encodeURIComponent(configKey)}`,
    ADMIN_DELETE: (configKey) =>
      `/api/admin/settings/${encodeURIComponent(configKey)}`,
  },
});

export default ENDPOINTS;
