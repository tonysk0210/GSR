package com.hn2.cms.service.aca4001.erase.spi;

import java.util.List;
import java.util.Map;
import java.util.Set;

// 單表：選到哪些主鍵 ID，就處理該表的那些列
public interface EraseTarget {
    String schema();                 // e.g. "dbo"

    String table();                  // e.g. "CrmRec"

    String idColumn();               // e.g. "ID"

    List<String> whitelistColumns(); // 要鏡像/清空/還原的欄位

    Set<String> dateColsNorm();      // 規範化後的日期欄位（同你原本處理）

    Set<String> intColsNorm();       // 規範化後的整數欄位

    // 讀取要鏡像的資料（以主鍵 ID 為準）
    List<Map<String, Object>> loadRowsByIds(List<String> ids);

    // 清空白名單欄位 + 設 isERASE=1（必要時也會把 CreatedByUserID/ModifiedByUserID 指定成 -2）
    int nullifyAndMarkErased(List<String> ids);

    // 還原：把鏡像 rows 回寫 + 設 isERASE=0（並固定覆寫 ModifiedOnDate/ModifiedByUserID）
    int restoreFromRows(List<Map<String, Object>> rows, String operatorUserId);

    // 查 isERASE 狀態（可批次或單筆）
    boolean isErased(String id);
}
