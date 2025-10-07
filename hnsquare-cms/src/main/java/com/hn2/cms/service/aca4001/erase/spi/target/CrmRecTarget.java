package com.hn2.cms.service.aca4001.erase.spi.target;

import com.hn2.cms.service.aca4001.erase.spi.AbstractSql2oTarget;
import com.hn2.cms.service.aca4001.erase.support.RowUtils;
import com.hn2.cms.service.aca4001.erase.support.SqlNorm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class CrmRecTarget extends AbstractSql2oTarget {

    @Autowired
    public CrmRecTarget(org.sql2o.Sql2o sql2o) {
        super(sql2o);
    }

    @Override
    public String table() {
        return "CrmRec";
    }

    @Override
    public List<String> whitelistColumns() {
        return List.of("ProSource1",
                "ProNoticeDep",
                "CrmCrime1",
                "CrmCrime2",
                "CrmCrime3",
                "CrmTerm",
                "CrmChaDate",
                "CrmDischarge",
                "CrmDisDate",
                "CrmTrain",
                "CrmCert",
                "CrmMemo",
                "CrmRemission",
                "Crm_ReleaseDate",
                "Crm_Release",
                "Crm_NoJail",
                "Crm_Sentence",
                "Crm_VerdictDate",
                "CreatedByUserID",
                "ModifiedByUserID");
    }

    @Override
    public Set<String> dateColsNorm() {
        return Set.of("CRMCHADATE", "CRMDISDATE", "CRMRELEASEDATE", "CRMVERDICTDATE");
    }

    @Override
    public Set<String> intColsNorm() {
        return Set.of("CREATEDBYUSERID", "MODIFIEDBYUSERID", "CRMSENTENCE", "CRMTERM");
    }

    @Override
    public List<Map<String, Object>> loadRowsByIds(List<String> ids) {
        return fetchRowsByIdsChunked(ids, 1000);
    }

    @Override
    public int nullifyAndMarkErased(List<String> ids) {
        StringBuilder sb = new StringBuilder("UPDATE dbo.CrmRec SET ");
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
                throw new IllegalStateException("CrmRecTarget.restoreFromRows: __PK__ 不可為空, rowKeys=" + r.keySet());
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
                    ", [isERASE]=0, [ModifiedOnDate]=SYSUTCDATETIME(), [ModifiedByUserID]=:uid",
                    q -> q.addParameter("uid", operatorUidInt != null ? operatorUidInt : operatorUserId)
            );
        }
        return total;
    }
}

