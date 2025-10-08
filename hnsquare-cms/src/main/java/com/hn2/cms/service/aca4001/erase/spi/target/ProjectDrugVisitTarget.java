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
public class ProjectDrugVisitTarget extends AbstractSql2oTarget implements DependentEraseTarget {

    @Autowired
    public ProjectDrugVisitTarget(org.sql2o.Sql2o sql2o) {
        super(sql2o);
    }

    @Override
    public String table() { return "ProjectDrugVisit"; }

    @Override
    public String parentTableName() { return "ProRec"; } // must match EraseCommand.tableToIds key

    @Override
    public List<String> whitelistColumns() {
        return java.util.Arrays.asList(
                "SocietyMemo",
                "MarrigeStatus",
                "FaiStatus",
                "FaiSupport",
                "FaiSupportMemo",
                "SocSupport",
                "SocSupportFriend",
                "SocSupportBoss",
                "SocSupportOther",
                "Live",
                "LiveMemo",
                "LifeMemo",
                "SocStatus",
                "SocStatusMemo",
                "MonthlyPsy",
                "MonthlyPsyMemo",
                "PsyStatus2",
                "CasePlan",
                "CreatedByUserID",
                "ModifiedByUserID"
        );
    }

    @Override
    public Set<String> dateColsNorm() {
        return java.util.Collections.emptySet();
    }

    @Override
    public Set<String> intColsNorm() {
        // Add PROJECTDRUGCASEID here if it's numeric in your schema.
        return java.util.Set.of("CREATEDBYUSERID", "MODIFIEDBYUSERID");
    }

    // Not the primary path; kept for interface completeness
    @Override
    public List<Map<String, Object>> loadRowsByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) return java.util.Collections.emptyList();
        final String sql = "SELECT ID AS __PK__," + String.join(",", whitelistColumns())
                + " FROM dbo.ProjectDrugVisit WHERE ID IN (:ids)";
        try (var con = sql2o.open()) {
            var t = con.createQuery(sql).addParameter("ids", ids).executeAndFetchTable();
            return t.rows().stream().map(org.sql2o.data.Row::asMap)
                    .collect(java.util.stream.Collectors.toList());
        }
    }

    // Mirror fetch by parent ProRecID
    @Override
    public List<Map<String, Object>> loadRowsByParentIds(List<String> parentProRecIds) {
        if (parentProRecIds == null || parentProRecIds.isEmpty()) return java.util.Collections.emptyList();
        final String sql = "SELECT ID AS __PK__," + String.join(",", whitelistColumns())
                + " FROM dbo.ProjectDrugVisit WHERE ProRecID IN (:pids)";
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

    // Batch erase by parent ProRecID
    @Override
    public int nullifyAndMarkErasedByParent(List<String> parentProRecIds) {
        if (parentProRecIds == null || parentProRecIds.isEmpty()) return 0;

        final String sql =
                "UPDATE dbo.ProjectDrugVisit SET " +
                        "  [SocietyMemo]=NULL," +
                        "  [MarrigeStatus]=NULL," +
                        "  [FaiStatus]=NULL," +
                        "  [FaiSupport]=NULL," +
                        "  [FaiSupportMemo]=NULL," +
                        "  [SocSupport]=NULL," +
                        "  [SocSupportFriend]=NULL," +
                        "  [SocSupportBoss]=NULL," +
                        "  [SocSupportOther]=NULL," +
                        "  [Live]=NULL," +
                        "  [LiveMemo]=NULL," +
                        "  [LifeMemo]=NULL," +
                        "  [SocStatus]=NULL," +
                        "  [SocStatusMemo]=NULL," +
                        "  [MonthlyPsy]=NULL," +
                        "  [MonthlyPsyMemo]=NULL," +
                        "  [PsyStatus2]=NULL," +
                        "  [CasePlan]=NULL," +
                        "  [CreatedByUserID]=-2," +
                        "  [ModifiedByUserID]=-2," +
                        "  [isERASE]=1," +
                        "  [ModifiedOnDate]=SYSDATETIME() " +
                        "WHERE ProRecID IN (:pids)";
        try (var con = sql2o.open()) {
            return con.createQuery(sql)
                    .addParameter("pids", parentProRecIds)
                    .executeUpdate()
                    .getResult();
        }
    }

    // Fallback by IDs
    @Override
    public int nullifyAndMarkErased(List<String> ids) {
        if (ids == null || ids.isEmpty()) return 0;

        final String sql =
                "UPDATE dbo.ProjectDrugVisit SET " +
                        "  [SocietyMemo]=NULL,[MarrigeStatus]=NULL,[FaiStatus]=NULL,[FaiSupport]=NULL,[FaiSupportMemo]=NULL," +
                        "  [SocSupport]=NULL,[SocSupportFriend]=NULL,[SocSupportBoss]=NULL,[SocSupportOther]=NULL," +
                        "  [Live]=NULL,[LiveMemo]=NULL,[LifeMemo]=NULL," +
                        "  [SocStatus]=NULL,[SocStatusMemo]=NULL," +
                        "  [MonthlyPsy]=NULL,[MonthlyPsyMemo]=NULL,[PsyStatus2]=NULL,[CasePlan]=NULL," +
                        "  [CreatedByUserID]=-2,[ModifiedByUserID]=-2," +
                        "  [isERASE]=1,[ModifiedOnDate]=SYSDATETIME() " +
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
                throw new IllegalStateException("ProjectDrugVisit.restoreFromRows: __PK__ 不可為空, rowKeys=" + r.keySet());
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
                    ", [isERASE]=0, [ModifiedOnDate]=SYSDATETIME(), [ModifiedByUserID]=:uid",
                    q -> q.addParameter("uid", operatorUidInt != null ? operatorUidInt : operatorUserId)
            );
        }
        return total;
    }
}
