package com.hn2.core.dto;

import lombok.Data;

/**
 * API 回應格式
 *
 * @author hsien
 */
@Data
public class ResponseMessage {
  /** 日期 */
  private String timestamp;
  /** 狀態 */
  private Integer status;
  /** 訊息 */
  private String message;
  /** 路徑 */
  private String path;
}
