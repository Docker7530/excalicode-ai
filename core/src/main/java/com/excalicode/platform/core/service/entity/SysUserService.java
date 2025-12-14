package com.excalicode.platform.core.service.entity;

import com.baomidou.mybatisplus.extension.service.IService;
import com.excalicode.platform.core.entity.SysUser;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 系统用户 Service */
public interface SysUserService extends IService<SysUser> {

  /**
   * 判断用户名是否已存在
   *
   * @param username 用户名
   * @return true 已存在; false 不存在
   */
  boolean existsByUsername(String username);

  /**
   * 根据用户名查询用户
   *
   * @param username 用户名
   * @return 用户信息
   */
  SysUser findByUsername(String username);

  /**
   * 根据 ID 列表查询用户并返回 Map
   *
   * @param ids 用户ID集合
   * @return map
   */
  default Map<Long, SysUser> findByIdsAsMap(Collection<Long> ids) {
    if (ids == null || ids.isEmpty()) {
      return Map.of();
    }
    return listByIds(ids).stream().collect(Collectors.toMap(SysUser::getId, Function.identity()));
  }
}
