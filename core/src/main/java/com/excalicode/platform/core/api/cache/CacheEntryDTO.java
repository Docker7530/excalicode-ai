package com.excalicode.platform.core.api.cache;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 缓存条目 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheEntryDTO {

  /** 缓存键 */
  private String key;

  /** 缓存值（序列化为字符串） */
  private String value;

  /** 值的类型 */
  private String valueType;
}
