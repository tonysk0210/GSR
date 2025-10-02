package com.hn2.cms.service.aca4001.erase.spi;

import com.hn2.cms.service.aca4001.erase.support.RowUtils;
import com.hn2.cms.service.aca4001.erase.support.SqlNorm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class CrmRecTarget extends AbstractSql2oTarget {

    @Autowired
    public CrmRecTarget(org.sql2o.Sql2o sql2o) {
        super(sql2o);
    }

    @Override
    public String table() {
        return "CrmRec";
    }

    @Override
    public List<String> whitelistColumns() {
        return List.of("ProSource1", "ProNoticeDep",
                "CrmCrime1", "CrmCrime2", "CrmCrime3",
                "CrmTerm", "CrmChaDate", "CrmDischarge", "CrmDisDate",
                "CrmTrain", "CrmCert", "CrmMemo",
                "CrmRemission", "Crm_ReleaseDate", "Crm_Release",
                "Crm_NoJail", "Crm_Sentence", "Crm_VerdictDate",
                "CreatedByUserID", "ModifiedByUserID");
    }

    @Override
    public Set<String> dateColsNorm() {
        return Set.of("CRMCHADATE", "CRMDISDATE", "CRMRELEASEDATE", "CRMVERDICTDATE");
    }

    @Override
    public Set<String> intColsNorm() {
        return Set.of("CREATEDBYUSERID", "MODIFIEDBYUSERID", "CRMSENTENCE", "CRMTERM");
    }

    @Override
    public List<Map<String, Object>> loadRowsByIds(List<String> ids) {
        return fetchRowsByIdsChunked(ids, 1000);
    }

    @Override
    public int nullifyAndMarkErased(List<String> ids) {
        StringBuilder sb = new StringBuilder("UPDATE dbo.CrmRec SET ");
        for (int i = 0; i < whitelistColumns().size(); i++) {
            String c = whitelistColumns().get(i);
            if (i > 0) sb.append(", ");
            if (c.equalsIgnoreCase("CreatedByUserID") || c.equalsIgnoreCase("ModifiedByUserID")) {
                sb.append("[").append(c).append("] = -2");
            } else {
                sb.append("[").append(c).append("] = NULL");
            }
        }
        sb.append(", [isERASE]=1, [ModifiedOnDate]=SYSUTCDATETIME() WHERE ID IN (:ids)");
        try (var con = sql2o.open()) {
            return con.createQuery(sb.toString())
                    .addParameter("ids", ids)
                    .executeUpdate()
                    .getResult();
        }
    }

    @Override
    public int restoreFromRows(List<Map<String, Object>> rows, String operatorUserId) {
        int total = 0;
        Integer operatorUidInt = SqlNorm.tryParseInt(operatorUserId);

        for (var r : rows) {
            String id = RowUtils.toStringCI(r, "__PK__");
            if (id == null || id.isBlank()) {
                throw new IllegalStateException("CrmRecTarget.restoreFromRows: __PK__ 不可為空, rowKeys=" + r.keySet());
            }
            var cleaned = new java.util.LinkedHashMap<String, Object>();
            for (String col : whitelistColumns()) {
                if ("ModifiedByUserID".equalsIgnoreCase(col)) continue;
                Object raw = RowUtils.getCI(r, col);
                Object norm = SqlNorm.normalizeForColumn(col, raw, dateColsNorm(), intColsNorm());
                cleaned.put(col, norm);
            }
            total += updateWithDynamicSet(
                    id,
                    cleaned,
                    ", [isERASE]=0, [ModifiedOnDate]=SYSUTCDATETIME(), [ModifiedByUserID]=:uid",
                    q -> q.addParameter("uid", operatorUidInt != null ? operatorUidInt : operatorUserId)
            );
        }
        return total;
    }
}

