package com.hn2.cms.service.aca4001.erase.support;

import java.util.Map;

/**
 * 以 Map<String,Object> 表示一列資料時的輔助工具。
 * 功能：
 * - 大小寫不敏感的取值（getCI / toStringCI）
 * - 將欄位名轉成安全的 SQL 參數名稱（paramName）
 * - 將欄位名正規化為比對用鍵（normKey）
 * - 從 row 取出主鍵值（優先 "__PK__"，否則依 idColumn）且做有效性檢查（extractIdOrThrow）
 * 設計：
 * - final + 私有建構子：純工具類，禁止實例化/繼承。
 */
public final class RowUtils {
    private RowUtils() {
    }

    /**
     * 以大小寫不敏感方式從 map 取 key 的值並轉成字串（取不到回 null）。
     */
    public static String toStringCI(Map<String, Object> m, String key) {
        Object v = getCI(m, key);
        return v == null ? null : String.valueOf(v);
    }

    /**
     * 大小寫不敏感地從 map 取值：原樣→全大寫→全小寫→逐一 equalsIgnoreCase。
     */
    public static Object getCI(Map<String, Object> m, String key) {
        if (m.containsKey(key)) return m.get(key);
        String up = key.toUpperCase(java.util.Locale.ROOT);
        String low = key.toLowerCase(java.util.Locale.ROOT);
        if (m.containsKey(up)) return m.get(up);
        if (m.containsKey(low)) return m.get(low);
        for (String k : m.keySet()) if (k.equalsIgnoreCase(key)) return m.get(k);
        return null;
    }

    /**
     * 將欄位名轉成安全的 SQL 參數名稱：非 [A-Za-z0-9_] 皆替換為底線。
     */
    public static String paramName(String col) {
        return col.replaceAll("[^A-Za-z0-9_]", "_");
    }

    /**
     * 將欄位名正規化為比對鍵：
     * - 去除非英數底線
     * - 移除底線
     * - 轉成大寫
     * 用途：忽略命名風格差異做欄位名比對（白名單/集合比對）。
     */
    public static String normKey(String col) {
        if (col == null) return "";
        String s = col.replaceAll("[^A-Za-z0-9_]", "");
        s = s.replace("_", "");
        return s.toUpperCase(java.util.Locale.ROOT);
    }

    /**
     * 取出 row 的主鍵值（字串）：
     * - 優先取 "__PK__"（建議在 SELECT 時使用 "ID AS __PK__"）
     * - 否則依 idColumn（原樣/大寫/小寫）嘗試
     * - 轉字串並 trim，若為 null/空白/字面 "null" → 拋 IllegalStateException
     */
    public static String extractIdOrThrow(Map<String, Object> row, String idColumn, String table) {
        if (row == null || row.isEmpty()) {
            throw new IllegalStateException("無法取得有效主鍵: row 為空, table=" + table + ", idColumn=" + idColumn);
        }

        Object v = null;

        // 1) 通用別名：支援大小寫變體
        if (row.containsKey("__PK__")) v = row.get("__PK__");
        if (v == null && row.containsKey("__pk__")) v = row.get("__pk__");

        // 2) 指定的 idColumn：先原樣，再大小寫變體
        if (v == null && idColumn != null && !idColumn.isEmpty()) {
            v = row.get(idColumn);
            if (v == null) v = row.get(idColumn.toUpperCase());
            if (v == null) v = row.get(idColumn.toLowerCase());

            // 2.1) 仍找不到 → 做一次不分大小寫線性掃描（保守但穩）
            if (v == null) {
                String foundKey = null;
                for (String k : row.keySet()) {
                    if (k != null && k.equalsIgnoreCase(idColumn)) {
                        foundKey = k;
                        break;
                    }
                }
                if (foundKey != null) v = row.get(foundKey);
            }
        }

        // 3) 再次 fallback：若某些查詢只回了 __Pk__ 這種奇怪大小寫
        if (v == null) {
            String foundKey = null;
            for (String k : row.keySet()) {
                if ("__PK__".equalsIgnoreCase(k)) {
                    foundKey = k;
                    break;
                }
            }
            if (foundKey != null) v = row.get(foundKey);
        }

        // 4) 轉成字串
        String id = null;
        if (v instanceof CharSequence) {
            id = v.toString().trim();
        } else if (v instanceof Number) {
            // 注意：如果你的 ID 是字串型（可能含前導零），DB 端請用文字型別，避免走 Number 失去前導零。
            id = String.valueOf(v);
        } else if (v != null) {
            id = v.toString().trim();
        }

        // 5) 驗證
        if (id == null || id.isEmpty() || "null".equalsIgnoreCase(id)) {
            throw new IllegalStateException("無法取得有效主鍵: table=" + table
                    + ", idColumn=" + idColumn + ", rowKeys=" + row.keySet());
        }
        return id;
    }

}
