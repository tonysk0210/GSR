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
public class ProjectJailGuidanceSummaryTarget extends AbstractSql2oTarget implements DependentEraseTarget {

    @Autowired
    public ProjectJailGuidanceSummaryTarget(org.sql2o.Sql2o sql2o) {
        super(sql2o);
    }

    @Override
    public String table() {
        return "ProjectJailGuidanceSummary";
    }

    @Override
    public String parentTableName() {
        // ★ 關鍵：這名稱要與 EraseCommand.tableToIds 的 key 一致（上面我們 put("ProRec", ids)）
        return "ProRec";
    }

    @Override
    public List<String> whitelistColumns() {
        // 只鏡像與要清空/還原的欄位；若還有其它欄位也需要，請在此擴充
        return java.util.Arrays.asList(
                "CreatedByBranchID",
                "CreatedByUserID",
                "ModifiedByUserID"
        );
    }

    @Override
    public Set<String> intColsNorm() {
        return java.util.Set.of("CREATEDBYUSERID", "MODIFIEDBYUSERID");
    }

    @Override
    public List<Map<String, Object>> loadRowsByIds(List<String> ids) {
        // 依附表不以自身 ID 清單操作，一般不會用到（仍保留以符合介面）
        if (ids == null || ids.isEmpty()) return java.util.Collections.emptyList();
        final String sql = "SELECT ID AS __PK__," + String.join(",", whitelistColumns())
                + " FROM dbo.ProjectJailGuidanceSummary WHERE ID IN (:ids)";
        var all = new java.util.ArrayList<Map<String, Object>>();
        try (var con = sql2o.open()) {
            var t = con.createQuery(sql).addParameter("ids", ids).executeAndFetchTable();
            all.addAll(t.rows().stream().map(org.sql2o.data.Row::asMap)
                    .collect(java.util.stream.Collectors.toList())); // JDK11 OK
        }
        return all;
    }

    @Override
    public List<Map<String, Object>> loadRowsByParentIds(List<String> parentIds) {
        if (parentIds == null || parentIds.isEmpty()) return java.util.Collections.emptyList();
        final String sql = "SELECT ID AS __PK__," + String.join(",", whitelistColumns())
                + " FROM dbo.ProjectJailGuidanceSummary WHERE ProRecID IN (:pids)";
        var all = new java.util.ArrayList<Map<String, Object>>();
        for (int i = 0; i < parentIds.size(); i += 1000) {
            var sub = parentIds.subList(i, Math.min(i + 1000, parentIds.size()));
            try (var con = sql2o.open()) {
                var t = con.createQuery(sql).addParameter("pids", sub).executeAndFetchTable();
                all.addAll(t.rows().stream().map(org.sql2o.data.Row::asMap)
                        .collect(java.util.stream.Collectors.toList()));
            }
        }
        return all;
    }

    @Override
    public int nullifyAndMarkErasedByParent(List<String> parentIds) {
        // CreatedByBranchID=NULL，CreatedByUserID/ModifiedByUserID=-2
        final String sql =
                "UPDATE dbo.ProjectJailGuidanceSummary SET " +
                        "  [CreatedByBranchID] = :branchMask, " +
                        "  [CreatedByUserID] = -2, " +
                        "  [ModifiedByUserID] = -2, " +
                        "  [isERASE] = 1, " +
                        "  [ModifiedOnDate] = SYSDATETIME() " +
                        "WHERE ProRecID IN (:pids)";
        try (var con = sql2o.open()) {
            return con.createQuery(sql)
                    .addParameter("branchMask", "") // 若無 FK，建議用空字串；有 FK 請改為 "ZZ" 等合法值
                    .addParameter("pids", parentIds)
                    .executeUpdate()
                    .getResult();
        }
    }

    @Override
    public int nullifyAndMarkErased(List<String> ids) {
        // 不是主要用法，但為了介面一致性保留
        final String sql =
                "UPDATE dbo.ProjectJailGuidanceSummary SET " +
                        "  [CreatedByBranchID] = NULL, " +
                        "  [CreatedByUserID] = -2, " +
                        "  [ModifiedByUserID] = -2, " +
                        "  [isERASE] = 1, " +
                        "  [ModifiedOnDate] = SYSDATETIME() " +
                        "WHERE ID IN (:ids)";
        try (var con = sql2o.open()) {
            return con.createQuery(sql)
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
                throw new IllegalStateException("ProjectJailGuidanceSummary.restoreFromRows: __PK__ 不可為空, rowKeys=" + r.keySet());
            }
            var cleaned = new java.util.LinkedHashMap<String, Object>();
            for (String col : whitelistColumns()) {
                if ("ModifiedByUserID".equalsIgnoreCase(col)) continue; // 由系統填目前操作者
                Object raw = RowUtils.getCI(r, col);
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
