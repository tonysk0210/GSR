package com.hn2.cms.service.aca4001.erase.rules;

import com.hn2.cms.service.aca4001.erase.support.RowUtils;
import com.hn2.cms.service.aca4001.erase.support.SqlNorm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Consumer;
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
    private static final int DEFAULT_CHUNK_SIZE = 1000;

    /* ========== 讀取資料（以主鍵 ID 清單） ========== */
    /** 依主鍵批次撈取原始資料，避免 IN 清單過長導致查詢失敗。 */
    public List<Map<String, Object>> loadRowsByIds(EraseTableConfigPojo r, List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        String cols = buildSelectCols(r);
        String sql = "SELECT " + cols + " FROM " + r.getSchema() + "." + r.getTable()
                + " WHERE " + r.getIdColumn() + " IN (:ids)";
        var rows = new ArrayList<Map<String, Object>>();
        chunked(ids, chunk -> rows.addAll(executeSelect(sql, "ids", chunk)));
        return rows;
    }

    /* ========== 讀取資料（依 parentId 映射） ========== */
    /** 依 parent 主鍵查詢子表資料，必要時會先透過 lookup 轉換 key。 */
    public List<Map<String, Object>> loadRowsByParentIds(EraseTableConfigPojo r, List<String> parentIds) {
        if (!r.isChild() || parentIds == null || parentIds.isEmpty()) {
            return List.of();
        }

        List<String> keys = resolveParentKeys(r, parentIds);
        if (keys.isEmpty()) {
            return List.of();
        }

        String cols = buildSelectCols(r);
        String sql = "SELECT " + cols + " FROM " + r.getSchema() + "." + r.getTable()
                + " WHERE " + r.getParentFkColumn() + " IN (:pids)";

        var rows = new ArrayList<Map<String, Object>>();
        chunked(keys, chunk -> rows.addAll(executeSelect(sql, "pids", chunk)));
        return rows;
    }

    /* ========== 解析 parentId 對應關係（例如 ACACardNo -> FamCardNo） ========== */
    /** 將 parentId 透過設定檔指定的 lookup 表轉換為實際 FK，並維持順序。 */
    private List<String> resolveParentKeys(EraseTableConfigPojo r, List<String> parentIds) {
        if (r.getParentIdLookupTable() == null
                || r.getParentIdLookupSrcColumn() == null
                || r.getParentIdLookupDstColumn() == null) {
            return parentIds;
        }
        if (parentIds == null || parentIds.isEmpty()) {
            return List.of();
        }

        String sql = "SELECT DISTINCT [" + r.getParentIdLookupDstColumn() + "] AS v "
                + "FROM " + r.getSchema() + "." + r.getParentIdLookupTable()
                + " WHERE [" + r.getParentIdLookupSrcColumn() + "] IN (:pids)";

        var collected = new LinkedHashSet<String>();
        chunked(parentIds, chunk -> collected.addAll(fetchLookupValues(sql, "pids", chunk)));
        return collected.isEmpty() ? List.of() : new ArrayList<>(collected);
    }

    /** 將清單切割成固定批次，避免單次 SQL 帶入過多參數。 */
    private void chunked(List<String> source, Consumer<List<String>> action) {
        if (source == null || source.isEmpty()) {
            return;
        }
        for (int i = 0; i < source.size(); i += DEFAULT_CHUNK_SIZE) {
            action.accept(source.subList(i, Math.min(i + DEFAULT_CHUNK_SIZE, source.size())));
        }
    }

    /** 使用 Sql2o 查詢並以 Map 形式回傳結果。 */
    private List<Map<String, Object>> executeSelect(String sql, String paramName, List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        try (var con = sql2o.open()) {
            var table = con.createQuery(sql)
                    .addParameter(paramName, values)
                    .executeAndFetchTable();
            return table.rows().stream()
                    .map(org.sql2o.data.Row::asMap)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to execute query for " + paramName + " in SQL: " + sql, e);
        }
    }

    /** 讀取 lookup 結果轉為字串清單，忽略空白值。 */
    private List<String> fetchLookupValues(String sql, String paramName, List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        try (var con = sql2o.open()) {
            var table = con.createQuery(sql)
                    .addParameter(paramName, values)
                    .executeAndFetchTable();
            List<String> out = new ArrayList<>();
            for (var row : table.rows()) {
                var value = row.getObject("v");
                if (value != null) {
                    String text = value.toString().trim();
                    if (!text.isEmpty()) {
                        out.add(text);
                    }
                }
            }
            return out;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to execute lookup for " + paramName + " in SQL: " + sql, e);
        }
    }

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

    /** 擷取白名單欄位並進行型別正規化，避免寫回時出現格式錯誤。 */
    private Map<String, Object> extractWhitelistValues(EraseTableConfigPojo r, Map<String, Object> row,
                                                       Set<String> normalizedDateCols, Set<String> normalizedIntCols) {
        var cleaned = new LinkedHashMap<String, Object>();
        for (String column : r.getWhitelist()) {
            if ("ModifiedByUserID".equalsIgnoreCase(column)) {
                continue;
            }
            Object raw = RowUtils.getCI(row, column);
            Object normalized = SqlNorm.normalizeForColumn(column, raw, normalizedDateCols, normalizedIntCols);
            cleaned.put(column, normalized);
        }
        return cleaned;
    }

    /** 組裝 UPDATE SET 子句，優先使用鏡像資料，再補上 restoreExtraSet 的固定值。 */
    private String buildRestoreSetClause(EraseTableConfigPojo r, Map<String, Object> cleaned) {
        var assignments = new ArrayList<String>();
        cleaned.forEach((column, value) -> assignments.add("[" + column + "] = :" + RowUtils.paramName(column)));
        r.getRestoreExtraSet().forEach((column, value) -> assignments.add("[" + column + "] = " + renderSqlValue(value)));
        return String.join(", ", assignments);
    }

    /** 將鏡像欄位值綁定回 Sql2o 查詢參數。 */
    private void bindWhitelistParameters(org.sql2o.Query query, Map<String, Object> cleaned) {
        cleaned.forEach((column, value) -> query.addParameter(RowUtils.paramName(column), value));
    }

    /** 若設定要求寫入 :uid，會在此決定使用 int 或字串綁定。 */
    private void bindRestoreExtras(org.sql2o.Query query, EraseTableConfigPojo r, String operatorUserId) {
        if (!requiresUidParameter(r)) {
            return;
        }
        Integer uidInt = SqlNorm.tryParseInt(operatorUserId);
        query.addParameter("uid", uidInt != null ? uidInt : operatorUserId);
    }

    /** 判斷 restoreExtraSet 是否使用 :uid 參數。 */
    private boolean requiresUidParameter(EraseTableConfigPojo r) {
        return r.getRestoreExtraSet().values().stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .anyMatch(val -> val.startsWith(":uid"));
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
    /** 根據鏡像快照回寫來源表，僅還原已標記塗銷的資料列。 */
    public int restoreRows(EraseTableConfigPojo r, List<Map<String, Object>> rows, String operatorUserId) {
        if (rows == null || rows.isEmpty()) {
            return 0;
        }
        int total = 0;
        var dateCols = normSet(r.getDateCols());
        var intCols = normSet(r.getIntCols());

        for (var row : rows) {
            String id = RowUtils.toStringCI(row, "__PK__");
            if (id == null || id.isBlank()) {
                throw new IllegalStateException("Restore: __PK__ 不能為空, table=" + r.getTable());
            }

            var cleaned = extractWhitelistValues(r, row, dateCols, intCols);
            String setClause = buildRestoreSetClause(r, cleaned);
            if (setClause.isEmpty()) {
                continue;
            }

            String sql = "UPDATE " + r.getSchema() + "." + r.getTable()
                    + " SET " + setClause + " WHERE " + r.getIdColumn() + "=:id AND ISNULL(isERASE,0)=1";
            try (var con = sql2o.open()) {
                var query = con.createQuery(sql).addParameter("id", id);
                bindWhitelistParameters(query, cleaned);
                bindRestoreExtras(query, r, operatorUserId);
                total += query.executeUpdate().getResult();
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
