package com.excalicode.platform.core.api.cache;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 缓存详情响应 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheDetailResponse {

  /** 缓存统计信息 */
  private CacheStatsDTO stats;

  /** 缓存条目列表 */
  private List<CacheEntryDTO> entries;

  /** 总条目数 */
  private Long totalEntries;

  /** 是否被截断（条目过多时只返回部分） */
  private Boolean truncated;
}
