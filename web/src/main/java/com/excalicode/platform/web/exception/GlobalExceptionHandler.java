package com.excalicode.platform.web.exception;

import java.time.OffsetDateTime;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.excalicode.platform.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String TRACE_ID_KEY = "traceId";

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusinessException(BusinessException e) {
        log.error("业务异常: {}", e.getMessage());
        return createProblemDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /**
     * 处理所有未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception e) {
        log.error("系统异常", e);
        String message = StringUtils.hasText(e.getMessage()) ? e.getMessage() : "系统内部错误";
        return createProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }

    private ProblemDetail createProblemDetail(HttpStatus status, String message) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status,
                StringUtils.hasText(message) ? message : status.getReasonPhrase());
        detail.setTitle(status.getReasonPhrase());
        detail.setProperty("timestamp", OffsetDateTime.now());
        String traceId = MDC.get(TRACE_ID_KEY);
        if (StringUtils.hasText(traceId)) {
            detail.setProperty(TRACE_ID_KEY, traceId);
        }
        return detail;
    }

}
