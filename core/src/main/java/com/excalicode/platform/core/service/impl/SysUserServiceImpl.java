package com.excalicode.platform.core.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.excalicode.platform.core.entity.SysUser;
import com.excalicode.platform.core.mapper.SysUserMapper;
import com.excalicode.platform.core.service.SysUserService;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 系统用户 Service 实现
 */
@Service
public class SysUserServiceImpl
        extends ServiceImpl<SysUserMapper, SysUser>
        implements SysUserService {

    @Override
    public Optional<SysUser> findByUsername(String username) {
        return Optional.ofNullable(lambdaQuery().eq(SysUser::getUsername, username).one());
    }

    @Override
    public boolean existsByUsername(String username) {
        return lambdaQuery().eq(SysUser::getUsername, username).exists();
    }
}
