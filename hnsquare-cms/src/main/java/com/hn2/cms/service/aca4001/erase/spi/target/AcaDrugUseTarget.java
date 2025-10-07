package com.hn2.cms.service.aca4001.erase.spi.target;

import com.hn2.cms.service.aca4001.erase.spi.AbstractSql2oTarget;
import com.hn2.cms.service.aca4001.erase.spi.DependentEraseTarget;
import com.hn2.cms.service.aca4001.erase.support.RowUtils;
import com.hn2.cms.service.aca4001.erase.support.SqlNorm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class AcaDrugUseTarget extends AbstractSql2oTarget implements DependentEraseTarget {

    @Autowired
    public AcaDrugUseTarget(org.sql2o.Sql2o sql2o) {
        super(sql2o);
    }

    @Override
    public String table() { return "AcaDrugUse"; }

    @Override
    public String parentTableName() { return "ProRec"; } // ★ 與 EraseCommand.tableToIds 的 key 對齊

    @Override
    public List<String> whitelistColumns() {
        return java.util.Arrays.asList(
                "Addr",
                "OprAddr",
                "DrgUserText",
                "OprFamilyText",
                "OprFamilyCareText",
                "CreatedByBranchID",
                "CreatedByUserID",
                "ModifiedByUserID"
        );
    }

    @Override
    public Set<String> intColsNorm() {
        return java.util.Set.of("CREATEDBYUSERID", "MODIFIEDBYUSERID");
    }

    // 這張表主要靠「父鍵 ProRecID」操作；保留 byIds 以符合介面
    @Override
    public List<Map<String, Object>> loadRowsByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) return java.util.Collections.emptyList();
        final String sql = "SELECT ID AS __PK__," + String.join(",", whitelistColumns())
                + " FROM dbo.AcaDrugUse WHERE ID IN (:ids)";
        try (var con = sql2o.open()) {
            var t = con.createQuery(sql).addParameter("ids", ids).executeAndFetchTable();
            return t.rows().stream().map(org.sql2o.data.Row::asMap)
                    .collect(java.util.stream.Collectors.toList());
        }
    }

    @Override
    public List<Map<String, Object>> loadRowsByParentIds(List<String> parentProRecIds) {
        if (parentProRecIds == null || parentProRecIds.isEmpty()) return java.util.Collections.emptyList();
        final String sql = "SELECT ID AS __PK__," + String.join(",", whitelistColumns())
                + " FROM dbo.AcaDrugUse WHERE ProRecID IN (:pids)";
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

    @Override
    public int nullifyAndMarkErasedByParent(List<String> parentProRecIds) {
        // ★ 若 CreatedByBranchID 有 FK，請把 '' 改成合法掩碼（如 'ZZ'）
        final String sql =
                "UPDATE dbo.AcaDrugUse SET " +
                        "  [Addr] = NULL, " +
                        "  [OprAddr] = NULL, " +
                        "  [DrgUserText] = NULL, " +
                        "  [OprFamilyText] = NULL, " +
                        "  [OprFamilyCareText] = NULL, " +
                        "  [CreatedByBranchID] = '', " +
                        "  [CreatedByUserID] = -2, " +
                        "  [ModifiedByUserID] = -2, " +
                        "  [isERASE] = 1, " +
                        "  [ModifiedOnDate] = SYSDATETIME() " + // 若你決定改存本地，換成 SYSDATETIME()
                        "WHERE ProRecID IN (:pids)";
        try (var con = sql2o.open()) {
            return con.createQuery(sql)
                    .addParameter("pids", parentProRecIds)
                    .executeUpdate()
                    .getResult();
        }
    }

    @Override
    public int nullifyAndMarkErased(List<String> ids) {
        // 非主要用法（保留）
        final String sql =
                "UPDATE dbo.AcaDrugUse SET " +
                        "  [Addr] = NULL, [OprAddr] = NULL, [DrgUserText] = NULL, " +
                        "  [OprFamilyText] = NULL, [OprFamilyCareText] = NULL, " +
                        "  [CreatedByBranchID] = '', [CreatedByUserID] = -2, [ModifiedByUserID] = -2, " +
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
                throw new IllegalStateException("AcaDrugUse.restoreFromRows: __PK__ 不可為空, rowKeys=" + r.keySet());
            }
            var cleaned = new java.util.LinkedHashMap<String, Object>();
            for (String col : whitelistColumns()) {
                if ("ModifiedByUserID".equalsIgnoreCase(col)) continue; // 由系統寫操作者
                Object raw = com.hn2.cms.service.aca4001.erase.support.RowUtils.getCI(r, col);
                Object norm = SqlNorm.normalizeForColumn(col, raw, java.util.Collections.emptySet(), intColsNorm());
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
