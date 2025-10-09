package com.hn2.cms.service.aca4001.erase.spi;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ACA 塗銷/還原的目標表介面（SPI）。
 * 每個可被塗銷的資料表都寫一個 Adapter 實作本介面，供 GenericEraseService 呼叫。
 * 1) erase：依 ids 讀出資料（loadRowsByIds）→ 產生/加密鏡像 → upsert 到 ACA_EraseMirror → 將原表欄位置空/標記已塗銷（nullifyAndMarkErased）。
 * 2) restore：從鏡像解密出 rows → 呼叫 restoreFromRows 將資料寫回原表。
 * 契約要點：
 * - table() 必回傳資料表名；schema() 預設 "dbo"，跨 schema 可覆寫。
 * - idColumn() 預設 "__PK__" 只是佔位。
 * - whitelistColumns() 回傳允許鏡像/還原的欄位白名單（含必要的審計欄位）。
 * - loadRowsByIds() 回傳的每列 Map 必須包含 idColumn() 與白名單欄位。
 * - nullifyAndMarkErased()/restoreFromRows() 回傳受影響筆數（累計）。
 */

public interface EraseTarget {
    /**
     * Schema 名；預設 "dbo"。需要跨 schema 時覆寫。
     */
    default String schema() {
        return "dbo";
    }

    /**
     * 資料表名稱（必實作），例如 "CrmRec"、"ProRec"。
     */
    String table();

    /**
     * 主鍵欄位名稱。
     * 預設值 "__PK__" 僅為佔位，實務上請覆寫成實際主鍵欄位（如 "ID"）。
     * GenericEraseService 會用它從 row Map 取出主鍵（RowUtils.extractIdOrThrow(...)）。
     */
    default String idColumn() {
        return "__PK__";
    }

    /**
     * 被鏡像/還原的白名單欄位列表（不含敏感暫存欄位）。
     * loadRowsByIds() 應至少包含這些欄位，restoreFromRows() 也只應寫回這些欄位。
     */
    List<String> whitelistColumns();

    /**
     * 回傳需要「日期正規化」的欄位名集合（可空，預設空集合）。
     */
    default Set<String> dateColsNorm() {
        return java.util.Collections.emptySet();
    }

    /**
     * 回傳需要「整數正規化」的欄位名集合（可空，預設空集合）。
     */
    default Set<String> intColsNorm() {
        return java.util.Collections.emptySet();
    }

    /**
     * 依主鍵清單批次載入資料列。
     * 需求：每筆 Map 必須包含 idColumn() 與 whitelistColumns() 中的欄位。
     */
    List<Map<String, Object>> loadRowsByIds(List<String> ids);

    /**
     * 執行塗銷：將指定 ids 的欄位置空/標記（例如 IsErased=1、淨空個資欄位）。
     *
     * @return 受影響筆數（總和）
     */
    int nullifyAndMarkErased(List<String> ids);

    /**
     * 執行還原：把傳入 rows（從鏡像解密並驗證過）寫回原表。
     * rows 的每筆 Map 會至少包含 "schema"、"table"、"idColumn"、"id" 與原先 fields。
     *
     * @param operatorUserId 操作人（審計/最後異動者）
     * @return 受影響筆數（總和）
     */
    int restoreFromRows(List<Map<String, Object>> rows, String operatorUserId);
}