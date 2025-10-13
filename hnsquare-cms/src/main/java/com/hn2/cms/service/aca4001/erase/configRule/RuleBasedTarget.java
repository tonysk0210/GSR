package com.hn2.cms.service.aca4001.erase.configRule;

import com.hn2.cms.service.aca4001.erase.support.RowUtils;
import com.hn2.cms.service.aca4001.erase.support.SqlNorm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RuleBasedTarget {

    private final org.sql2o.Sql2o sql2o;

    /* ========== 讀取資料 ========== */
    public List<Map<String, Object>> loadRowsByIds(EraseRule r, List<String> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        String cols = buildSelectCols(r);
        String sql = "SELECT " + cols + " FROM " + r.getSchema() + "." + r.getTable() + " WHERE " + r.getIdColumn() + " IN (:ids)";
        var out = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < ids.size(); i += 1000) {
            var sub = ids.subList(i, Math.min(i + 1000, ids.size()));
            try (var con = sql2o.open()) {
                var t = con.createQuery(sql).addParameter("ids", sub).executeAndFetchTable();
                out.addAll(t.rows().stream().map(org.sql2o.data.Row::asMap).collect(Collectors.toList()));
            }
        }
        return out;
    }

    public List<Map<String, Object>> loadRowsByParentIds(EraseRule r, List<String> parentIds) {
        if (!r.isChild() || parentIds == null || parentIds.isEmpty()) return List.of();

        // 先把 ACACardNo 轉成 FamCardNo（或其他對應）
        List<String> keys = resolveParentKeys(r, parentIds);
        if (keys.isEmpty()) return List.of();

        String cols = buildSelectCols(r);
        String sql = "SELECT " + cols + " FROM " + r.getSchema() + "." + r.getTable()
                + " WHERE " + r.getParentFkColumn() + " IN (:pids)";

        var out = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < keys.size(); i += 1000) {                      // ★ 用 keys 計數
            var sub = keys.subList(i, Math.min(i + 1000, keys.size()));     // ★ 切 keys
            try (var con = sql2o.open()) {
                var t = con.createQuery(sql)
                        .addParameter("pids", sub)                        // ★ 綁 keys 的 sub
                        .executeAndFetchTable();
                out.addAll(t.rows().stream().map(org.sql2o.data.Row::asMap).collect(Collectors.toList()));
            }
        }
        return out;
    }

    //---
    private List<String> resolveParentKeys(EraseRule r, List<String> parentIds) {
        if (r.getParentIdLookupTable() == null ||
                r.getParentIdLookupSrcColumn() == null ||
                r.getParentIdLookupDstColumn() == null) {
            return parentIds;
        }
        if (parentIds == null || parentIds.isEmpty()) return List.of();

        String sql = "SELECT DISTINCT [" + r.getParentIdLookupDstColumn() + "] AS v " +
                "FROM " + r.getSchema() + "." + r.getParentIdLookupTable() +
                " WHERE [" + r.getParentIdLookupSrcColumn() + "] IN (:pids)";

        var out = new ArrayList<String>();
        for (int i = 0; i < parentIds.size(); i += 1000) {
            var sub = parentIds.subList(i, Math.min(i + 1000, parentIds.size()));
            try (var con = sql2o.open()) {
                var t = con.createQuery(sql).addParameter("pids", sub).executeAndFetchTable();
                for (var row : t.rows()) {
                    var v = row.getObject("v");
                    if (v != null) {
                        String s = v.toString().trim();
                        if (!s.isEmpty()) out.add(s);
                    }
                }
            }
        }
        // 去重
        return out.stream().distinct().collect(Collectors.toList());
    }

    private String buildSelectCols(EraseRule r) {
        return (r.getWhitelist() == null || r.getWhitelist().isEmpty())
                ? r.getIdColumn() + " AS __PK__"
                : r.getIdColumn() + " AS __PK__," + String.join(",", r.getWhitelist());
    }

    /* ========== 清空（Erase） ========== */
    public int eraseByIds(EraseRule r, List<String> ids) {
        if (ids == null || ids.isEmpty()) return 0;
        String setSql = buildEraseSetSql(r);
        String sql = "UPDATE " + r.getSchema() + "." + r.getTable()
                + " SET " + setSql + " WHERE " + r.getIdColumn() + " IN (:ids)";
        try (var con = sql2o.open()) {
            return con.createQuery(sql).addParameter("ids", ids).executeUpdate().getResult();
        }
    }

    public int eraseByParent(EraseRule r, List<String> parentIds) {
        if (!r.isChild() || parentIds == null || parentIds.isEmpty()) return 0;

        List<String> keys = resolveParentKeys(r, parentIds);   // ★ 新增
        if (keys.isEmpty()) return 0;

        String setSql = buildEraseSetSql(r);
        String sql = "UPDATE " + r.getSchema() + "." + r.getTable()
                + " SET " + setSql + " WHERE " + r.getParentFkColumn() + " IN (:pids)";
        try (var con = sql2o.open()) {
            return con.createQuery(sql).addParameter("pids", keys).executeUpdate().getResult();
        }
    }

    private String buildEraseSetSql(EraseRule r) {
        // 1) 先把白名單欄位預設為 NULL
        var parts = new ArrayList<String>();
        for (String c : r.getWhitelist()) {
            // 若 eraseSet 另有指定該欄位值，則以 eraseSet 為準，不重覆加
            if (!r.getEraseSet().containsKey(c)) {
                parts.add("[" + c + "]=NULL");
            }
        }
        // 2) 再把 eraseSet 指定的欄位值塞進來（支援 ${NOW} 與字串/數值）
        for (var e : r.getEraseSet().entrySet()) {
            parts.add("[" + e.getKey() + "]=" + renderSqlValue(e.getValue()));
        }
        return String.join(", ", parts);
    }

    private String renderSqlValue(Object v) {
        if (v == null) return "NULL";
        if (v instanceof Number) return v.toString();
        String s = v.toString();
        if ("${NOW}".equals(s)) return "SYSDATETIME()";   // 可改 SYSUTCDATETIME()
        if (s.startsWith(":")) return s;                 // 參數名，留給外層綁（:uid）
        // 字串常值：以 N'...' 寫入（避免 NCHAR/NVARCHAR 問題）
        return "N'" + s.replace("'", "''") + "'";
    }

    /* ========== 還原（Restore） ========== */
    public int restoreRows(EraseRule r, List<Map<String, Object>> rows, String operatorUserId) {
        if (rows == null || rows.isEmpty()) return 0;
        int total = 0;

        // 還原時：只寫回白名單欄位；另外把 restoreExtraSet 也併進 SET
        for (var row : rows) {
            String id = RowUtils.toStringCI(row, "__PK__");
            if (id == null || id.isBlank()) {
                throw new IllegalStateException("Restore: __PK__ 不可為空, table=" + r.getTable());
            }

            // 1) 正規化欄位值（依 dateCols/intCols）
            var cleaned = new LinkedHashMap<String, Object>();
            for (String c : r.getWhitelist()) {
                if ("ModifiedByUserID".equalsIgnoreCase(c)) continue; // 讓 restoreExtraSet 控制
                Object raw = RowUtils.getCI(row, c);
                Object norm = SqlNorm.normalizeForColumn(c, raw, normSet(r.getDateCols()), normSet(r.getIntCols()));
                cleaned.put(c, norm);
            }

            // 2) 動態組 UPDATE SET
            StringBuilder set = new StringBuilder();
            int i = 0;
            for (var e : cleaned.entrySet()) {
                if (i++ > 0) set.append(", ");
                set.append("[").append(e.getKey()).append("] = :").append(RowUtils.paramName(e.getKey()));
            }
            // 3) 追加 restoreExtraSet（支援 :uid / ${NOW}）
            for (var e : r.getRestoreExtraSet().entrySet()) {
                if (set.length() > 0) set.append(", ");
                String val = renderSqlValue(e.getValue());
                set.append("[").append(e.getKey()).append("] = ").append(val);
            }

            if (set.length() == 0) continue; // 沒東西可還原

            String sql = "UPDATE " + r.getSchema() + "." + r.getTable()
                    + " SET " + set + " WHERE " + r.getIdColumn() + "=:id AND ISNULL(isERASE,0)=1";
            try (var con = sql2o.open()) {
                var q = con.createQuery(sql).addParameter("id", id);
                for (var e : cleaned.entrySet()) {
                    q.addParameter(RowUtils.paramName(e.getKey()), e.getValue());
                }
                // 綁定 :uid（若有在 restoreExtraSet 用到）
                if (r.getRestoreExtraSet().values().stream().anyMatch(v -> (v != null && v.toString().startsWith(":uid")))) {
                    Integer uidInt = SqlNorm.tryParseInt(operatorUserId);
                    q.addParameter("uid", uidInt != null ? uidInt : operatorUserId);
                }
                total += q.executeUpdate().getResult();
            }
        }
        return total;
    }

    private Set<String> normSet(Set<String> s) {
        if (s == null) return Set.of();
        return s.stream().map(com.hn2.cms.service.aca4001.erase.support.RowUtils::normKey).collect(Collectors.toSet());
    }
}
