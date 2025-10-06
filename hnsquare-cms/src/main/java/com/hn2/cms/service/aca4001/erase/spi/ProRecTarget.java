package com.hn2.cms.service.aca4001.erase.spi;

import com.hn2.cms.service.aca4001.erase.support.RowUtils;
import com.hn2.cms.service.aca4001.erase.support.SqlNorm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class ProRecTarget extends AbstractSql2oTarget {

    @Autowired
    public ProRecTarget(org.sql2o.Sql2o sql2o) {
        super(sql2o);
    }

    @Override
    public String table() {
        return "ProRec";
    }

    @Override
    public List<String> whitelistColumns() {
        // ★ 僅列入需要鏡像/塗銷/還原的欄位
        return java.util.Arrays.asList(
                "ProPlight",
                "HasPreviousPlight",
                "PreviousPlightChangedDesc",
                "ProStatus",
                "ProFile",
                "ProMemo",
                "ProWorkerBackup",
                "CreatedByUserID",
                "ModifiedByUserID"
        );
    }

    @Override
    public Set<String> dateColsNorm() {
        // 若白名單內沒有日期欄，可回傳空集合
        return java.util.Collections.emptySet();
    }

    @Override
    public Set<String> intColsNorm() {
        // HasPreviousPlight（若為 bit）、CreatedByUserID、ModifiedByUserID 可能是數字型
        return java.util.Set.of("HASPREVIOUSPLIGHT", "CREATEDBYUSERID", "MODIFIEDBYUSERID");
    }

    @Override
    public List<Map<String, Object>> loadRowsByIds(List<String> ids) {
        return fetchRowsByIdsChunked(ids, 1000);
    }

    @Override
    public int nullifyAndMarkErased(List<String> ids) {
        // 照 CrmRecTarget 寫法：ID IN (:ids)，白名單欄位逐一設 NULL，唯 CreatedBy/ModifiedByUserID 設 -2
        StringBuilder sb = new StringBuilder("UPDATE dbo.ProRec SET ");
        for (int i = 0; i < whitelistColumns().size(); i++) {
            String c = whitelistColumns().get(i);
            if (i > 0) sb.append(", ");
            if (c.equalsIgnoreCase("CreatedByUserID") || c.equalsIgnoreCase("ModifiedByUserID")) {
                sb.append("[").append(c).append("] = -2");
            } else {
                sb.append("[").append(c).append("] = NULL");
            }
        }
        sb.append(", [isERASE]=1, [ModifiedOnDate]=SYSDATETIME() WHERE ID IN (:ids)");
        try (var con = sql2o.open()) {
            return con.createQuery(sb.toString())
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
                throw new IllegalStateException("ProRecTarget.restoreFromRows: __PK__ 不可為空, rowKeys=" + r.keySet());
            }
            var cleaned = new java.util.LinkedHashMap<String, Object>();
            for (String col : whitelistColumns()) {
                // 和 CrmRec 一樣：還原時 ModifiedByUserID 交由系統寫入目前操作者
                if ("ModifiedByUserID".equalsIgnoreCase(col)) continue;
                Object raw = RowUtils.getCI(r, col);
                Object norm = SqlNorm.normalizeForColumn(col, raw, dateColsNorm(), intColsNorm());
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