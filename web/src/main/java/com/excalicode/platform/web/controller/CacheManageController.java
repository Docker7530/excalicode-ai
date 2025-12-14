package com.excalicode.platform.web.controller;

import com.excalicode.platform.core.api.cache.CacheDetailResponse;
import com.excalicode.platform.core.api.cache.CacheEntryDTO;
import com.excalicode.platform.core.api.cache.CacheStatsDTO;
import com.github.benmanes.caffeine.cache.Cache;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 缓存管理 Controller */
@Slf4j
@RestController
@RequestMapping("/api/cache-manage")
@RequiredArgsConstructor
public class CacheManageController {

  private static final int MAX_ENTRIES_TO_RETURN = 100;
  private final CacheManager cacheManager;

  /** 获取所有缓存的统计信息 */
  @GetMapping("/stats")
  public ResponseEntity<List<CacheStatsDTO>> getAllCacheStats() {
    log.info("查询所有缓存统计信息");
    List<CacheStatsDTO> statsList = new ArrayList<>();

    cacheManager
        .getCacheNames()
        .forEach(
            cacheName -> {
              if (cacheName != null) {
                org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
                if (cache instanceof CaffeineCache caffeineCache) {
                  CacheStatsDTO stats = buildCacheStats(cacheName, caffeineCache);
                  statsList.add(stats);
                }
              }
            });

    return ResponseEntity.ok(statsList);
  }

  /** 获取指定缓存的详细信息（包含条目） */
  @GetMapping("/detail/{cacheName}")
  public ResponseEntity<CacheDetailResponse> getCacheDetail(
      @PathVariable String cacheName, @RequestParam(defaultValue = "100") int limit) {
    log.info("查询缓存详情: {}, limit: {}", cacheName, limit);

    if (cacheName == null || cacheName.isBlank()) {
      return ResponseEntity.badRequest().build();
    }

    org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
    if (cache == null) {
      log.warn("缓存不存在: {}", cacheName);
      return ResponseEntity.notFound().build();
    }

    if (!(cache instanceof CaffeineCache caffeineCache)) {
      log.warn("缓存类型不支持: {}", cache.getClass());
      return ResponseEntity.badRequest().build();
    }

    CacheStatsDTO stats = buildCacheStats(cacheName, caffeineCache);
    List<CacheEntryDTO> entries =
        buildCacheEntries(caffeineCache, Math.min(limit, MAX_ENTRIES_TO_RETURN));

    long totalSize = caffeineCache.getNativeCache().estimatedSize();
    boolean truncated = totalSize > entries.size();

    CacheDetailResponse response =
        CacheDetailResponse.builder()
            .stats(stats)
            .entries(entries)
            .totalEntries(totalSize)
            .truncated(truncated)
            .build();

    return ResponseEntity.ok(response);
  }

  /** 清除指定缓存的所有内容 */
  @DeleteMapping("/{cacheName}")
  public ResponseEntity<Void> clearCache(@PathVariable String cacheName) {
    log.info("清除缓存: {}", cacheName);

    if (cacheName == null || cacheName.isBlank()) {
      return ResponseEntity.badRequest().build();
    }

    org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
    if (cache == null) {
      log.warn("缓存不存在: {}", cacheName);
      return ResponseEntity.notFound().build();
    }

    cache.clear();
    log.info("缓存已清除: {}", cacheName);
    return ResponseEntity.ok().build();
  }

  /** 清除指定缓存的指定键 */
  @DeleteMapping("/{cacheName}/key/{key}")
  public ResponseEntity<Void> evictCacheKey(
      @PathVariable String cacheName, @PathVariable String key) {
    log.info("清除缓存项: cache={}, key={}", cacheName, key);

    if (cacheName == null || cacheName.isBlank() || key == null) {
      return ResponseEntity.badRequest().build();
    }

    org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
    if (cache == null) {
      log.warn("缓存不存在: {}", cacheName);
      return ResponseEntity.notFound().build();
    }

    cache.evict(key);
    log.info("缓存项已清除: cache={}, key={}", cacheName, key);
    return ResponseEntity.ok().build();
  }

  /** 构建缓存统计信息 */
  private CacheStatsDTO buildCacheStats(String cacheName, CaffeineCache caffeineCache) {
    Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();
    com.github.benmanes.caffeine.cache.stats.CacheStats stats = nativeCache.stats();

    return CacheStatsDTO.builder()
        .cacheName(cacheName)
        .size(nativeCache.estimatedSize())
        .requestCount(stats.requestCount())
        .hitCount(stats.hitCount())
        .missCount(stats.missCount())
        .hitRate(stats.hitRate() * 100)
        .loadSuccessCount(stats.loadSuccessCount())
        .loadFailureCount(stats.loadFailureCount())
        .evictionCount(stats.evictionCount())
        .build();
  }

  /** 构建缓存条目列表 */
  private List<CacheEntryDTO> buildCacheEntries(CaffeineCache caffeineCache, int limit) {
    Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();

    return nativeCache.asMap().entrySet().stream()
        .limit(limit)
        .map(
            entry -> {
              Object key = entry.getKey();
              Object value = entry.getValue();

              return CacheEntryDTO.builder()
                  .key(String.valueOf(key))
                  .value(formatValue(value))
                  .valueType(value.getClass().getSimpleName())
                  .build();
            })
        .collect(Collectors.toList());
  }

  /** 格式化缓存值以便显示 */
  private String formatValue(Object value) {
    if (value == null) {
      return "null";
    }

    String str = value.toString();
    if (str.length() > 500) {
      return str.substring(0, 500) + "...";
    }
    return str;
  }
}
