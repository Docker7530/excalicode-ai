import { ENDPOINTS } from './endpoints.js';
import api from './request.js';

/**
 * 缓存管理 API
 */

/**
 * 获取所有缓存的统计信息
 */
export function getCacheStats() {
  return api.get(ENDPOINTS.CACHE_MANAGE.STATS);
}

/**
 * 获取指定缓存的详细信息
 */
export function getCacheDetail(cacheName, limit = 100) {
  return api.get(ENDPOINTS.CACHE_MANAGE.DETAIL(cacheName), {
    params: { limit },
  });
}

/**
 * 清除指定缓存的所有内容
 */
export function clearCache(cacheName) {
  return api.delete(ENDPOINTS.CACHE_MANAGE.CLEAR(cacheName));
}

/**
 * 清除指定缓存的指定键
 */
export function evictCacheKey(cacheName, key) {
  return api.delete(ENDPOINTS.CACHE_MANAGE.EVICT_KEY(cacheName, key));
}
