package com.hn2.cms.service.aca4001.erase.support;

import java.util.Set;

/**
 * SQL 寫回前的「欄位值正規化」工具。
 * 目的：
 * - 將從鏡像(JSON)或各種來源得到的欄位值，統一轉成 JDBC/DB 友善的型別：
 * - 日期時間欄位 → java.sql.Timestamp
 * - 整數欄位     → Integer
 * - 其餘欄位：原樣返回
 * 使用方式：
 * - 以欄位名 col 決定是否需做「日期正規化」或「整數正規化」，判斷依據為
 * dateColsNorm / intColsNorm（內容需為欄位名的正規化鍵，見 RowUtils.normKey）。
 * 容錯：
 * - 無法解析時，盡量回傳「原字串」（避免丟例外中斷整批），
 * 或回傳 null（例如空字串日期）。
 */
public final class SqlNorm {
    private SqlNorm() {
    }

    /**
     * 依欄位名與白名單集合，將值正規化為 DB 友善型別。
     * 規則：
     * - 若 col 的正規化鍵存在於 dateColsNorm：
     * 1) 已是 java.sql.Timestamp → 直接回傳
     * 2) java.util.Date → 轉 java.sql.Timestamp
     * 3) 其他型別 → 以字串解析：
     * - 優先試 OffsetDateTime / ZonedDateTime（可含時區）
     * - 否則將 ISO-like 字串做清理（去掉 'T'、小數秒、Z、UTC 偏移等），補滿到「yyyy-MM-dd HH:mm:ss」後
     * 用 Timestamp.valueOf(...) 解析
     * - 解析失敗 → 回傳原字串（保底）
     * - 若 col 的正規化鍵存在於 intColsNorm：
     * 1) Number → 轉 intValue()
     * 2) 其他 → Integer.parseInt(trim)；失敗回原字串
     * - 其他欄位 → 原樣回傳
     *
     * @param col          欄位名
     * @param val          欲正規化的值
     * @param dateColsNorm 需要做日期正規化的欄位（使用 RowUtils.normKey 後的鍵）
     * @param intColsNorm  需要做整數正規化的欄位（使用 RowUtils.normKey 後的鍵）
     * @return 正規化後的值（Timestamp / Integer / 原值/原字串）
     */
    public static Object normalizeForColumn(String col, Object val, Set<String> dateColsNorm, Set<String> intColsNorm) {
        if (val == null) return null;
        // 將欄位名正規化成比較用鍵（去掉非英數與底線、移除底線、轉大寫）
        String key = RowUtils.normKey(col);

        // -------- 日期欄位正規化 --------
        if (dateColsNorm.contains(key)) {
            // 已是 JDBC 友善型別
            if (val instanceof java.sql.Timestamp) return val;
            if (val instanceof java.util.Date) return new java.sql.Timestamp(((java.util.Date) val).getTime());

            // 其餘 → 轉字串處理
            String s = val.toString().trim();
            if (s.isEmpty()) return null; // 空字串視為 NULL

            // 1) 優先試有時區資訊的格式
            try {
                var odt = java.time.OffsetDateTime.parse(s);
                return java.sql.Timestamp.from(odt.toInstant());
            } catch (Exception ignore) {
            }

            // 2) 寬鬆處理 ISO-like：去掉 'T'、時區偏移(+08:00)/Z、小數秒等
            try {
                var zdt = java.time.ZonedDateTime.parse(s);
                return java.sql.Timestamp.from(zdt.toInstant());
            } catch (Exception ignore) {
            }
            s = s.replace('T', ' ');
            int plus = Math.max(s.indexOf('+'), s.indexOf('-'));
            if (plus > 10) s = s.substring(0, plus);
            int z = s.indexOf('Z');
            if (z > 0) s = s.substring(0, z);
            int dot = s.indexOf('.');
            if (dot > 0) s = s.substring(0, dot);
            if (s.length() == 10) s += " 00:00:00";
            String ts = s.substring(0, Math.min(19, s.length()));
            try {
                return java.sql.Timestamp.valueOf(ts);
            } catch (IllegalArgumentException ex) {
                return val.toString();
            }
        }

        // -------- 整數欄位正規化 --------
        if (intColsNorm.contains(key)) {
            if (val instanceof Number) return ((Number) val).intValue();
            try {
                return Integer.parseInt(val.toString().trim());
            } catch (Exception ex) {
                return val.toString();
            }
        }
        // 其他欄位：不變動
        return val;
    }

    /**
     * 嘗試將字串轉為 Integer；失敗回 null。
     *
     * @param s 欲解析的字串
     * @return Integer 或 null（當 s 為 null/空白/非數字）
     */
    public static Integer tryParseInt(String s) {
        try {
            return s == null ? null : Integer.valueOf(s.trim());
        } catch (Exception ignore) {
            return null;
        }
    }
}
