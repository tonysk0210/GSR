package com.hn2.core.payload;

import javax.validation.Valid;
import lombok.Data;

/**
 * 前端payload 範本
 *
 * @param <T> 業務邏輯用 自訂payload型態
 */
@Data
public class GeneralPayload<T> {
  /** 業務邏輯資料 */
  @Valid T data;
  /** 分頁資訊 */
  PagePayload page;
}
