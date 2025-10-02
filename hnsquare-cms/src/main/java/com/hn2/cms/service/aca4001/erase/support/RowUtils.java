package com.hn2.cms.service.aca4001.erase.support;

import java.util.Map;

public final class RowUtils {
    private RowUtils() {
    }

    public static String toStringCI(Map<String, Object> m, String key) {
        Object v = getCI(m, key);
        return v == null ? null : String.valueOf(v);
    }

    public static Object getCI(Map<String, Object> m, String key) {
        if (m.containsKey(key)) return m.get(key);
        String up = key.toUpperCase(java.util.Locale.ROOT);
        String low = key.toLowerCase(java.util.Locale.ROOT);
        if (m.containsKey(up)) return m.get(up);
        if (m.containsKey(low)) return m.get(low);
        for (String k : m.keySet()) if (k.equalsIgnoreCase(key)) return m.get(k);
        return null;
    }

    public static String paramName(String col) {
        return col.replaceAll("[^A-Za-z0-9_]", "_");
    }

    public static String normKey(String col) {
        if (col == null) return "";
        String s = col.replaceAll("[^A-Za-z0-9_]", "");
        s = s.replace("_", "");
        return s.toUpperCase(java.util.Locale.ROOT);
    }

    public static String extractIdOrThrow(Map<String, Object> row, String idColumn, String table) {
        Object v = row.get("__PK__");
        if (v == null && idColumn != null) {
            v = row.get(idColumn);
            if (v == null) {
                v = row.get(idColumn.toUpperCase());
                if (v == null) v = row.get(idColumn.toLowerCase());
            }
        }
        String id = (v == null) ? null : v.toString().trim();
        if (id == null || id.isEmpty() || "null".equalsIgnoreCase(id)) {
            throw new IllegalStateException("無法取得有效主鍵: table=" + table
                    + ", idColumn=" + idColumn + ", rowKeys=" + row.keySet());
        }
        return id;
    }
}
