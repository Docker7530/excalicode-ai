package com.excalicode.platform.web.security.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/** 统一处理已认证用户访问受限资源的场景，返回 JSON 403。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

  private final ProblemDetailResponseWriter responseWriter;

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException {
    Principal principal = request.getUserPrincipal();
    log.warn(
        "无权限访问 user={} path={} reason={}",
        principal != null ? principal.getName() : "anonymous",
        request.getRequestURI(),
        accessDeniedException.getMessage());
    responseWriter.write(response, HttpStatus.FORBIDDEN, "暂无权限执行该操作。", request);
  }
}
