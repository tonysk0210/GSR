package com.hn2.cms.service.aca4001.erase.spi;

import com.hn2.cms.service.aca4001.erase.support.RowUtils;
import com.hn2.cms.service.aca4001.erase.support.SqlNorm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class ProjectDrugOpenTarget extends AbstractSql2oTarget implements DependentEraseTarget {

    @Autowired
    public ProjectDrugOpenTarget(org.sql2o.Sql2o sql2o) {
        super(sql2o);
    }

    @Override
    public String table() {
        return "ProjectDrugOpen";
    }

    @Override
    public String parentTableName() {
        return "ProRec";
    } // 與 EraseCommand.tableToIds 的 key 對齊

    @Override
    public List<String> whitelistColumns() {
        return java.util.Arrays.asList(
                "CrmProNoticeDep",
                "CrmCrime",
                "CrmTerm",
                "CrmDisDate",
                "CrmDischarge",
                "Crime",
                "CrimeMemo",
                "Drug",
                "DrugMemo",
                "BehaviorPeriod",
                "BehaviorTimes",
                "HarmReduction",
                "DrugBehavior",
                "OtherMemo",
                "CorrectionRepeat",
                "CorrectionEffect",
                "FaiAppellation",
                "FaiCardNo",
                "FaiName",
                "FaiTel",
                "FaiAddress",
                "IsContact",
                "FileID",
                "FaiDrug",
                "FaiMemo",
                "FaiStatus",
                "FaiSupport",
                "FaiSupportMemo",
                "SocSupport",
                "SocSupportFriend",
                "SocSupportBoss",
                "SocSupportOther",
                "SocSupportAnalysis",
                "PreJob",
                "PreJobMemo",
                "Job",
                "JobMemo",
                "Live",
                "LiveMemo",
                "PsyStatus",
                "IsMeasures",
                "MeasuresMemo",
                "CloseReason",
                "ElseReason",
                "CreatedByUserID",
                "ModifiedByUserID"
        );
    }

    @Override
    public Set<String> dateColsNorm() {
        return java.util.Set.of("CRMDISDATE");
    }

    @Override
    public Set<String> intColsNorm() {
        // 可能為整數/數值型的欄位（視實際資料型別調整）
        return java.util.Set.of(
                "CREATEDBYUSERID", "MODIFIEDBYUSERID",
                "BEHAVIORTIMES", "FILEID"
        );
    }

    // by-ids 非主要用法（保留）
    @Override
    public List<Map<String, Object>> loadRowsByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) return java.util.Collections.emptyList();
        final String sql = "SELECT ID AS __PK__," + String.join(",", whitelistColumns())
                + " FROM dbo.ProjectDrugOpen WHERE ID IN (:ids)";
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
                + " FROM dbo.ProjectDrugOpen WHERE ProRecID IN (:pids)";
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
                "UPDATE dbo.ProjectDrugOpen SET " +
                        "  [CrmProNoticeDep]=NULL,[CrmCrime]=NULL,[CrmTerm]=NULL,[CrmDisDate]=NULL,[CrmDischarge]=NULL," +
                        "  [Crime]=NULL,[CrimeMemo]=NULL,[Drug]=NULL,[DrugMemo]=NULL," +
                        "  [BehaviorPeriod]=NULL,[BehaviorTimes]=NULL,[HarmReduction]=NULL,[DrugBehavior]=NULL,[OtherMemo]=NULL," +
                        "  [CorrectionRepeat]=NULL,[CorrectionEffect]=NULL," +
                        "  [FaiAppellation]=NULL,[FaiCardNo]=NULL,[FaiName]=NULL,[FaiTel]=NULL,[FaiAddress]=NULL," +
                        "  [IsContact]=NULL," + //      -- 若為 NOT NULL BIT，改為 0
                        "  [FileID]=NULL," +
                        "  [FaiDrug]=NULL,[FaiMemo]=NULL,[FaiStatus]=NULL,[FaiSupport]=NULL,[FaiSupportMemo]=NULL," +
                        "  [SocSupport]=NULL,[SocSupportFriend]=NULL,[SocSupportBoss]=NULL,[SocSupportOther]=NULL,[SocSupportAnalysis]=NULL," +
                        "  [PreJob]=NULL,[PreJobMemo]=NULL,[Job]=NULL,[JobMemo]=NULL,[Live]=NULL,[LiveMemo]=NULL," +
                        "  [PsyStatus]=NULL," +
                        "  [IsMeasures]=NULL," + //     -- 若為 NOT NULL BIT，改為 0
                        "  [MeasuresMemo]=NULL,[CloseReason]=NULL,[ElseReason]=NULL," +
                        "  [CreatedByUserID]=-2,[ModifiedByUserID]=-2," +
                        "  [isERASE]=1, [ModifiedOnDate]=SYSDATETIME() " +
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
                "UPDATE dbo.ProjectDrugOpen SET " +
                        "  [CrmProNoticeDep]=NULL,[CrmCrime]=NULL,[CrmTerm]=NULL,[CrmDisDate]=NULL,[CrmDischarge]=NULL," +
                        "  [Crime]=NULL,[CrimeMemo]=NULL,[Drug]=NULL,[DrugMemo]=NULL," +
                        "  [BehaviorPeriod]=NULL,[BehaviorTimes]=NULL,[HarmReduction]=NULL,[DrugBehavior]=NULL,[OtherMemo]=NULL," +
                        "  [CorrectionRepeat]=NULL,[CorrectionEffect]=NULL," +
                        "  [FaiAppellation]=NULL,[FaiCardNo]=NULL,[FaiName]=NULL,[FaiTel]=NULL,[FaiAddress]=NULL," +
                        "  [IsContact]=NULL, [FileID]=NULL," +
                        "  [FaiDrug]=NULL,[FaiMemo]=NULL,[FaiStatus]=NULL,[FaiSupport]=NULL,[FaiSupportMemo]=NULL," +
                        "  [SocSupport]=NULL,[SocSupportFriend]=NULL,[SocSupportBoss]=NULL,[SocSupportOther]=NULL,[SocSupportAnalysis]=NULL," +
                        "  [PreJob]=NULL,[PreJobMemo]=NULL,[Job]=NULL,[JobMemo]=NULL,[Live]=NULL,[LiveMemo]=NULL," +
                        "  [PsyStatus]=NULL,[IsMeasures]=NULL,[MeasuresMemo]=NULL,[CloseReason]=NULL,[ElseReason]=NULL," +
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
                throw new IllegalStateException("ProjectDrugOpen.restoreFromRows: __PK__ 不可為空, rowKeys=" + r.keySet());
            }
            var cleaned = new java.util.LinkedHashMap<String, Object>();
            for (String col : whitelistColumns()) {
                if ("ModifiedByUserID".equalsIgnoreCase(col)) continue; // 由系統填目前操作者
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