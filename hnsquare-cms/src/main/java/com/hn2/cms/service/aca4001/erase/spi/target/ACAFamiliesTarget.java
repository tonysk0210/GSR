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
@Order(1) // must run BEFORE ACABrd (needs FamCardNo still present)
public class ACAFamiliesTarget extends AbstractSql2oTarget implements DependentEraseTarget {

    @Autowired
    public ACAFamiliesTarget(org.sql2o.Sql2o sql2o) {
        super(sql2o);
    }

    @Override
    public String table() {
        return "ACAFamilies";
    }

    @Override
    public String parentTableName() {
        return "ACABrd";
    } // parent ids = ACACardNo

    @Override
    public List<String> whitelistColumns() {
        return List.of(
                "FaiIDNo",
                "FaiName",
                "FaiSex",
                "FaiNationality",
                "IsContact",
                "FaiBrith",
                "FaiAddress",
                "IsAcaAddress",
                "FaiTel",
                "IsAcaTel",
                "FaiMobile",
                "FaiAppellation",
                "FaiPassport",
                "FaiCareer",
                "FaiOther",
                "FaiPostal",
                "IsDead",
                "ACACardNo",
                "CreatedByUserID",
                "ModifiedByUserID"
        );
    }

    @Override
    public Set<String> dateColsNorm() {
        return Set.of("FAIBRITH");
    }

    @Override
    public Set<String> intColsNorm() {
        return Set.of("CREATEDBYUSERID", "MODIFIEDBYUSERID");
    }

    private static final Set<String> BIT_TO_ZERO = Set.of();

    @Override
    public List<Map<String, Object>> loadRowsByParentIds(List<String> acaCardNos) {
        if (acaCardNos == null || acaCardNos.isEmpty()) return Collections.emptyList();
        String selectCols = "f.ID AS __PK__," +
                whitelistColumns().stream()
                        .map(c -> "f.[" + c + "] AS [" + c + "]")
                        .collect(java.util.stream.Collectors.joining(","));

        final String sql =
                "SELECT " + selectCols + " " +
                        "FROM dbo.ACAFamilies f " +
                        "JOIN dbo.ACABrd b ON b.FamCardNo = f.FamCardNo " +
                        "WHERE b.ACACardNo IN (:pids)";
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

        var sb = new StringBuilder()
                .append("UPDATE f SET ");

        var cols = whitelistColumns();
        for (int i = 0; i < cols.size(); i++) {
            if (i > 0) sb.append(", ");
            String c = cols.get(i);

            if ("CreatedByUserID".equalsIgnoreCase(c) || "ModifiedByUserID".equalsIgnoreCase(c)) {
                sb.append("f.[").append(c).append("] = -2");
            } else if (BIT_TO_ZERO.contains(c.toUpperCase(Locale.ROOT))) {
                sb.append("f.[").append(c).append("] = 0");
            } else {
                sb.append("f.[").append(c).append("] = NULL");
            }
        }

        // 若表中沒有 isERASE / ModifiedOnDate，請刪掉這兩個欄位的設定
        sb.append(", f.[isERASE] = 1, f.[ModifiedOnDate] = SYSDATETIME() ")
                .append("FROM dbo.ACAFamilies f ")
                .append("JOIN dbo.ACABrd b ON b.FamCardNo = f.FamCardNo ")
                .append("WHERE b.ACACardNo IN (:pids)");

        try (var con = sql2o.open()) {
            return con.createQuery(sb.toString())
                    .addParameter("pids", acaCardNos)
                    .executeUpdate()
                    .getResult();
        }
    }

    @Override
    public List<Map<String, Object>> loadRowsByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyList();
        final String sql = "SELECT ID AS __PK__," + String.join(",", whitelistColumns()) +
                " FROM dbo.ACAFamilies WHERE ID IN (:ids)";
        try (var con = sql2o.open()) {
            var t = con.createQuery(sql).addParameter("ids", ids).executeAndFetchTable();
            return t.rows().stream().map(org.sql2o.data.Row::asMap).collect(Collectors.toList());
        }
    }

    @Override
    public int nullifyAndMarkErased(List<String> ids) {
        if (ids == null || ids.isEmpty()) return 0;
        var sb = new StringBuilder("UPDATE dbo.ACAFamilies SET ");
        var cols = whitelistColumns();
        for (int i = 0; i < cols.size(); i++) {
            if (i > 0) sb.append(", ");
            String c = cols.get(i);
            if ("CreatedByUserID".equalsIgnoreCase(c) || "ModifiedByUserID".equalsIgnoreCase(c)) {
                sb.append('[').append(c).append("] = -2");
            } else if (BIT_TO_ZERO.contains(c.toUpperCase(Locale.ROOT))) {
                sb.append('[').append(c).append("] = 0");
            } else {
                sb.append('[').append(c).append("] = NULL");
            }
        }
        sb.append(", [isERASE] = 1, [ModifiedOnDate] = SYSDATETIME() WHERE ID IN (:ids)");
        try (var con = sql2o.open()) {
            return con.createQuery(sb.toString()).addParameter("ids", ids).executeUpdate().getResult();
        }
    }

    @Override
    public int restoreFromRows(List<Map<String, Object>> rows, String operatorUserId) {
        int total = 0;
        Integer uidInt = SqlNorm.tryParseInt(operatorUserId);
        for (var r : rows) {
            String id = RowUtils.toStringCI(r, "__PK__");
            if (id == null || id.isBlank())
                throw new IllegalStateException("ACAFamilies.restoreFromRows: __PK__ 不可為空");
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
