package com.hn2.core.payload;

import javax.validation.constraints.Min;
import lombok.Data;

@Data
public class PagePayload {
  /** 查詢頁數 */
  @Min(1)
  private Integer page;
  /** 每頁筆數 */
  @Min(1)
  private Integer pageSize;
}
