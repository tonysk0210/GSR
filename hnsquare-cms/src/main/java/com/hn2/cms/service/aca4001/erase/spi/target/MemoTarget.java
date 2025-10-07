package com.hn2.cms.service.aca4001.erase.spi.target;

import com.hn2.cms.service.aca4001.erase.spi.AbstractSql2oTarget;
import com.hn2.cms.service.aca4001.erase.spi.DependentEraseTarget;
import com.hn2.cms.service.aca4001.erase.support.RowUtils;
import com.hn2.cms.service.aca4001.erase.support.SqlNorm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Order(3) // after ACAFamilies & Career, before ACABrd
public class MemoTarget extends AbstractSql2oTarget implements DependentEraseTarget {

    @Autowired
    public MemoTarget(org.sql2o.Sql2o sql2o) {
        super(sql2o);
    }

    @Override
    public String table() {
        return "Memo";
    }

    @Override
    public String parentTableName() {
        return "ACABrd";
    } // parent ids = ACACardNo

    @Override
    public List<String> whitelistColumns() {
        return List.of(
                "MemoDate",
                "MemoNote",
                "MemoWorkers",
                "CreatedByUserID",
                "ModifiedByUserID");
    }

    @Override
    public Set<String> dateColsNorm() {
        return Set.of("MEMODATE");
    }

    @Override
    public Set<String> intColsNorm() {
        return Set.of("CREATEDBYUSERID", "MODIFIEDBYUSERID");
    }

    @Override
    public List<Map<String, Object>> loadRowsByParentIds(List<String> acaCardNos) {
        if (acaCardNos == null || acaCardNos.isEmpty()) return Collections.emptyList();
        final String sql = "SELECT ID AS __PK__," + String.join(",", whitelistColumns()) +
                " FROM dbo.Memo WHERE ACACardNo IN (:pids)";
        var all = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < acaCardNos.size(); i += 1000) {
            var sub = acaCardNos.subList(i, Math.min(i + 1000, acaCardNos.size()));
            try (var con = sql2o.open()) {
                var t = con.createQuery(sql).addParameter("pids", sub).executeAndFetchTable();
                all.addAll(t.rows().stream().map(org.sql2o.data.Row::asMap).collect(Collectors.toList()));
            }
        }
        return all;
    }

    @Override
    public int nullifyAndMarkErasedByParent(List<String> acaCardNos) {
        if (acaCardNos == null || acaCardNos.isEmpty()) return 0;
        final String sql =
                "UPDATE dbo.Memo SET " +
                        "  [MemoDate]=NULL, [MemoNote]=NULL, [MemoWorkers]=NULL, " +
                        "  [CreatedByUserID]=-2, [ModifiedByUserID]=-2, " +
                        "  [isERASE]=1, [ModifiedOnDate]=SYSDATETIME() " +
                        "WHERE ACACardNo IN (:pids)";
        try (var con = sql2o.open()) {
            return con.createQuery(sql).addParameter("pids", acaCardNos).executeUpdate().getResult();
        }
    }

    @Override
    public List<Map<String, Object>> loadRowsByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyList();
        final String sql = "SELECT ID AS __PK__," + String.join(",", whitelistColumns()) +
                " FROM dbo.Memo WHERE ID IN (:ids)";
        try (var con = sql2o.open()) {
            var t = con.createQuery(sql).addParameter("ids", ids).executeAndFetchTable();
            return t.rows().stream().map(org.sql2o.data.Row::asMap).collect(Collectors.toList());
        }
    }

    @Override
    public int nullifyAndMarkErased(List<String> ids) {
        if (ids == null || ids.isEmpty()) return 0;
        final String sql =
                "UPDATE dbo.Memo SET " +
                        "  [MemoDate]=NULL, [MemoNote]=NULL, [MemoWorkers]=NULL, " +
                        "  [CreatedByUserID]=-2, [ModifiedByUserID]=-2, " +
                        "  [isERASE]=1, [ModifiedOnDate]=SYSDATETIME() " +
                        "WHERE ID IN (:ids)";
        try (var con = sql2o.open()) {
            return con.createQuery(sql).addParameter("ids", ids).executeUpdate().getResult();
        }
    }

    @Override
    public int restoreFromRows(List<Map<String, Object>> rows, String operatorUserId) {
        int total = 0;
        Integer uidInt = SqlNorm.tryParseInt(operatorUserId);
        for (var r : rows) {
            String id = RowUtils.toStringCI(r, "__PK__");
            if (id == null || id.isBlank()) throw new IllegalStateException("Memo.restoreFromRows: __PK__ 不可為空");
            var cleaned = new LinkedHashMap<String, Object>();
            for (String col : whitelistColumns()) {
                if ("ModifiedByUserID".equalsIgnoreCase(col)) continue;
                Object raw = RowUtils.getCI(r, col);
                Object norm = SqlNorm.normalizeForColumn(col, raw, dateColsNorm(), intColsNorm());
                cleaned.put(col, norm);
            }
            total += updateWithDynamicSet(
                    id, cleaned,
                    ", [isERASE]=0, [ModifiedOnDate]=SYSDATETIME(), [ModifiedByUserID]=:uid",
                    q -> q.addParameter("uid", uidInt != null ? uidInt : operatorUserId)
            );
        }
        return total;
    }
}

