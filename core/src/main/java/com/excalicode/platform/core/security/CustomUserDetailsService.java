package com.excalicode.platform.core.security;

import java.util.Collections;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.excalicode.platform.core.entity.SysUser;
import com.excalicode.platform.core.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;

/**
 * 自定义 UserDetailsService 实现 Spring Security 使用此服务加载用户信息
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final SysUserMapper sysUserMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 从数据库查询用户
        SysUser sysUser = sysUserMapper
                .selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));

        if (sysUser == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        // 构造 Spring Security 的 UserDetails 对象
        return User.builder().username(sysUser.getUsername()).password(sysUser.getPassword())
                .authorities(Collections
                        .singletonList(new SimpleGrantedAuthority("ROLE_" + sysUser.getRole())))
                .build();
    }
}
