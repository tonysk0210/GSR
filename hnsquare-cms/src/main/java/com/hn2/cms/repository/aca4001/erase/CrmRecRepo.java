package com.hn2.cms.repository.aca4001.erase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class CrmRecRepo {
    private final org.sql2o.Sql2o sql2o;

    /**
     * 依 ACACardNo 查出已被塗銷 (isERASE=1) 的 CrmRec.ID 清單。
     * - ISNULL(isERASE,0)=1：避免 null 值造成判斷問題。
     */
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

        // 1) 將 ModifiedByUserID 視為系統控管欄位，禁止 mirror 覆寫
        java.util.Set<String> systemColsUpper = java.util.Set.of(
                "MODIFIEDONDATE", "CREATEDONDATE", "ISERASE", "ID",
                "MODIFIEDBYUSERID" // ★ 新增：禁止 mirror 設定
        );

        var cleaned = new java.util.LinkedHashMap<String, Object>();
        for (var e : fields.entrySet()) {
            String rawCol = e.getKey();
            if (rawCol == null) continue;
            String col = rawCol.trim();
            String colUpper = col.toUpperCase(java.util.Locale.ROOT);
            if (systemColsUpper.contains(colUpper)) continue; // ★ 過濾掉 ModifiedByUserID
            cleaned.put(col, e.getValue());
        }

        // 2) 動態 SET：mirror 欄位
        StringBuilder set = new StringBuilder();
        int i = 0;
        for (String col : cleaned.keySet()) {
            if (i++ > 0) set.append(", ");
            String param = col.replaceAll("[^A-Za-z0-9_]", "_");
            set.append('[').append(col).append(']').append(" = :").append(param);
        }

        // 3) 固定控管欄位：一律覆寫 ModifiedByUserID 與 ModifiedOnDate 與 isERASE
        if (set.length() > 0) set.append(", ");
        set.append("[isERASE] = 0, [ModifiedOnDate] = SYSUTCDATETIME(), [ModifiedByUserID] = :uid");

        String sql = "UPDATE dbo.CrmRec SET " + set + " WHERE [ID] = :id AND ISNULL([isERASE],0)=1";

        try (var con = sql2o.open()) {
            var q = con.createQuery(sql).addParameter("id", id).addParameter("uid", operatorUserId);

            // 綁定 mirror 動態欄位參數
            for (var e : cleaned.entrySet()) {
                String col = e.getKey();
                Object val = e.getValue();
                String param = col.replaceAll("[^A-Za-z0-9_]", "_");
                q.addParameter(param, val);
            }

            return q.executeUpdate().getResult();
        }
    }

    /**
     * 驗證 IDs 是否都屬於同一個 ACACardNo；
     * 回傳「不屬於」的 ID 清單（若清單為空代表都屬於同一個案）。
     */
    public List<String> findNotBelong(String acaCardNo, List<String> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        String sql = "SELECT ID FROM CrmRec WHERE ID IN (:ids) AND ACACardNo <> :aca";
        try (var con = sql2o.open()) {
            return con.createQuery(sql)
                    .addParameter("ids", ids) // sql2o 支援集合展開
                    .addParameter("aca", acaCardNo)
                    .executeAndFetch(String.class);
        }
    }

    /**
     * 檢查單筆是否已被塗銷（轉 BIT 再回傳 Boolean）。
     */
    public boolean isErased(String id) {
        String sql = "SELECT CAST(ISNULL(isERASE,0) AS BIT) FROM CrmRec WHERE ID=:id";
        try (var con = sql2o.open()) {
            Boolean b = con.createQuery(sql).addParameter("id", id).executeScalar(Boolean.class);
            return Boolean.TRUE.equals(b);
        }
    }

    /**
     * 讀取「白名單欄位」的當前值，轉成 Map 以利序列化成 JSON 保存到 mirror。
     * 這裡同時把 CreatedByUserID、ModifiedByUserID 也讀進來，讓 mirror 有完整還原資訊。
     */
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

    /**
     * 將白名單欄位清空並標記 isERASE=1。
     * - 這裡把 CreatedByUserID、ModifiedByUserID 都設為 -2（代表「被塗銷」的占位值）。
     * - ModifiedOnDate 使用 SYSUTCDATETIME() 做為系統時間戳記。
     * <p>
     * 注意：
     * - mask 變數目前未使用（僅保留示意）。你現在是直接設 NULL，而不是設置 "[ERASED]" 字樣。
     * - 若欄位為 int/日期等非 nvarchar，不適合改成字串，改 NULL 是正確做法。
     */
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
