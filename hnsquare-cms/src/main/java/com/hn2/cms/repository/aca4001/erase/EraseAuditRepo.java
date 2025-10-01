package com.hn2.cms.repository.aca4001.erase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class EraseAuditRepo {
    private final org.sql2o.Sql2o sql2o;

    public void insertRestore(String schema, String table, String id, String acaCardNo,
                              String restoreReason,
                              String userName, String userIp,
                              String branchId, String userId) {
        String sql = "INSERT INTO dbo.ACA_EraseAudit " +
                "(TargetSchema, TargetTable, TargetID, ACACardNo, " +
                " ActionType, IsErased, RestoreReason, " +
                " UserName, UserIP, CreatedByBranchID, CreatedByUserID) " +
                "VALUES (:schema, :tbl, :tid, :aca, 'RESTORE', 0, :rsn, :uname, :uip, :bid, :uid)";
        try (var con = sql2o.open()) {
            var s = (schema == null || schema.isBlank()) ? "dbo" : schema;
            con.createQuery(sql)
                    .addParameter("schema", s)
                    .addParameter("tbl", table)
                    .addParameter("tid", id)
                    .addParameter("aca", acaCardNo)
                    .addParameter("rsn", restoreReason)
                    .addParameter("uname", userName)
                    .addParameter("uip", userIp)
                    .addParameter("bid", branchId)
                    .addParameter("uid", userId)
                    .executeUpdate();
        }
    }

    // 通用異動表：ACA_EraseAudit（這裡先寫 ERASE）
    public void insertErase(String schema, String table, String id, String acaCardNo,
                            Integer docNum, String eraseReason,
                            String userName, String userIp,
                            String branchId, String userId) {
        String sql = "INSERT INTO dbo.ACA_EraseAudit " +
                "(TargetSchema, TargetTable, TargetID, ACACardNo, " +
                " ActionType, IsErased, DocNum, EraseReason, " +
                " UserName, UserIP, CreatedByBranchID, CreatedByUserID) " +
                "VALUES (:schema, :tbl, :tid, :aca, 'ERASE', 1, :doc, :rsn, :uname, :uip, :bid, :uid)";
        try (var con = sql2o.open()) {
            var schemaToUse = (schema == null || schema.isBlank()) ? "dbo" : schema;
            con.createQuery(sql)
                    .addParameter("schema", schemaToUse)   // e.g. "dbo"
                    .addParameter("tbl", table)       // e.g. "CrmRec"
                    .addParameter("tid", id)
                    .addParameter("aca", acaCardNo)
                    .addParameter("doc", docNum)
                    .addParameter("rsn", eraseReason)
                    .addParameter("uname", userName)
                    .addParameter("uip", userIp)
                    .addParameter("bid", branchId)
                    .addParameter("uid", userId)
                    .executeUpdate();
        }
    }
}

