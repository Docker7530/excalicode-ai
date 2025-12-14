package com.excalicode.platform.core.service;

import com.excalicode.platform.core.api.redis.RedisKeyValueResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Redis 管理服务，提供最基础的键查询与删除功能 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisAdminService {

  private final StringRedisTemplate stringRedisTemplate;

  /**
   * 查询指定键的内容及 TTL
   *
   * @param key 要查询的键
   * @return 键的详细信息
   */
  public RedisKeyValueResponse getKeyDetail(String key) {
    String actualKey = normalizeKey(key);
    try {
      boolean exists = Boolean.TRUE.equals(stringRedisTemplate.hasKey(actualKey));
      String value = exists ? stringRedisTemplate.opsForValue().get(actualKey) : null;
      Long ttl = exists ? stringRedisTemplate.getExpire(actualKey) : null;
      Long ttlSeconds = ttl == null || ttl < 0 ? null : ttl;

      return RedisKeyValueResponse.builder()
          .key(actualKey)
          .value(value)
          .ttlSeconds(ttlSeconds)
          .exists(exists)
          .build();
    } catch (DataAccessException ex) {
      log.error("查询 Redis 键失败: {}", actualKey, ex);
      throw ex;
    }
  }

  /**
   * 删除指定键
   *
   * @param key 要删除的键
   * @return true 表示删除成功
   */
  public boolean deleteKey(String key) {
    String actualKey = normalizeKey(key);
    try {
      return Boolean.TRUE.equals(stringRedisTemplate.delete(actualKey));
    } catch (DataAccessException ex) {
      log.error("删除 Redis 键失败: {}", actualKey, ex);
      throw ex;
    }
  }

  /** 统一处理键的合法性 */
  private String normalizeKey(String key) {
    if (!StringUtils.hasText(key)) {
      throw new IllegalArgumentException("Redis key 不能为空");
    }
    return key.trim();
  }
}
