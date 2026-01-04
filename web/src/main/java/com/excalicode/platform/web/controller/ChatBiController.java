package com.excalicode.platform.web.controller;

import com.excalicode.platform.core.api.chatbi.ChatBiAskRequest;
import com.excalicode.platform.core.api.chatbi.ChatBiAskResponse;
import com.excalicode.platform.core.api.chatbi.ChatBiSessionDetailResponse;
import com.excalicode.platform.core.api.chatbi.ChatBiSessionSummaryResponse;
import com.excalicode.platform.core.entity.SysUser;
import com.excalicode.platform.core.exception.BusinessException;
import com.excalicode.platform.core.service.ChatBiService;
import com.excalicode.platform.core.service.entity.SysUserService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** ChatBI */
@RestController
@RequestMapping("/api/chatbi")
@RequiredArgsConstructor
public class ChatBiController {

  private final ChatBiService chatBiService;
  private final SysUserService sysUserService;

  /** 提问。 */
  @PostMapping("/ask")
  @PreAuthorize("hasAnyRole('USER','ADMIN')")
  public ResponseEntity<ChatBiAskResponse> ask(@RequestBody @Valid ChatBiAskRequest request) {
    SysUser currentUser = requireCurrentUser();
    return ResponseEntity.ok(chatBiService.ask(request, currentUser));
  }

  /** 当前用户会话列表。 */
  @GetMapping("/sessions")
  @PreAuthorize("hasAnyRole('USER','ADMIN')")
  public ResponseEntity<List<ChatBiSessionSummaryResponse>> listSessions() {
    SysUser currentUser = requireCurrentUser();
    return ResponseEntity.ok(chatBiService.listMySessions(currentUser));
  }

  /** 会话详情 */
  @GetMapping("/sessions/{sessionId}")
  @PreAuthorize("hasAnyRole('USER','ADMIN')")
  public ResponseEntity<ChatBiSessionDetailResponse> getSessionDetail(
      @PathVariable Long sessionId) {
    SysUser currentUser = requireCurrentUser();
    return ResponseEntity.ok(chatBiService.getSessionDetail(sessionId, currentUser));
  }

  private SysUser requireCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !StringUtils.hasText(authentication.getName())) {
      throw new BusinessException("未检测到登录用户");
    }
    SysUser user = sysUserService.findByUsername(authentication.getName());
    if (user == null) {
      throw new BusinessException("当前登录用户不存在");
    }
    return user;
  }
}
