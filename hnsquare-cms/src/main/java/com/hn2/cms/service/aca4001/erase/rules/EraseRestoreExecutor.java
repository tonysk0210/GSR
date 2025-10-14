package com.hn2.cms.service.aca4001.erase.rules;

import com.hn2.cms.service.aca4001.erase.support.RowUtils;
import com.hn2.cms.service.aca4001.erase.support.SqlNorm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 規則驅動的「塗銷／還原」通用執行器。
 * - 依 EraseTableConfigPojo 的設定，讀取資料、組 UPDATE、執行。
 * - 支援：
 * 1) 以主鍵 ID 清單讀／改
 * 2) 以父鍵（可經 lookup 映射）讀／改子表
 */
@Component
@RequiredArgsConstructor
public class EraseRestoreExecutor {

    private final org.sql2o.Sql2o sql2o;

    /* ========== 讀取資料（以主鍵 ID 清單） ========== */
    public List<Map<String, Object>> loadRowsByIds(EraseTableConfigPojo r, List<String> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        // 主鍵 + 白名單欄位
        String cols = buildSelectCols(r);
        String sql = "SELECT " + cols + " FROM " + r.getSchema() + "." + r.getTable() + " WHERE " + r.getIdColumn() + " IN (:ids)";
        var out = new ArrayList<Map<String, Object>>();
        // 分批執行，避免 IN(...) 過長造成效能或語法限制
        for (int i = 0; i < ids.size(); i += 1000) {
            var sub = ids.subList(i, Math.min(i + 1000, ids.size()));
            try (var con = sql2o.open()) {
                var t = con.createQuery(sql).addParameter("ids", sub).executeAndFetchTable();
                out.addAll(t.rows().stream().map(org.sql2o.data.Row::asMap).collect(Collectors.toList()));
            }
        }
        return out;
    }

