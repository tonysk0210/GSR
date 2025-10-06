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
        return "ProRec"; // 這個 Target 負責 dbo.ProRec
    }

    @Override
    public List<String> whitelistColumns() {
        // 只列「需要鏡像、塗銷、還原」的欄位（白名單）
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
        return java.util.Collections.emptySet(); // 本白名單無日期欄
    }

    @Override
    public Set<String> intColsNorm() {
        // 指出需要做整數正規化的欄位（大寫以便大小寫不敏感比對）
        return java.util.Set.of("HASPREVIOUSPLIGHT", "CREATEDBYUSERID", "MODIFIEDBYUSERID");
    }

    @Override
    public List<Map<String, Object>> loadRowsByIds(List<String> ids) {
        // 用抽象基底的分片查詢（避免 IN 過長）
        return fetchRowsByIdsChunked(ids, 1000);
    }

    @Override
    public int nullifyAndMarkErased(List<String> ids) {
        // 逐一組出白名單欄位的 SET 子句：
        // - CreatedBy/ModifiedByUserID -> -2（sentinel，避免 NOT NULL 衝突且易於辨識「被塗銷」）
        // - 其餘欄位 -> NULL
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
        // 標記 isERASE=1，更新時間
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
        Integer operatorUidInt = SqlNorm.tryParseInt(operatorUserId); // 盡量以 int 綁參

        for (var r : rows) {
            String id = RowUtils.toStringCI(r, "__PK__"); // 取出主鍵（查詢時以 ID AS __PK__ 回來）
            if (id == null || id.isBlank()) {
                throw new IllegalStateException("ProRecTarget.restoreFromRows: __PK__ 不可為空, rowKeys=" + r.keySet());
            }
            // 準備要回填的欄位（白名單），同時做型別正規化（日期/整數/字串）
            var cleaned = new java.util.LinkedHashMap<String, Object>();
            for (String col : whitelistColumns()) {
                if ("ModifiedByUserID".equalsIgnoreCase(col)) continue; // 還原時這欄改由「現在操作者」覆寫
                Object raw = RowUtils.getCI(r, col);
                Object norm = SqlNorm.normalizeForColumn(col, raw, dateColsNorm(), intColsNorm());
                cleaned.put(col, norm);
            }
            // 以抽象基底的共用方法，動態產出 UPDATE SET
            // 後綴 suffix：isERASE=0 / ModifiedOnDate / ModifiedByUserID=當前操作者
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