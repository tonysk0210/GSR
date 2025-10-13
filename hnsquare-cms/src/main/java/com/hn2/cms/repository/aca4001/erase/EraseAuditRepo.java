package com.hn2.cms.repository.aca4001.erase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class EraseAuditRepo {
    private final org.sql2o.Sql2o sql2o;

    /**
     * 寫入一筆「塗銷 (ERASE)」稽核記錄至 ACA_EraseAudit。
     * <p>
     * 寫入內容：
     * - ActionType 固定為 'ERASE'
     * - DocNum：此次作業單號（可為 null）
     * - EraseReason：塗銷原因（可為 null）
     * - RestoreReason：固定 NULL（此方法只處理塗銷）
     * - CreatedByUserID：操作者（Integer）
     * - UserIP：來源 IP
     * - CreatedOnDate：使用 SYSDATETIME() 於 DB 端產生
     * <p>
     * 例外處理：
     * - 任何 DB 錯誤將由 Sql2o/JDBC 拋出 Runtime 例外，交由上層交易控管回滾。
     *
     * @param acaCardNo       個案卡號（不可為空）
     * @param docNum          作業單號（可為 null）
     * @param eraseReason     塗銷原因（可為 null）
     * @param createdByUserId 操作者 ID（不可為空，型別需與 DB 欄位一致）
     * @param userIp          來源 IP（可為 null）
     */
    public void insertEraseAction(String acaCardNo, Integer docNum, String eraseReason, Integer createdByUserId, String userIp) {
        // 準備 INSERT：ActionType='ERASE'；RestoreReason 固定 NULL；CreatedOnDate 由 DB 端 SYSDATETIME() 產生
        final String sql =
                "INSERT INTO dbo.ACA_EraseAudit (" +
                        "  ACACardNo, ActionType, DocNum, EraseReason, RestoreReason," +
                        "  CreatedByUserID, UserIP, CreatedOnDate" +
                        ") VALUES (" +
                        "  :aca, 'ERASE', :doc, :ers, NULL," +
                        "  :uid, :uip, SYSDATETIME()" +
                        ")";
        try (var con = sql2o.open()) {
            con.createQuery(sql)
                    .addParameter("aca", acaCardNo)
                    .addParameter("doc", docNum)
                    .addParameter("ers", eraseReason)
                    .addParameter("uid", createdByUserId)
                    .addParameter("uip", userIp)
                    .executeUpdate();
        }
    }

    /**
     * 寫入一筆「還原 (RESTORE)」塗銷異動記錄至 ACA_EraseAudit。
     * 寫入內容：
     * - ActionType 固定 'RESTORE'
     * - DocNum、EraseReason 皆為 NULL（此方法僅處理還原）
     * - RestoreReason、CreatedByUserID、UserIP 依參數寫入
     * - CreatedOnDate 使用 SYSDATETIME()（本機時間）
     *
     * @param acaCardNo       個案卡號（不可為空）
     * @param restoreReason   還原原因（可為 null）
     * @param createdByUserId 建檔/操作者 ID（若 DB 欄位為 INT，建議改為 Integer 型別）
     * @param userIp          來源 IP（可為 null）
     */
    public void insertRestoreAction(String acaCardNo, String restoreReason, String createdByUserId, String userIp) {
        final String sql =
                "INSERT INTO dbo.ACA_EraseAudit (" +
                        "  ACACardNo, ActionType, DocNum, EraseReason, RestoreReason," +
                        "  CreatedByUserID, UserIP, CreatedOnDate" +
                        ") VALUES (" +
                        "  :aca, 'RESTORE', NULL, NULL, :rs," +
                        "  :uid, :uip, SYSDATETIME()" +
                        ")";
        try (var con = sql2o.open()) {
            con.createQuery(sql)
                    .addParameter("aca", acaCardNo)
                    .addParameter("rs", restoreReason)
                    .addParameter("uid", createdByUserId)
                    .addParameter("uip", userIp)
                    .executeUpdate();
        }
    }

    /**
     * 依 ACACardNo 刪除 ACA_EraseMirror 中屬於該個案的所有鏡像資料。
     * 典型情境：
     * - 還原流程成功後，清掉此個案的鏡像（避免被重覆還原或占用空間）。
     * 行為：
     * - 無資料可刪時不會拋錯（受影響筆數為 0）。
     * - 任何 DB 錯誤由 Sql2o/JDBC 拋出 Runtime 例外，上層可用交易回滾。
     *
     * @param acaCardNo 個案卡號（不可為空）
     */
    public void deleteByAcaCardNo(String acaCardNo) {
        final String sql = "DELETE FROM dbo.ACA_EraseMirror WHERE ACACardNo = :aca";
        try (var con = sql2o.open()) {
            con.createQuery(sql)
                    .addParameter("aca", acaCardNo)
                    .executeUpdate(); // 執行刪除；無符合資料則影響筆數為 0
        }
    }

}

