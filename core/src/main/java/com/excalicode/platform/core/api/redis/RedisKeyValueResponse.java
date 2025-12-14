package com.excalicode.platform.core.api.redis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Redis 键值响应 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedisKeyValueResponse {

  /** 键名称 */
  private String key;

  /** 键对应的值，字符串形式 */
  private String value;

  /** 剩余过期时间（秒），若永久不过期则为 null */
  private Long ttlSeconds;

  /** 键是否存在 */
  private boolean exists;
}
