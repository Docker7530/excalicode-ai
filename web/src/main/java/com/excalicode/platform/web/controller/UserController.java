package com.excalicode.platform.web.controller;

import com.excalicode.platform.core.api.user.SysUserResponse;
import com.excalicode.platform.core.api.user.UserCreateRequest;
import com.excalicode.platform.core.api.user.UserUpdateRequest;
import com.excalicode.platform.core.entity.SysUser;
import com.excalicode.platform.core.exception.BusinessException;
import com.excalicode.platform.core.service.entity.SysUserService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 管理员用户管理接口 */
@Validated
@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserController {

  private final SysUserService sysUserService;
  private final PasswordEncoder passwordEncoder;

  /**
   * 查询所有用户
   *
   * @return 用户列表
   */
  @GetMapping
  public ResponseEntity<List<SysUserResponse>> listUsers() {
    List<SysUserResponse> responses = sysUserService.list().stream().map(this::toResponse).toList();
    return ResponseEntity.ok(responses);
  }

  /**
   * 新增用户
   *
   * @param request 创建请求
   * @return 创建后的用户信息
   */
  @PostMapping
  public ResponseEntity<SysUserResponse> createUser(@RequestBody @Valid UserCreateRequest request) {
    if (sysUserService.existsByUsername(request.getUsername())) {
      throw new BusinessException("用户名已存在");
    }

    SysUser user = new SysUser();
    user.setUsername(request.getUsername());
    user.setPassword(passwordEncoder.encode(request.getPassword()));
    user.setRole(request.getRole());

    sysUserService.save(user);

    SysUser saved = sysUserService.getById(user.getId());
    return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
  }

  /**
   * 更新用户
   *
   * @param id 用户ID
   * @param request 更新请求
   * @return 更新后的用户信息
   */
  @PutMapping("/{id}")
  public ResponseEntity<SysUserResponse> updateUser(
      @PathVariable Long id, @RequestBody @Valid UserUpdateRequest request) {
    SysUser existing = sysUserService.getById(id);
    if (existing == null) {
      throw new BusinessException("用户名已存在");
    }

    boolean usernameChanged = !existing.getUsername().equals(request.getUsername());
    if (usernameChanged
        && sysUserService
            .lambdaQuery()
            .eq(SysUser::getUsername, request.getUsername())
            .ne(SysUser::getId, id)
            .exists()) {
      throw new BusinessException("用户名已存在");
    }

    existing.setUsername(request.getUsername());
    existing.setRole(request.getRole());
    if (request.getPassword() != null && !request.getPassword().isBlank()) {
      existing.setPassword(passwordEncoder.encode(request.getPassword()));
    }

    sysUserService.updateById(existing);

    SysUser updated = sysUserService.getById(id);
    return ResponseEntity.ok(toResponse(updated));
  }

  /**
   * 删除用户
   *
   * @param id 用户ID
   * @return 删除结果
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
    SysUser existing = sysUserService.getById(id);
    if (existing == null) {
      throw new BusinessException("用户不存在");
    }

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String currentUsername = authentication != null ? authentication.getName() : null;
    if (existing.getUsername().equals(currentUsername)) {
      throw new BusinessException("不能删除当前登录用户");
    }

    sysUserService.removeById(id);
    return ResponseEntity.noContent().build();
  }

  private SysUserResponse toResponse(SysUser user) {
    SysUserResponse response = new SysUserResponse();
    response.setId(user.getId());
    response.setUsername(user.getUsername());
    response.setRole(user.getRole());
    response.setCreatedTime(user.getCreatedTime());
    response.setUpdatedTime(user.getUpdatedTime());
    return response;
  }
}
