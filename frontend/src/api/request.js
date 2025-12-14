/**
 * Axios 客户端配置
 * 统一输出后端返回的数据部分，错误统一适配 ProblemDetail
 */
import router from '@/router';
import axios from 'axios';
import { API_BASE_URL } from './endpoints.js';

const REQUEST_TIMEOUT = 600000;

const DEFAULT_HEADERS = {
  'Content-Type': 'application/json;charset=UTF-8',
  Accept: 'application/json',
};

const request = axios.create({
  baseURL: API_BASE_URL,
  timeout: REQUEST_TIMEOUT,
  headers: DEFAULT_HEADERS,
});

/**
 * 判断当前响应是否为二进制流，以便保留原始 response
 */
const isBinaryResponse = (response) => {
  const contentType = response.headers?.['content-type'] ?? '';
  return (
    response.config.responseType === 'blob' ||
    contentType.includes('application/octet-stream') ||
    contentType.includes('application/vnd.openxmlformats')
  );
};

/**
 * 将 Blob 结果解析为对象或兼容格式
 */
const normalizeBlobPayload = async (blob) => {
  try {
    const text = await blob.text();
    return JSON.parse(text);
  } catch (_error) {
    try {
      const text = await blob.text();
      return text ? { detail: text } : null;
    } catch {
      return null;
    }
  }
};

/**
 * 统一解析 ProblemDetail 或字符串错误信息
 */
const resolveProblemDetail = async (payload) => {
  if (payload instanceof Blob) {
    return normalizeBlobPayload(payload);
  }
  return payload;
};

/**
 * 填充错误对象上的易读信息
 */
const attachProblemDetail = (error, problem, response) => {
  if (problem && typeof problem === 'object') {
    const detail = problem.detail || problem.message || problem.title;
    const title = problem.title || response.statusText;

    error.message = detail || title || `请求失败（${response.status}）`;
    error.problemDetail = {
      title,
      detail,
      status: problem.status ?? response.status,
      traceId: problem.traceId,
      timestamp: problem.timestamp,
    };
    return;
  }

  if (typeof problem === 'string' && problem.trim()) {
    error.message = problem.trim();
    return;
  }

  error.message = error?.message || `请求失败（${response.status}）`;
};

const handleResponseSuccess = (response) => {
  if (isBinaryResponse(response)) {
    // 返回完整 response 以便调用方继续读取 headers
    return response;
  }

  return response.data;
};

const handleResponseError = async (error) => {
  if (error?.code === 'ECONNABORTED' || error?.message?.includes('timeout')) {
    error.message = '请求超时，请稍后再试';
    return Promise.reject(error);
  }

  const { response } = error;

  if (!response) {
    error.message = error?.message || '网络异常，请检查连接';
    return Promise.reject(error);
  }

  const requestConfig = error.config || response.config;
  const skipAuthRedirect = requestConfig?.skipAuthRedirect;

  // 401 未授权 - 跳转登录页
  if (response.status === 401) {
    if (skipAuthRedirect) {
      return Promise.reject(error);
    }

    ElMessage.error('登录已过期，请重新登录');
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    localStorage.removeItem('role');

    if (router.currentRoute.value.path !== '/login') {
      router.push('/login');
    }

    return Promise.reject(new Error('未授权，请重新登录'));
  }

  const problem = await resolveProblemDetail(response.data);
  attachProblemDetail(error, problem, response);

  return Promise.reject(error);
};

// 请求拦截器 - 添加 JWT Token
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error),
);

// 响应拦截器
request.interceptors.response.use(handleResponseSuccess, (error) =>
  handleResponseError(error),
);

/**
 * 轻量级 HTTP 包装，默认返回 data 字段
 */
const api = {
  /**
   * 发送 GET 请求
   */
  get(url, params = {}, config = {}) {
    return request.get(url, { params, ...config });
  },
  /**
   * 发送 POST 请求
   */
  post(url, data = {}, config = {}) {
    return request.post(url, data, config);
  },
  /**
   * 发送 PUT 请求
   */
  put(url, data = {}, config = {}) {
    return request.put(url, data, config);
  },
  /**
   * 发送 PATCH 请求
   */
  patch(url, data = {}, config = {}) {
    return request.patch(url, data, config);
  },
  /**
   * 发送 DELETE 请求
   */
  delete(url, params = {}, config = {}) {
    return request.delete(url, { params, ...config });
  },
};

export default api;
export { request };
