package com.excalicode.platform.core.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.excalicode.platform.core.entity.SysUser;

/**
 * 系统用户 Service
 */
public interface SysUserService
        extends IService<SysUser> {

    /**
     * 判断用户名是否已存在
     *
     * @param username 用户名
     * @return true 已存在; false 不存在
     */
    boolean existsByUsername(String username);
}
