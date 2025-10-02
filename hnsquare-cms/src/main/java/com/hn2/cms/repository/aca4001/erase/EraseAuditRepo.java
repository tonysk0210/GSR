package com.hn2.cms.repository.aca4001.erase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class EraseAuditRepo {
    private final org.sql2o.Sql2o sql2o;

    /**
     * 新增一筆「還原 RESTORE」操作的稽核紀錄。
     *
     * @param schema        資料來源 schema（預設為 "dbo"）
     * @param table         資料來源表名稱（如 CrmRec）
     * @param id            被還原的紀錄主鍵
     * @param acaCardNo     個案卡號
     * @param restoreReason 還原原因（操作人輸入）
     * @param userIp        操作者 IP
     * @param createdByUserId        操作者使用者 ID
     */
    public void insertRestore(String schema, String table, String id, String acaCardNo,
                              String restoreReason,
                              String createdByUserId, String userIp) {

        final String sql =
                "INSERT INTO dbo.ACA_EraseAudit ("
                        + " TargetSchema, TargetTable, TargetID, ACACardNo,"
                        + " ActionType, IsErased,"
                        + " DocNum, EraseReason, RestoreReason,"
                        + " CreatedByUserID, UserIP"
                        + ") VALUES ("
                        + " :schema, :tbl, :tid, :aca,"
                        + " 'RESTORE', 0,"
                        + " NULL, NULL, :rsn,"
                        + " :uid, :uip"
                        + ")";

        final String s = (schema == null || schema.isBlank()) ? "dbo" : schema;

        try (var con = sql2o.open()) {
            con.createQuery(sql)
                    .addParameter("schema", s)
                    .addParameter("tbl", table)
                    .addParameter("tid", id)
                    .addParameter("aca", acaCardNo)
                    .addParameter("rsn", restoreReason)
                    .addParameter("uid", createdByUserId)
                    .addParameter("uip", userIp)
                    .executeUpdate();
        }
    }


    /**
     * 新增一筆「塗銷 ERASE」操作的稽核紀錄。
     *
     * @param schema      資料來源 schema（預設 "dbo"）
     * @param table       資料來源表名稱（如 CrmRec）
     * @param id          被塗銷的紀錄主鍵
     * @param acaCardNo   個案卡號
     * @param docNum      文件號（與業務文件關聯用，可為 null）
     * @param eraseReason 塗銷原因（操作人輸入）
     * @param userIp      操作者 IP
     * @param createdByUserId      操作者使用者 ID
     */
    public void insertErase(String schema, String table, String id, String acaCardNo,
                            Integer docNum, String eraseReason,
                            String createdByUserId, String userIp) {

        final String sql =
                "INSERT INTO dbo.ACA_EraseAudit ("
                        + " TargetSchema, TargetTable, TargetID, ACACardNo,"
                        + " ActionType, IsErased,"
                        + " DocNum, EraseReason, RestoreReason,"
                        + " CreatedByUserID, UserIP"
                        + ") VALUES ("
                        + " :schema, :tbl, :tid, :aca,"
                        + " 'ERASE', 1,"
                        + " :doc, :rsn, NULL,"
                        + " :uid, :uip"
                        + ")";

        final String s = (schema == null || schema.isBlank()) ? "dbo" : schema;

        try (var con = sql2o.open()) {
            con.createQuery(sql)
                    .addParameter("schema", s)
                    .addParameter("tbl", table)
                    .addParameter("tid", id)
                    .addParameter("aca", acaCardNo)
                    .addParameter("doc", docNum)
                    .addParameter("rsn", eraseReason)
                    .addParameter("uid", createdByUserId)
                    .addParameter("uip", userIp)
                    .executeUpdate();
        }
    }
}

