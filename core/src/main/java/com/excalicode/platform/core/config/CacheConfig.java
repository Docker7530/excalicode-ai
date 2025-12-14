package com.excalicode.platform.core.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.List;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/** 缓存配置类 使用 Caffeine 作为本地缓存实现。 */
@Configuration
public class CacheConfig {

  /** 缓存名称常量。 */
  public static final String PROMPTS_CACHE = "prompts";

  public static final String AI_FUNCTION_CONFIGS_CACHE = "aiFunctionConfigs";

  public static final String SYS_SETTINGS_CACHE = "sysSettings";

  /** 配置 Caffeine 缓存管理器 */
  @Bean
  @Primary
  CacheManager cacheManager() {
    SimpleCacheManager cacheManager = new SimpleCacheManager();
    cacheManager.setCaches(
        List.of(
            buildCache(PROMPTS_CACHE, 100, Duration.ofHours(24)),
            buildCache(AI_FUNCTION_CONFIGS_CACHE, 100, Duration.ofHours(1)),
            buildCache(SYS_SETTINGS_CACHE, 200, Duration.ofHours(6))));
    return cacheManager;
  }

  private Cache buildCache(String name, int maximumSize, Duration expireAfterWrite) {
    return new CaffeineCache(
        name,
        Caffeine.newBuilder()
            .maximumSize(maximumSize)
            .expireAfterWrite(expireAfterWrite)
            .expireAfterAccess(Duration.ofHours(2))
            .recordStats()
            .build());
  }
}
