package com.hn2.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataDto<T> {
  /** 業務資料 */
  private T data;

  /** 分頁資訊 */

  @Builder.Default
  private PageInfo page = PageInfo.builder().build();

  /** 回應資訊 */
  @Builder.Default
  private ResponseInfo response = ResponseInfo.builder().build();

  public DataDto(T data,ResponseInfo response) {
        this.data = data;
        this.response = response;
  }
}