    /* ========== 讀取資料（以父鍵清單；子表用） ========== */
    public List<Map<String, Object>> loadRowsByParentIds(EraseTableConfigPojo r, List<String> parentIds) {
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

    /* ========== 父鍵映射（例如 ACACardNo -> FamCardNo） ========== */
    private List<String> resolveParentKeys(EraseTableConfigPojo r, List<String> parentIds) {
        // 若規則沒設定 lookup（table/src/dst 任一為 null），直接回傳原父鍵
        if (r.getParentIdLookupTable() == null || r.getParentIdLookupSrcColumn() == null || r.getParentIdLookupDstColumn() == null) {
            return parentIds;
        }
        if (parentIds == null || parentIds.isEmpty()) return List.of();

        // SELECT DISTINCT [dst] AS v FROM schema.lookupTable WHERE [src] IN (:pids)
        String sql = "SELECT DISTINCT [" + r.getParentIdLookupDstColumn() + "] AS v " +
                "FROM " + r.getSchema() + "." + r.getParentIdLookupTable() +
                " WHERE [" + r.getParentIdLookupSrcColumn() + "] IN (:pids)";

        var out = new ArrayList<String>();

        // 分批查出對應鍵值
        for (int i = 0; i < parentIds.size(); i += 1000) {
            var sub = parentIds.subList(i, Math.min(i + 1000, parentIds.size()));
            try (var con = sql2o.open()) {
                var t = con.createQuery(sql).addParameter("pids", sub).executeAndFetchTable();
                // 讀出別名 v 的值，去空白、過濾空字串
                for (var row : t.rows()) {
                    var v = row.getObject("v");
                    if (v != null) {
                        String s = v.toString().trim();
                        if (!s.isEmpty()) out.add(s);
                    }
                }
            }
        }
        // 去重後回傳
        return out.stream().distinct().collect(Collectors.toList());
    }

    /* ========== 組 SELECT 欄位字串（主鍵必帶為 __PK__） ========== */
    private String buildSelectCols(EraseTableConfigPojo r) {
        return (r.getWhitelist() == null || r.getWhitelist().isEmpty())
                ? r.getIdColumn() + " AS __PK__"
                : r.getIdColumn() + " AS __PK__," + String.join(",", r.getWhitelist());
    }

    /* ========== 清空（Erase）by 主鍵 ID ========== */
    public int eraseByIds(EraseTableConfigPojo r, List<String> ids) {
        if (ids == null || ids.isEmpty()) return 0;
        // 將白名單欄位清成 NULL，再覆蓋 eraseExtraSet
        String setSql = buildEraseSetSql(r);
        String sql = "UPDATE " + r.getSchema() + "." + r.getTable()
                + " SET " + setSql + " WHERE " + r.getIdColumn() + " IN (:ids)";
        try (var con = sql2o.open()) {
            return con.createQuery(sql).addParameter("ids", ids).executeUpdate().getResult();
        }
    }

    /* ========== 清空（Erase）by 父鍵（會先做映射） ========== */
    public int eraseByParent(EraseTableConfigPojo r, List<String> parentIds) {
        if (!r.isChild() || parentIds == null || parentIds.isEmpty()) return 0;

        // 先把父鍵映射成子表實際過濾鍵
        List<String> keys = resolveParentKeys(r, parentIds);
        if (keys.isEmpty()) return 0;

        String setSql = buildEraseSetSql(r);
        String sql = "UPDATE " + r.getSchema() + "." + r.getTable()
                + " SET " + setSql + " WHERE " + r.getParentFkColumn() + " IN (:pids)";
        try (var con = sql2o.open()) {
            return con.createQuery(sql).addParameter("pids", keys).executeUpdate().getResult();
        }
    }

    /* ========== 產生 Erase 用的 SET 子句 ========== */
    private String buildEraseSetSql(EraseTableConfigPojo r) {

        var parts = new ArrayList<String>();
        // 1) 白名單欄位預設 = NULL（若 eraseExtraSet 有覆蓋就不清）
        for (String c : r.getWhitelist()) {
            if (!r.getEraseExtraSet().containsKey(c)) {
                parts.add("[" + c + "]=NULL");
            }
        }
        // 2) 附加 eraseExtraSet（支援 ${NOW}、:param、字串、數值）
        for (var e : r.getEraseExtraSet().entrySet()) {
            parts.add("[" + e.getKey() + "]=" + renderSqlValue(e.getValue()));
        }
        return String.join(", ", parts);
    }

    /* ========== 將宣告的值轉成 SQL 字面值/參數 ========== */
    private String renderSqlValue(Object v) {
        if (v == null) return "NULL";
        if (v instanceof Number) return v.toString();
        String s = v.toString();
        if ("${NOW}".equals(s)) return "SYSDATETIME()";
        if (s.startsWith(":")) return s;                 // 參數名，留給外層綁（:uid）
        // 其餘當作 NVARCHAR 常值，做引號轉義避免 SQL 注入
        return "N'" + s.replace("'", "''") + "'";
    }

    /* ========== 還原（Restore） ========== */
    public int restoreRows(EraseTableConfigPojo r, List<Map<String, Object>> rows, String operatorUserId) {
        if (rows == null || rows.isEmpty()) return 0;
        int total = 0;

        // 逐列更新（通常 rows 來自鏡像／備份資料）
        for (var row : rows) {
            // 每列必須帶 __PK__（主鍵）
            String id = RowUtils.toStringCI(row, "__PK__");
            if (id == null || id.isBlank()) {
                throw new IllegalStateException("Restore: __PK__ 不可為空, table=" + r.getTable());
            }

            // 1) 依規則的 dateCols/intCols 對白名單欄位做型態正規化
            var cleaned = new LinkedHashMap<String, Object>();
            for (String c : r.getWhitelist()) {
                // ModifiedByUserID 交給 restoreExtraSet 控制，避免衝突
                if ("ModifiedByUserID".equalsIgnoreCase(c)) continue; // 讓 restoreExtraSet 控制
                Object raw = RowUtils.getCI(row, c);
                Object norm = SqlNorm.normalizeForColumn(c, raw, normSet(r.getDateCols()), normSet(r.getIntCols()));
                cleaned.put(c, norm);
            }

            // 2) 白名單欄位組成動態 SET（使用命名參數）
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

            if (set.length() == 0) continue; // 沒東西可寫回就略過

            // 安全柵欄：只還原目前 isERASE=1 的列，避免覆蓋正常資料
            String sql = "UPDATE " + r.getSchema() + "." + r.getTable()
                    + " SET " + set + " WHERE " + r.getIdColumn() + "=:id AND ISNULL(isERASE,0)=1";
            try (var con = sql2o.open()) {
                var q = con.createQuery(sql).addParameter("id", id);
                // 綁定白名單欄位的命名參數
                for (var e : cleaned.entrySet()) {
                    q.addParameter(RowUtils.paramName(e.getKey()), e.getValue());
                }
                // 若 restoreExtraSet 用到 :uid，嘗試以 int 綁定，否則以字串
                if (r.getRestoreExtraSet().values().stream().anyMatch(v -> (v != null && v.toString().startsWith(":uid")))) {
                    Integer uidInt = SqlNorm.tryParseInt(operatorUserId);
                    q.addParameter("uid", uidInt != null ? uidInt : operatorUserId);
                }
                total += q.executeUpdate().getResult();
            }
        }
        return total;
    }

    /* ========== 欄位名集合正規化（大小寫一致化） ========== */
    private Set<String> normSet(Set<String> s) {
        if (s == null) return Set.of();
        return s.stream().map(com.hn2.cms.service.aca4001.erase.support.RowUtils::normKey).collect(Collectors.toSet());
    }

    /*
    loadRowsByIds() & loadRowsByParentIds() 回傳型別：List<Map<String,Object>>
    範例（以 ProRec 為例）：

    [
  {
    "__PK__": "PR00123",
    "ProPlight": "A類",
    "HasPreviousPlight": 1,
    "PreviousPlightChangedDesc": null,
    "ProStatus": "CLOSE",
    "ProFile": null,
    "ProMemo": "備註文字",
    "ProWorkerBackup": null,
    "CreatedByUserID": 88,
    "ModifiedByUserID": 88
  },
  {
    "__PK__": "PR00124",
    "ProPlight": null,
    "HasPreviousPlight": 0,
    "PreviousPlightChangedDesc": "",
    "ProStatus": "OPEN",
    "ProFile": "xxx.pdf",
    "ProMemo": null,
    "ProWorkerBackup": null,
    "CreatedByUserID": 12,
    "ModifiedByUserID": 12
  }
    ]
    */
}
