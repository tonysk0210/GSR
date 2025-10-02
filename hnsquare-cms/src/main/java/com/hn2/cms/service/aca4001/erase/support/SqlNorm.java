package com.hn2.cms.service.aca4001.erase.support;

import java.util.Set;

public final class SqlNorm {
    private SqlNorm() {
    }

    public static Object normalizeForColumn(
            String col, Object val,
            Set<String> dateColsNorm, Set<String> intColsNorm) {
        if (val == null) return null;
        String key = RowUtils.normKey(col);

        if (dateColsNorm.contains(key)) {
            if (val instanceof java.sql.Timestamp) return val;
            if (val instanceof java.util.Date) return new java.sql.Timestamp(((java.util.Date) val).getTime());
            String s = val.toString().trim();
            if (s.isEmpty()) return null;
            try { // OffsetDateTime
                var odt = java.time.OffsetDateTime.parse(s);
                return java.sql.Timestamp.from(odt.toInstant());
            } catch (Exception ignore) {
            }
            try { // ZonedDateTime
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

        if (intColsNorm.contains(key)) {
            if (val instanceof Number) return ((Number) val).intValue();
            try {
                return Integer.parseInt(val.toString().trim());
            } catch (Exception ex) {
                return val.toString();
            }
        }
        return val;
    }

    public static Integer tryParseInt(String s) {
        try {
            return s == null ? null : Integer.valueOf(s.trim());
        } catch (Exception ignore) {
            return null;
        }
    }
}
