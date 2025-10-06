package com.hn2.cms.service.aca4001.erase.spi;

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
        return "CrmRec"; // 這個 SPI 服務負責 dbo.CrmRec
    }

    // 決定要鏡像/清空/還原的欄位白名單（敏感 + 審計必要欄位）
    @Override
    public List<String> whitelistColumns() {
        return List.of("ProSource1", "ProNoticeDep",
                "CrmCrime1", "CrmCrime2", "CrmCrime3",
                "CrmTerm", "CrmChaDate", "CrmDischarge", "CrmDisDate",
                "CrmTrain", "CrmCert", "CrmMemo",
                "CrmRemission", "Crm_ReleaseDate", "Crm_Release",
                "Crm_NoJail", "Crm_Sentence", "Crm_VerdictDate",
                "CreatedByUserID", "ModifiedByUserID");
    }

    // 指出哪些欄位屬於日期（大寫名稱，配合 SqlNorm / RowUtils 大寫比較）
    @Override
    public Set<String> dateColsNorm() {
        return Set.of("CRMCHADATE", "CRMDISDATE", "CRMRELEASEDATE", "CRMVERDICTDATE");
    }

    // 指出哪些欄位屬於整數
    @Override
    public Set<String> intColsNorm() {
        return Set.of("CREATEDBYUSERID", "MODIFIEDBYUSERID", "CRMSENTENCE", "CRMTERM");
    }

    // 大量主鍵查詢時用共用的「分片查詢」
    @Override
    public List<Map<String, Object>> loadRowsByIds(List<String> ids) {
        return fetchRowsByIdsChunked(ids, 1000);
    }

    // ★「塗銷」：白名單欄位全部清空（NULL），但 Created/ModifiedByUserID 設為 -2，並把 isERASE=1
    @Override
    public int nullifyAndMarkErased(List<String> ids) {
        StringBuilder sb = new StringBuilder("UPDATE dbo.CrmRec SET ");
        for (int i = 0; i < whitelistColumns().size(); i++) {
            String c = whitelistColumns().get(i);
            if (i > 0) sb.append(", ");
            if (c.equalsIgnoreCase("CreatedByUserID") || c.equalsIgnoreCase("ModifiedByUserID")) {
                sb.append("[").append(c).append("] = -2"); // 身分欄位以 sentinel 值 -2 表示「已塗銷」
            } else {
                sb.append("[").append(c).append("] = NULL"); // 其餘全部清空
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

    // ★「還原」：用鏡像回填白名單欄位，ModifiedByUserID 以操作者覆寫；並 isERASE=0
    @Override
    public int restoreFromRows(List<Map<String, Object>> rows, String operatorUserId) {
        int total = 0;
        Integer operatorUidInt = SqlNorm.tryParseInt(operatorUserId); // 能轉 int 就傳 int，否則傳字串

        for (var r : rows) {
            String id = RowUtils.toStringCI(r, "__PK__"); // 取出查詢時包成的主鍵別名
            if (id == null || id.isBlank()) {
                throw new IllegalStateException("CrmRecTarget.restoreFromRows: __PK__ 不可為空, rowKeys=" + r.keySet());
            }
            var cleaned = new java.util.LinkedHashMap<String, Object>();
            for (String col : whitelistColumns()) {
                if ("ModifiedByUserID".equalsIgnoreCase(col)) continue; // 這欄改由操作者覆寫，不用鏡像值
                Object raw = RowUtils.getCI(r, col); // 取欄位值（大小寫不敏感）
                Object norm = SqlNorm.normalizeForColumn(col, raw, dateColsNorm(), intColsNorm()); // 依欄位型別做轉換：日期/整數/文字
                cleaned.put(col, norm);
            }

            // 用共用的動態 SET 函數組 UPDATE；suffix 內把 isERASE=0 + ModifiedOnDate + ModifiedByUserID 帶回
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

