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
public class ProjectJailGuidanceTarget extends AbstractSql2oTarget implements DependentEraseTarget {

    @Autowired
    public ProjectJailGuidanceTarget(org.sql2o.Sql2o sql2o) {
        super(sql2o);
    }

    @Override
    public String table() { return "ProjectJailGuidance"; }

    @Override
    public String parentTableName() { return "ProRec"; } // 與 EraseCommand.tableToIds 的 key 對齊

    @Override
    public List<String> whitelistColumns() {
        return java.util.Arrays.asList(
                "FamilyStatus",
                "IsFamilyStatusMemo",
                "NotFamilyStatusMemo",
                "SentenceVisitStatus",
                "SentenceVisitStatusMemo",
                "PreJob",
                "PreJobMemo",
                "Temperament",
                "TemperamentMemo",
                "GuidanceIntention",
                "Job",
                "JobMemo",
                "TrainingMemo",
                "LicenseMemo",
                "PsyCounseling",
                "AnotherCase",
                "AnotherCaseMemo",
                "Other",
                "Assist",
                "IsWilling",
                "CreatedByUserID",
                "ModifiedByUserID"
        );
    }

    @Override
    public Set<String> intColsNorm() {
        // 帳號型欄位可能為數字
        return java.util.Set.of("CREATEDBYUSERID", "MODIFIEDBYUSERID");
    }

    // by-ids（通常不會用到，為符合介面保留）
    @Override
    public List<Map<String, Object>> loadRowsByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) return java.util.Collections.emptyList();
        final String sql = "SELECT ID AS __PK__," + String.join(",", whitelistColumns())
                + " FROM dbo.ProjectJailGuidance WHERE ID IN (:ids)";
        try (var con = sql2o.open()) {
            var t = con.createQuery(sql).addParameter("ids", ids).executeAndFetchTable();
            return t.rows().stream().map(org.sql2o.data.Row::asMap)
                    .collect(java.util.stream.Collectors.toList());
        }
    }

    // 依父鍵 ProRecID 撈 rows（鏡像用）
    @Override
    public List<Map<String, Object>> loadRowsByParentIds(List<String> parentProRecIds) {
        if (parentProRecIds == null || parentProRecIds.isEmpty()) return java.util.Collections.emptyList();
        final String sql = "SELECT ID AS __PK__," + String.join(",", whitelistColumns())
                + " FROM dbo.ProjectJailGuidance WHERE ProRecID IN (:pids)";
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

    // 依父鍵批次清空欄位並標記 isERASE=1
    @Override
    public int nullifyAndMarkErasedByParent(List<String> parentProRecIds) {
        final String sql =
                "UPDATE dbo.ProjectJailGuidance SET " +
                        "  [FamilyStatus] = NULL, " +
                        "  [IsFamilyStatusMemo] = NULL, " +
                        "  [NotFamilyStatusMemo] = NULL, " +
                        "  [SentenceVisitStatus] = NULL, " +
                        "  [SentenceVisitStatusMemo] = NULL, " +
                        "  [PreJob] = NULL, " +
                        "  [PreJobMemo] = NULL, " +
                        "  [Temperament] = NULL, " +
                        "  [TemperamentMemo] = NULL, " +
                        "  [GuidanceIntention] = NULL, " +
                        "  [Job] = NULL, " +
                        "  [JobMemo] = NULL, " +
                        "  [TrainingMemo] = NULL, " +
                        "  [LicenseMemo] = NULL, " +
                        "  [PsyCounseling] = NULL, " +
                        "  [AnotherCase] = NULL, " +
                        "  [AnotherCaseMemo] = NULL, " +
                        "  [Other] = NULL, " +
                        "  [Assist] = NULL, " +
                        "  [IsWilling] = NULL, " +
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
                "UPDATE dbo.ProjectJailGuidance SET " +
                        "  [FamilyStatus] = NULL, [IsFamilyStatusMemo] = NULL, [NotFamilyStatusMemo] = NULL, " +
                        "  [SentenceVisitStatus] = NULL, [SentenceVisitStatusMemo] = NULL, " +
                        "  [PreJob] = NULL, [PreJobMemo] = NULL, [Temperament] = NULL, [TemperamentMemo] = NULL, " +
                        "  [GuidanceIntention] = NULL, [Job] = NULL, [JobMemo] = NULL, " +
                        "  [TrainingMemo] = NULL, [LicenseMemo] = NULL, [PsyCounseling] = NULL, " +
                        "  [AnotherCase] = NULL, [AnotherCaseMemo] = NULL, [Other] = NULL, [Assist] = NULL, [IsWilling] = NULL, " +
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
                throw new IllegalStateException("ProjectJailGuidance.restoreFromRows: __PK__ 不可為空, rowKeys=" + r.keySet());
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
                    ", [isERASE]=0, [ModifiedOnDate]=SYSDATETIME(), [ModifiedByUserID]=:uid",
                    q -> q.addParameter("uid", operatorUidInt != null ? operatorUidInt : operatorUserId)
            );
        }
        return total;
    }
}