/*@Component
@RequiredArgsConstructor
public class CrmRecTarget implements EraseTarget {
    private final org.sql2o.Sql2o sql2o;

    @Override
    public String schema() {
        return "dbo";
    }

    @Override
    public String table() {
        return "CrmRec";
    }

    // ★ 固定用 __PK__，搭配 SQL 的別名
    @Override
    public String idColumn() {
        return "__PK__";
    }

    @Override
    public List<String> whitelistColumns() {
        return List.of("ProSource1", "ProNoticeDep",
                "CrmCrime1", "CrmCrime2", "CrmCrime3",
                "CrmTerm", "CrmChaDate", "CrmDischarge", "CrmDisDate",
                "CrmTrain", "CrmCert", "CrmMemo",
                "CrmRemission", "Crm_ReleaseDate", "Crm_Release",
                "Crm_NoJail", "Crm_Sentence", "Crm_VerdictDate",
                "CreatedByUserID", "ModifiedByUserID");
    }

    @Override
    public Set<String> dateColsNorm() {
        return Set.of("CRMCHADATE", "CRMDISDATE", "CRMRELEASEDATE", "CRMVERDICTDATE");
    }

    @Override
    public Set<String> intColsNorm() {
        return Set.of("CREATEDBYUSERID", "MODIFIEDBYUSERID", "CRMSENTENCE", "CRMTERM");
    }

    @Override
    public List<Map<String, Object>> loadRowsByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) return java.util.Collections.emptyList();

        var white = whitelistColumns();
        final String selectCols = (white == null || white.isEmpty())
                ? "ID AS __PK__"
                : "ID AS __PK__," + String.join(",", white);

        String sql = "SELECT " + selectCols + " FROM dbo.CrmRec WHERE ID IN (:ids)";
        try (var con = sql2o.open()) {
            var table = con.createQuery(sql)
                    .addParameter("ids", ids)
                    .executeAndFetchTable();
            return table.rows().stream()
                    .map(org.sql2o.data.Row::asMap)
                    .collect(java.util.stream.Collectors.toList());
        }
    }

    @Override
    public int nullifyAndMarkErased(List<String> ids) {
        StringBuilder sb = new StringBuilder("UPDATE dbo.CrmRec SET ");
        for (int i = 0; i < whitelistColumns().size(); i++) {
            String c = whitelistColumns().get(i);
            if (i > 0) sb.append(", ");
            if (c.equalsIgnoreCase("CreatedByUserID") || c.equalsIgnoreCase("ModifiedByUserID")) {
                sb.append("[").append(c).append("] = -2");
            } else {
                sb.append("[").append(c).append("] = NULL");
            }
        }
        sb.append(", [isERASE]=1, [ModifiedOnDate]=SYSUTCDATETIME() WHERE ID IN (:ids)");
        try (var con = sql2o.open()) {
            return con.createQuery(sb.toString())
                    .addParameter("ids", ids)
                    .executeUpdate()
                    .getResult();
        }
    }

    @Override
    public int restoreFromRows(List<Map<String, Object>> rows, String operatorUserId) {
        int total = 0;
        Integer operatorUidInt = tryParseInt(operatorUserId); // 盡量用 int 寫回

        for (var r : rows) {
            // 1) 主鍵：大小寫不敏感
            String id = toStringCI(r, "__PK__");
            if (id == null || id.isBlank()) {
                throw new IllegalStateException("CrmRecTarget.restoreFromRows: __PK__ 不可為空, rowKeys=" + r.keySet());
            }

            // 2) 準備要寫回的欄位：大小寫不敏感取值 + 型別正規化
            var cleaned = new java.util.LinkedHashMap<String, Object>();
            for (String col : whitelistColumns()) {
                if ("ModifiedByUserID".equalsIgnoreCase(col)) continue; // 由我們統一覆寫
                Object raw = getCI(r, col);                             // map 可能全小寫
                Object norm = normalizeForColumn(col, raw, dateColsNorm(), intColsNorm());
                cleaned.put(col, norm);
            }

            // 3) 動態 SET
            StringBuilder set = new StringBuilder();
            int i = 0;
            for (var e : cleaned.entrySet()) {
                if (i++ > 0) set.append(", ");
                set.append("[").append(e.getKey()).append("] = :").append(paramName(e.getKey()));
            }
            if (set.length() > 0) set.append(", ");
            set.append("[isERASE]=0, [ModifiedOnDate]=SYSUTCDATETIME(), [ModifiedByUserID]=:uid");

            String sql = "UPDATE dbo.CrmRec SET " + set + " WHERE ID=:id AND ISNULL(isERASE,0)=1";
            try (var con = sql2o.open()) {
                var q = con.createQuery(sql)
                        .addParameter("id", id)
                        .addParameter("uid", operatorUidInt != null ? operatorUidInt : operatorUserId); // 優先用 int

                // 綁定動態參數
                for (var e : cleaned.entrySet()) {
                    q.addParameter(paramName(e.getKey()), e.getValue());
                }
                total += q.executeUpdate().getResult();
            }
        }
        return total;
    }

    @Override
    public boolean isErased(String id) {
        String sql = "SELECT CAST(ISNULL(isERASE,0) AS BIT) FROM dbo.CrmRec WHERE ID=:id";
        try (var con = sql2o.open()) {
            Boolean b = con.createQuery(sql).addParameter("id", id).executeScalar(Boolean.class);
            return Boolean.TRUE.equals(b);
        }
    }
    //===== Helper methods =====

    private static String paramName(String col) {
        return col.replaceAll("[^A-Za-z0-9_]", "_");
    }

    private static String toStringCI(Map<String, Object> m, String key) {
        Object v = getCI(m, key);
        return v == null ? null : String.valueOf(v);
    }

    private static Object getCI(Map<String, Object> m, String key) {
        if (m.containsKey(key)) return m.get(key);
        String up = key.toUpperCase(java.util.Locale.ROOT);
        String low = key.toLowerCase(java.util.Locale.ROOT);
        if (m.containsKey(up)) return m.get(up);
        if (m.containsKey(low)) return m.get(low);
        for (String k : m.keySet()) {
            if (k.equalsIgnoreCase(key)) return m.get(k);
        }
        return null;
    }


    private static Integer tryParseInt(String s) {
        try {
            return s == null ? null : Integer.valueOf(s.trim());
        } catch (Exception ignore) {
            return null;
        }
    }


    private static Object normalizeForColumn(String col, Object val, Set<String> dateColsNorm, Set<String> intColsNorm) {
        if (val == null) return null;
        String key = normKey(col);

        // 日期欄位
        if (dateColsNorm.contains(key)) {
            if (val instanceof java.sql.Timestamp) return val;
            if (val instanceof java.util.Date) return new java.sql.Timestamp(((java.util.Date) val).getTime());

            String s = val.toString().trim();
            if (s.isEmpty()) return null;

            // 1) 先試 OffsetDateTime (含時區，例如 2000-12-22T00:00:00.000+08:00)
            try {
                var odt = java.time.OffsetDateTime.parse(s);
                return java.sql.Timestamp.from(odt.toInstant());
            } catch (Exception ignore) {
            }

            // 2) 再試 ZonedDateTime
            try {
                var zdt = java.time.ZonedDateTime.parse(s);
                return java.sql.Timestamp.from(zdt.toInstant());
            } catch (Exception ignore) {
            }

            // 3) 傳統格式：yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss（去掉 T/Z/毫秒/時區）
            s = s.replace('T', ' ');
            int plus = Math.max(s.indexOf('+'), s.indexOf('-')); // 找 +08:00/-05:00（注意：字串裡可能有第一個 - 是年份，長度判斷保守）
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
                // 讓 JDBC 以原字串嘗試（不建議，但保底）
                return val.toString();
            }
        }

        // 整數欄位
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

    private static String normKey(String col) {
        if (col == null) return "";
        String s = col.replaceAll("[^A-Za-z0-9_]", "");
        s = s.replace("_", "");
        return s.toUpperCase(java.util.Locale.ROOT);
    }

}*/
