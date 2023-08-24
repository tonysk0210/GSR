package com.hn2.core.dto;

import lombok.Data;

/**
 * 登入資訊
 *
 * @author david
 */
@Data
public class AuthenticateDto {
  /** 回傳狀態代碼 */
  private String status;
  /** 回傳狀態說明 */
  private String msg;
  /** 查詢資料日期 */
  private String queryDate;
  /** 使用者帳號 */
  private String userId;
  /** 機關ID */
  private String organId;
  /** tokenID */
  private String tokenId;
}
