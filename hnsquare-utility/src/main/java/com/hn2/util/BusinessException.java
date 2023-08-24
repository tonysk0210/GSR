package com.hn2.util;

/**
 * 業務邏輯例外
 *
 * @author hsien
 */
public class BusinessException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private String message = "未知的錯誤";

  private Integer statusCode;

  /**
   * 例外訊息Exception
   *
   * @param message 例外訊息
   */
  public BusinessException(String message) {
    super(message);
  }

  public BusinessException(ErrorType errorType) {
    super(errorType.getMessage());
    this.statusCode = errorType.getStatus();
  }

  public BusinessException(ErrorType errorType, String extraMessage) {
    super(extraMessage);
    this.statusCode = errorType.getStatus();
  }

  public Integer getStatusCode() {
    return this.statusCode;
  }
}
