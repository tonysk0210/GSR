package com.hn2.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PageInfo {
  /** 總頁數 */
  private Integer totalPages;
  /** 當前頁數 */
  private Integer currentPage;
  /** 每頁筆數 */
  private Integer pageItems;
  /**
   * 總筆數
   */
  private Long totalDatas;
}
