package com.hn2.cms.service.aca4001.erase.spi;

import com.hn2.cms.service.aca4001.erase.support.RowUtils;

import java.util.List;
import java.util.Map;

/**
 * 以 Sql2o 實作的塗銷/還原共用基底。
 * 提供：
 * - buildSelectCols()：依 white-list 動態產生 SELECT 欄位，並把主鍵 ID 映射成 "__PK__"
 * - fetchRowsByIdsChunked()：ID 清單分批 IN 查詢，避免一次參數過多
 * - updateWithDynamicSet()：依 Map 動態組 UPDATE SET 子句，並支援額外後綴（含 ${NOW} 置換）與自訂綁參
 * 典型用法：各表的 EraseTarget 實作可繼承本類別來撰寫 load/erase/restore。
 */
public abstract class AbstractSql2oTarget implements EraseTarget {
    protected final org.sql2o.Sql2o sql2o;

    protected AbstractSql2oTarget(org.sql2o.Sql2o sql2o) {
        this.sql2o = sql2o;
    }

    /**
     * 產生 SELECT 欄位清單。
     * - 若 whitelistColumns() 為空：只選 ID 並別名成 "__PK__"
     * - 否則：選 "ID AS __PK__" + 以逗號串接的白名單欄位
     * 目的：讓上層以統一鍵 "__PK__" 取主鍵值（配合 RowUtils.extractIdOrThrow）。
     */
    protected String buildSelectCols() {
        var white = whitelistColumns();
        return (white == null || white.isEmpty())
                ? "ID AS __PK__"
                : "ID AS __PK__," + String.join(",", white);
    }

    /**
     * 將 IDs 依 chunkSize 分批，使用 IN (:ids) 查詢，彙整成 List<Map<String,Object>>。
     * - schema().table() 由子類別提供
     * - Map key 為欄位名；含 "__PK__" 與白名單欄位
     * - 以 sql2o 的 Table → Row.asMap() 做映射
     * 回傳：所有批次的合併結果；ids 為空則回空清單。
     */
    protected List<Map<String, Object>> fetchRowsByIdsChunked(List<String> ids, int chunkSize) {
        if (ids == null || ids.isEmpty()) return java.util.Collections.emptyList();
        final String sql = "SELECT " + buildSelectCols() + " FROM " + schema() + "." + table() + " WHERE ID IN (:ids)";
        var all = new java.util.ArrayList<Map<String, Object>>();
        for (int i = 0; i < ids.size(); i += chunkSize) {
            var sub = ids.subList(i, Math.min(i + chunkSize, ids.size()));
            try (var con = sql2o.open()) {
                var t = con.createQuery(sql).addParameter("ids", sub).executeAndFetchTable();
                all.addAll(t.rows().stream().map(org.sql2o.data.Row::asMap)
                        .collect(java.util.stream.Collectors.toList()));
            }
        }
        return all;
    }

    /**
     * 依 cleaned（欄位→值）動態產生 UPDATE SET 子句並更新單筆 ID。
     * - extraSetSuffix：可附加額外的 SET 片段（可含前導逗號），其中的 ${NOW} 會被替換為
     * SYSDATETIME()（本地）或 SYSUTCDATETIME()（UTC），由 useUtcNow 決定
     * - binder：呼叫端補充綁定額外參數（如 :uid）
     * - 若 cleaned 與 suffix 都為空，直接回傳 0（不執行 UPDATE）
     * WHERE 子句：`WHERE ID=:id AND ISNULL(isERASE,0)=1`（僅在已標記塗銷時才允許更新）
     * 回傳：受影響筆數。
     */
    protected int updateWithDynamicSet(String id, Map<String, Object> cleaned, String extraSetSuffix, java.util.function.Function<org.sql2o.Query, org.sql2o.Query> binder) {
        // ---- 1) 統一決定時間來源（true=UTC / false=本地）----
        final boolean useUtcNow = false; // ★ 想存本地改成 false
        final String nowExpr = useUtcNow ? "SYSUTCDATETIME()" : "SYSDATETIME()";

        // ---- 2) 先把 extra 後綴的 ${NOW} 取代掉 ----
        String suffix = (extraSetSuffix == null) ? "" : extraSetSuffix.replace("${NOW}", nowExpr);

        // ---- 3) 動態組 SET 子句（可能為空）----
        StringBuilder set = new StringBuilder();
        int i = 0;
        for (var e : cleaned.entrySet()) {
            if (i++ > 0) set.append(", ");
            set.append("[")
                    .append(e.getKey())
                    .append("] = :")
                    .append(RowUtils.paramName(e.getKey()));
        }

        // ---- 4) 合併 suffix；若 cleaned 為空，移除 suffix 的前導逗號與空白 ----
        if (!suffix.isEmpty()) {
            if (set.length() == 0) {
                // 沒有任何欄位要 SET，只能靠 suffix；把開頭形如 ", " 或 "," 去掉
                String trimmedSuffix = suffix;
                while (trimmedSuffix.startsWith(",") || trimmedSuffix.startsWith(" ")) {
                    trimmedSuffix = trimmedSuffix.substring(1);
                }
                set.append(trimmedSuffix);
            } else {
                set.append(suffix);
            }
        }

        // ---- 5) 防呆：到這裡如果還是空，代表真的沒有任何可更新欄位，直接 return 0 ----
        if (set.length() == 0) {
            return 0;
        }

        String sql = "UPDATE " + schema() + "." + table() + " SET " + set + " WHERE ID=:id AND ISNULL(isERASE,0)=1";
        try (var con = sql2o.open()) {
            var q = con.createQuery(sql).addParameter("id", id);
            for (var e : cleaned.entrySet()) {
                q.addParameter(RowUtils.paramName(e.getKey()), e.getValue());
            }
            q = binder.apply(q);
            return q.executeUpdate().getResult();
        }
    }
}
