package com.hn2.cms.service.aca4001.erase.spi;

import java.util.List;
import java.util.Map;
import java.util.Set;

// 單表：選到哪些主鍵 ID，就處理該表的那些列
public interface EraseTarget {
    default String schema() {
        return "dbo"; // 預設 schema
    }

    String table(); // 目標表名（必實作）

    default String idColumn() {
        return "__PK__"; // 預設用查詢別名 __PK__ 當作主鍵欄位名
    }

    List<String> whitelistColumns(); // 要鏡像/清空/還原的欄位白名單

    default Set<String> dateColsNorm() {
        return java.util.Collections.emptySet(); // 需作日期正規化的欄位（大寫存）
    }

    default Set<String> intColsNorm() {
        return java.util.Collections.emptySet(); // 需作整數正規化的欄位（大寫存）
    }

    List<Map<String, Object>> loadRowsByIds(List<String> ids); // 依一組主鍵讀出資料（鏡像用）

    int nullifyAndMarkErased(List<String> ids); // 批次「清空欄位 + isERASE=1」

    int restoreFromRows(List<Map<String, Object>> rows, String operatorUserId); // 用鏡像回填

    default boolean isErased(String id) {
        return false; // 可選：查單筆是否已塗銷
    }
}