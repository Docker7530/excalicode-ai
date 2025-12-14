package com.excalicode.platform.web.controller;

import com.excalicode.platform.core.api.redis.RedisKeyValueResponse;
import com.excalicode.platform.core.service.RedisAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Redis 管理控制器，仅提供最基础的查询与删除能力，便于首页自检 */
@Slf4j
@RestController
@RequestMapping("/api/redis")
@RequiredArgsConstructor
public class RedisAdminController {

  private final RedisAdminService redisAdminService;

  /** 根据 key 查询 Redis 内容 */
  @GetMapping("/key")
  public ResponseEntity<RedisKeyValueResponse> getKey(@RequestParam("key") String key) {
    if (!StringUtils.hasText(key)) {
      return ResponseEntity.badRequest().build();
    }
    log.info("查询 Redis 键: {}", key);
    RedisKeyValueResponse response = redisAdminService.getKeyDetail(key);
    if (!response.isExists()) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(response);
  }

  /** 删除指定 key */
  @DeleteMapping("/key")
  public ResponseEntity<Void> deleteKey(@RequestParam("key") String key) {
    if (!StringUtils.hasText(key)) {
      return ResponseEntity.badRequest().build();
    }
    log.info("删除 Redis 键: {}", key);
    boolean deleted = redisAdminService.deleteKey(key);
    if (!deleted) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok().build();
  }
}
