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
    FUNCTION_TYPES: '/api/ai-function/function-types',
    LIST: '/api/ai-function/list',
    SET: '/api/ai-function/set',
    DETAIL: (id) => `/api/ai-function/${id}`,
    CLEAR_CACHE: '/api/ai-function/clear-cache',
  },

  REQUIREMENT: {
    ENHANCE: '/api/requirement/enhance',
    BREAKDOWN: '/api/process/breakdown',
    ANALYZE: '/api/cosmic/analyze',
    ANALYZE_V2: '/api/cosmic/analyze-v2',
    IMPORT_PROCESSES: '/api/cosmic/process/import',
    IMPORT_COSMIC_PROCESSES: '/api/cosmic/subprocess/import',
    EXPORT_TABLE: '/api/cosmic/table/export',
    GENERATE_DOCUMENT: '/api/cosmic/documents/preview',
    EXPORT_DOCUMENT: '/api/cosmic/documents/export',
  },
});

export default ENDPOINTS;
