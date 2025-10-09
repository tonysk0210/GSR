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
     * 單筆：還原動作（整個 API 呼叫只寫一次）
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
     * 單筆：塗銷動作（整個 API 呼叫只寫一次）
     */
    public void insertEraseAction(String acaCardNo, Integer docNum, String eraseReason, Integer createdByUserId, String userIp) {
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

    public void deleteByAcaCardNo(String acaCardNo) {
        final String sql = "DELETE FROM dbo.ACA_EraseMirror WHERE ACACardNo = :aca";
        try (var con = sql2o.open()) {
            con.createQuery(sql)
                    .addParameter("aca", acaCardNo)
                    .executeUpdate();
        }
    }

}

