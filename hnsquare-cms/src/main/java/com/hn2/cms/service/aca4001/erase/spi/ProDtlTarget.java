package com.hn2.cms.service.aca4001.erase.spi;

import com.hn2.cms.service.aca4001.erase.support.RowUtils;
import com.hn2.cms.service.aca4001.erase.support.SqlNorm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class ProDtlTarget extends AbstractSql2oTarget implements DependentEraseTarget {

    @Autowired
    public ProDtlTarget(org.sql2o.Sql2o sql2o) {
        super(sql2o);
    }

    @Override
    public String table() {
        return "ProDtl";
    }

    @Override
    public String parentTableName() {
        return "ProRec";
    } // 與 EraseCommand.tableToIds 的 key 對齊

    @Override
    public List<String> whitelistColumns() {
        // 僅鏡像/還原這些欄位（你列出的欄位）
        return java.util.Arrays.asList(
                "ProCost",
                "ProDtlDate",
                "ProDtlEndMemo",
                "ProDtlTrackOther",
                "ProPlace",
                "ProMaterial_G",
                "IsTrainingCompleted",
                "HasLicenses",
                "ProContent",
                "InterviewPlace",
                "CreatedByUserID",
                "ModifiedByUserID"
        );
    }

    @Override
    public Set<String> dateColsNorm() {
        // 有日期欄位
        return java.util.Set.of("PRODTLDATE");
    }

    @Override
    public Set<String> intColsNorm() {
        // 僅對整數欄位做數值正規化；BIT/DECIMAL 不放這裡
        return java.util.Set.of("CREATEDBYUSERID", "MODIFIEDBYUSERID");
    }

    // by-ids 非主要用法，保留以符合介面
    @Override
    public List<Map<String, Object>> loadRowsByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) return java.util.Collections.emptyList();
        final String sql = "SELECT ID AS __PK__," + String.join(",", whitelistColumns())
                + " FROM dbo.ProDtl WHERE ID IN (:ids)";
        try (var con = sql2o.open()) {
            var t = con.createQuery(sql).addParameter("ids", ids).executeAndFetchTable();
            return t.rows().stream().map(org.sql2o.data.Row::asMap)
                    .collect(java.util.stream.Collectors.toList());
        }
    }

    // 依父鍵 ProRecID 撈 mirror 用資料
    @Override
    public List<Map<String, Object>> loadRowsByParentIds(List<String> parentProRecIds) {
        if (parentProRecIds == null || parentProRecIds.isEmpty()) return java.util.Collections.emptyList();
        final String sql = "SELECT ID AS __PK__," + String.join(",", whitelistColumns())
                + " FROM dbo.ProDtl WHERE ProRecID IN (:pids)";
        var all = new java.util.ArrayList<Map<String, Object>>();
        for (int i = 0; i < parentProRecIds.size(); i += 1000) {
            var sub = parentProRecIds.subList(i, Math.min(i + 1000, parentProRecIds.size()));
            try (var con = sql2o.open()) {
                var t = con.createQuery(sql).addParameter("pids", sub).executeAndFetchTable();
                all.addAll(t.rows().stream().map(org.sql2o.data.Row::asMap)
                        .collect(java.util.stream.Collectors.toList()));
            }
        }
        return all;
    }

    // 依父鍵清空欄位並標記 isERASE=1
    @Override
    public int nullifyAndMarkErasedByParent(List<String> parentProRecIds) {
        final String sql =
                "UPDATE dbo.ProDtl SET " +
                        "  [ProCost] = NULL, " +
                        "  [ProDtlDate] = NULL, " +
                        "  [ProDtlEndMemo] = NULL, " +
                        "  [ProDtlTrackOther] = NULL, " +
                        "  [ProPlace] = NULL, " +
                        "  [ProMaterial_G] = NULL, " +
                        "  [IsTrainingCompleted] = NULL, " +   // ※ 若 NOT NULL，請改為 0
                        "  [HasLicenses] = NULL, " +           // ※ 若 NOT NULL，請改為 0
                        "  [ProContent] = NULL, " +
                        "  [InterviewPlace] = NULL, " +
                        "  [CreatedByUserID] = -2, " +
                        "  [ModifiedByUserID] = -2, " +
                        "  [isERASE] = 1, " +
                        "  [ModifiedOnDate] = SYSDATETIME() " +
                        "WHERE ProRecID IN (:pids)";
        try (var con = sql2o.open()) {
            return con.createQuery(sql)
                    .addParameter("pids", parentProRecIds)
                    .executeUpdate()
                    .getResult();
        }
    }

    // 非主要用法（保留）
    @Override
    public int nullifyAndMarkErased(List<String> ids) {
        final String sql =
                "UPDATE dbo.ProDtl SET " +
                        "  [ProCost] = NULL, [ProDtlDate] = NULL, [ProDtlEndMemo] = NULL, " +
                        "  [ProDtlTrackOther] = NULL, [ProPlace] = NULL, [ProMaterial_G] = NULL, " +
                        "  [IsTrainingCompleted] = NULL, [HasLicenses] = NULL, " +
                        "  [ProContent] = NULL, [InterviewPlace] = NULL, " +
                        "  [CreatedByUserID] = -2, [ModifiedByUserID] = -2, " +
                        "  [isERASE] = 1, [ModifiedOnDate] = SYSDATETIME() " +
                        "WHERE ID IN (:ids)";
        try (var con = sql2o.open()) {
            return con.createQuery(sql).addParameter("ids", ids).executeUpdate().getResult();
        }
    }

    @Override
    public int restoreFromRows(List<Map<String, Object>> rows, String operatorUserId) {
        int total = 0;
        Integer operatorUidInt = SqlNorm.tryParseInt(operatorUserId);

        for (var r : rows) {
            String id = RowUtils.toStringCI(r, "__PK__");
            if (id == null || id.isBlank()) {
                throw new IllegalStateException("ProDtl.restoreFromRows: __PK__ 不可為空, rowKeys=" + r.keySet());
            }
            var cleaned = new java.util.LinkedHashMap<String, Object>();
            for (String col : whitelistColumns()) {
                if ("ModifiedByUserID".equalsIgnoreCase(col)) continue; // 由系統填寫目前操作者
                Object raw = RowUtils.getCI(r, col);
                Object norm = SqlNorm.normalizeForColumn(col, raw, dateColsNorm(), intColsNorm());
                cleaned.put(col, norm);
            }
            total += updateWithDynamicSet(
                    id,
                    cleaned,
                    ", [isERASE]=0, [ModifiedOnDate]=SYSDATETIME(), [ModifiedByUserID]=:uid",
                    q -> q.addParameter("uid", operatorUidInt != null ? operatorUidInt : operatorUserId)
            );
        }
        return total;
    }
}
