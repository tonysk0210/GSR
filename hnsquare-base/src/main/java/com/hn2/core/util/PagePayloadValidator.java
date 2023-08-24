package com.hn2.core.util;

import com.hn2.core.payload.PagePayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PagePayloadValidator {
  /**
   * 檢查分頁payload完整性
   *
   * @param pagePayload 分頁payload
   * @return true or false
   */
  public boolean validatePagePayload(PagePayload pagePayload) {
    boolean valid = true;
    // null check
    if (pagePayload != null) {
      // 檢查分頁筆數
      Integer pageItems = pagePayload.getPageSize();
      if (pageItems == null || pageItems < 1) {
        valid = false;
      }
      // 檢查頁數
      Integer page = pagePayload.getPage();
      if (page == null || page < 1) {
        valid = false;
      }
    } else {
      valid = false;
    }
    return valid;
  }

  /** 檢查是否有該分頁資料 */
  public boolean checkPageExist(PagePayload pagePayload, int totalDataSize) {

    int offset = (pagePayload.getPage() - 1) * pagePayload.getPageSize();
    boolean check = (offset < totalDataSize) || (pagePayload.getPage() == 1 && totalDataSize == 0);
    return check;
  }
}
