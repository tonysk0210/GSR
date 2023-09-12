package com.hn2.util;

import org.springframework.stereotype.Component;

@Component
public class SqlStringHelper {
  /**
   * 建立分頁用raw sql<br>
   * 使用此種方式分頁須先下ORDER BY
   *
   * @param page 頁數
   * @param pageSize 分頁筆數
   * @return 分頁用raw sql字串
   */
  public String getPageSql(int page, int pageSize) {
    String sql;
    if (page <= 0) {
      sql = "";
    } else {
      int offset = (page - 1) * pageSize;
      sql = "OFFSET " + offset + " ROWS\n";
      sql += "FETCH NEXT " + pageSize + " ROWS ONLY\n";
    }

    return sql;
  }
}
