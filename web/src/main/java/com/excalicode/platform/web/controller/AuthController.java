package com.excalicode.platform.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.excalicode.platform.core.api.auth.LoginRequest;
import com.excalicode.platform.core.api.auth.LoginResponse;
import com.excalicode.platform.core.api.auth.SessionValidationResponse;
import com.excalicode.platform.core.entity.SysUser;
import com.excalicode.platform.core.mapper.SysUserMapper;
import com.excalicode.platform.core.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 认证控制器 - 处理登录请求 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final SysUserMapper sysUserMapper;
  private final PasswordEncoder passwordEncoder;
  private final JwtUtil jwtUtil;

  /**
   * 用户登录
   *
   * @param loginRequest 登录请求
   * @return 登录响应(包含 JWT Token)
   */
  @PostMapping("/login")
  public ResponseEntity<Object> login(@RequestBody LoginRequest loginRequest) {
    // 查询用户
    SysUser user =
        sysUserMapper.selectOne(
            new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, loginRequest.getUsername()));

    // 验证用户存在且密码正确
    if (user == null || !passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
      return ResponseEntity.badRequest().body("用户名或密码错误");
    }

    // 生成 Token
    String token = jwtUtil.generateToken(user.getUsername());

    log.info("用户登录成功: {}", user.getUsername());

    return ResponseEntity.ok(new LoginResponse(token, user.getUsername(), user.getRole()));
  }

  /**
   * 校验当前登录态
   *
   * @param authentication Spring Security 认证信息
   * @return 当前登录用户信息
   */
  @GetMapping("/session")
  public ResponseEntity<SessionValidationResponse> validateSession(Authentication authentication) {
    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    String username = authentication.getName();

    SysUser user =
        sysUserMapper.selectOne(
            new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));

    if (user == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    return ResponseEntity.ok(new SessionValidationResponse(user.getUsername(), user.getRole()));
  }
}
