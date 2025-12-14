package com.excalicode.platform.web.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** 将 ProblemDetail 写入 HTTP 响应的工具，统一 401/403 等错误返回格式。 */
@Component
@RequiredArgsConstructor
public class ProblemDetailResponseWriter {

  private final ObjectMapper objectMapper;

  /**
   * 写出问题详情响应。
   *
   * @param response HTTP 响应对象
   * @param status HTTP 状态
   * @param message 错误提示
   * @param request 当前请求（可为空）
   */
  public void write(
      HttpServletResponse response, HttpStatus status, String message, HttpServletRequest request)
      throws IOException {
    ProblemDetail detail =
        ProblemDetail.forStatusAndDetail(
            status, StringUtils.hasText(message) ? message : status.getReasonPhrase());
    detail.setTitle(status.getReasonPhrase());
    detail.setProperty("timestamp", OffsetDateTime.now());
    if (request != null) {
      detail.setProperty("path", request.getRequestURI());
    }

    response.setStatus(status.value());
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    objectMapper.writeValue(response.getWriter(), detail);
  }
}
