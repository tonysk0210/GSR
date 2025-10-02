package com.hn2.cms.service.aca4001.erase.spi;

import java.util.List;
import java.util.Map;
import java.util.Set;

// 依附表：用父鍵（例如 ProRecID）去清空該表所有符合的多筆列
public interface DependentEraseTarget {
    String schema();
    String table();
    String parentColumn();           // e.g. "ProRecID"
    String idColumn();               // 該表主鍵欄位（如 "ID"）
    List<String> whitelistColumns();
    Set<String> dateColsNorm();
    Set<String> intColsNorm();

    // 讀取要鏡像的資料（用父鍵清單抓出多筆）
    List<Map<String,Object>> loadRowsByParentIds(List<String> parentIds);

    // 清空白名單欄位 + 設 isERASE=1（以父鍵批次處理）
    int nullifyAndMarkErasedByParent(List<String> parentIds);

    // 還原：把鏡像 rows 回寫 + 設 isERASE=0
    int restoreFromRows(List<Map<String,Object>> rows, String operatorUserId);
}