package com.excalicode.platform.core.exception;

/** 业务异常 */
public class BusinessException extends RuntimeException {

  /**
   * 构造函数。
   *
   * @param message 异常提示信息
   */
  public BusinessException(String message) {
    super(message);
  }

  /**
   * 构造函数。
   *
   * @param message 异常提示信息
   * @param cause 原始异常
   */
  public BusinessException(String message, Throwable cause) {
    super(message, cause);
  }
}
