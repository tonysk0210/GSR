package com.hn2.cms.repository.aca4001.erase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class CrmRecRepo {
    private final org.sql2o.Sql2o sql2o;

    public List<String> findErasedIdsByAcaCardNo(String acaCardNo) {
        String sql = "SELECT ID FROM dbo.CrmRec WHERE ACACardNo=:aca AND ISNULL(isERASE,0)=1";
        try (var con = sql2o.open()) {
            return con.createQuery(sql).addParameter("aca", acaCardNo).executeAndFetch(String.class);
        }
    }

    /**
     * 從 mirror map 寫回欄位，並將 isERASE 改回 0。
     * 注意：這裡允許還原 CreatedByUserID 與 ModifiedByUserID（不再視為系統欄位排除）。
     */
    public int restoreFieldsAndUnmarkErased(String id, Map<String, Object> fields, String operatorUserId) {
        if (fields == null || fields.isEmpty()) {
            // 只把狀態改回，更新修改者與時間
            String onlySys = "UPDATE dbo.CrmRec " +
                    "SET [isERASE]=0, [ModifiedByUserID]=:uid, [ModifiedOnDate]=SYSUTCDATETIME() " +
                    "WHERE [ID]=:id AND ISNULL([isERASE],0)=1";
            try (var con = sql2o.open()) {
                return con.createQuery(onlySys)
                        .addParameter("id", id)
                        .addParameter("uid", operatorUserId)
                        .executeUpdate()
                        .getResult();
            }
        }

        // 忽略大小寫與空白的系統欄位過濾
        java.util.Set<String> systemColsUpper = java.util.Set.of(
                "MODIFIEDBYUSERID", "CREATEDBYUSERID", "MODIFIEDONDATE", "CREATEDONDATE",
                "ISERASE", "ID"
        );

        // 清理：移除系統欄位、修剪空白、保留原值
        var cleaned = new java.util.LinkedHashMap<String, Object>();
        for (var e : fields.entrySet()) {
            String rawCol = e.getKey();
            if (rawCol == null) continue;
            String col = rawCol.trim();
            String colUpper = col.toUpperCase(java.util.Locale.ROOT);
            if (systemColsUpper.contains(colUpper)) continue; // 這些欄位不從 mirror 還原
            cleaned.put(col, e.getValue());
        }

        // 動態組 SET 子句
        StringBuilder set = new StringBuilder();
        int i = 0;
        for (String col : cleaned.keySet()) {
            if (i++ > 0) set.append(", ");
            // 用 [] 包住欄位名；參數名用去掉非字母數字的安全版本
            String param = col.replaceAll("[^A-Za-z0-9_]", "_");
            set.append('[').append(col).append(']').append(" = :").append(param);
        }

        // 系統控管欄位（一定要加）——避免重複逗號
        if (set.length() > 0) set.append(", ");
        set.append("[isERASE] = 0, [ModifiedByUserID] = :uid, [ModifiedOnDate] = SYSUTCDATETIME()");

        String sql = "UPDATE dbo.CrmRec SET " + set + " WHERE [ID] = :id AND ISNULL([isERASE],0)=1";

        try (var con = sql2o.open()) {
            var q = con.createQuery(sql)
                    .addParameter("id", id)
                    .addParameter("uid", operatorUserId);

            // 綁定參數（用剛才產生的安全參數名）
            for (var e : cleaned.entrySet()) {
                String col = e.getKey();
                Object val = e.getValue();
                String param = col.replaceAll("[^A-Za-z0-9_]", "_");
                q.addParameter(param, val);
            }

            return q.executeUpdate().getResult();
        }
    }

    // 驗證所選 IDs 是否屬於該 ACACardNo
    public List<String> findNotBelong(String acaCardNo, List<String> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        String sql = "SELECT ID FROM CrmRec WHERE ID IN (:ids) AND ACACardNo <> :aca";
        try (var con = sql2o.open()) {
            return con.createQuery(sql)
                    .addParameter("ids", ids)
                    .addParameter("aca", acaCardNo)
                    .executeAndFetch(String.class);
        }
    }

    // 是否已被塗銷
    public boolean isErased(String id) {
        String sql = "SELECT CAST(ISNULL(isERASE,0) AS BIT) FROM CrmRec WHERE ID=:id";
        try (var con = sql2o.open()) {
            Boolean b = con.createQuery(sql).addParameter("id", id).executeScalar(Boolean.class);
            return Boolean.TRUE.equals(b);
        }
    }

    // 讀白名單欄位 → Map（方便組 JSON）
    public Map<String, Object> loadSensitiveFields(String id) {
        String sql = "SELECT ProSource1, ProNoticeDep, " +
                "CrmCrime1, CrmCrime2, CrmCrime3, " +
                "CrmTerm, CrmChaDate, CrmDischarge, CrmDisDate, " +
                "CrmTrain, CrmCert, CrmMemo, " +
                "CrmRemission, Crm_ReleaseDate, Crm_Release, " +
                "Crm_NoJail, Crm_Sentence, Crm_VerdictDate, " +
                "CreatedByUserID, ModifiedByUserID " +
                "FROM CrmRec WHERE ID=:id";
        try (var con = sql2o.open()) {
            var t = con.createQuery(sql).addParameter("id", id).executeAndFetchTable();
            return t.rows().isEmpty() ? null : t.rows().get(0).asMap();
        }
    }

    // 清空白名單欄位 + 標記 isERASE=1
    public void nullifyAndMarkErased(String id) {

        String mask = "[ERASED]"; // 或 "[ERASED]"

        String sql = "UPDATE CrmRec SET " +
                "ProSource1=NULL, ProNoticeDep=NULL, " +
                "CrmCrime1=NULL, CrmCrime2=NULL, CrmCrime3=NULL, " +
                "CrmTerm=NULL, CrmChaDate=NULL, CrmDischarge=NULL, CrmDisDate=NULL, " +
                "CrmTrain=NULL, CrmCert=NULL, CrmMemo=NULL, " +
                "CrmRemission=NULL, Crm_ReleaseDate=NULL, Crm_Release=NULL, " +
                "Crm_NoJail=NULL, Crm_Sentence=NULL, Crm_VerdictDate=NULL, " +
                "CreatedByUserID=-2, ModifiedByUserID=-2, " +
                "isERASE=1, " +
                "ModifiedOnDate=SYSUTCDATETIME() " +
                "WHERE ID=:id";
        try (var con = sql2o.open()) {
            con.createQuery(sql).addParameter("id", id).executeUpdate();
        }
    }
}
