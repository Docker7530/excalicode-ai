package com.excalicode.platform.core.service.entity.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.excalicode.platform.core.entity.SysUser;
import com.excalicode.platform.core.mapper.SysUserMapper;
import com.excalicode.platform.core.service.entity.SysUserService;
import org.springframework.stereotype.Service;

/** 系统用户 Service 实现 */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser>
    implements SysUserService {

  @Override
  public boolean existsByUsername(String username) {
    return lambdaQuery().eq(SysUser::getUsername, username).exists();
  }

  @Override
  public SysUser findByUsername(String username) {
    return lambdaQuery().eq(SysUser::getUsername, username).one();
  }
}
