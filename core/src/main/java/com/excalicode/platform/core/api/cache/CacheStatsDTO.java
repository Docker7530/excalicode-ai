package com.excalicode.platform.core.api.cache;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 缓存统计信息 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheStatsDTO {

  /** 缓存名称 */
  private String cacheName;

  /** 缓存大小（当前条目数） */
  private Long size;

  /** 请求次数 */
  private Long requestCount;

  /** 命中次数 */
  private Long hitCount;

  /** 未命中次数 */
  private Long missCount;

  /** 命中率（百分比） */
  private Double hitRate;

  /** 加载成功次数 */
  private Long loadSuccessCount;

  /** 加载失败次数 */
  private Long loadFailureCount;

  /** 驱逐次数 */
  private Long evictionCount;
}
