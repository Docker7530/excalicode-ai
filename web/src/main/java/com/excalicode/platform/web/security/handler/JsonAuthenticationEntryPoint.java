package com.excalicode.platform.web.security.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/** 统一处理未认证访问的入口，返回 JSON 401。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final ProblemDetailResponseWriter responseWriter;

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {
    log.warn("未认证访问被拒绝 path={} reason={}", request.getRequestURI(), authException.getMessage());
    responseWriter.write(response, HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录。", request);
  }
}
