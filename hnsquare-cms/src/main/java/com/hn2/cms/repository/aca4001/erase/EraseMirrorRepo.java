package com.hn2.cms.repository.aca4001.erase;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class EraseMirrorRepo {
    private final org.sql2o.Sql2o sql2o;

    @Data
    @NoArgsConstructor
    public static class MirrorRow {
        private String targetSchema;
        private String targetTable;
        private String targetId;
        private String acaCardNo;
        private String payloadBase64;
        private String ivBase64;
        private String sha256;
    }

    // 依 ACACardNo 撈出指定表的 mirror（這裡先支援 CrmRec）
    public List<MirrorRow> findAllByAcaCardNo(String acaCardNo, List<String> tables, String schema) {
        String sql = "SELECT " +
                " TargetSchema   AS targetSchema, " +
                " TargetTable    AS targetTable, " +
                " TargetID       AS targetId, " +
                " ACACardNo      AS acaCardNo, " +
                " EncodedPayload AS payloadBase64, " +
                " AesIvBase64    AS ivBase64, " +
                " PayloadSha256Hex AS sha256 " +
                "FROM dbo.ACA_EraseMirror " +
                "WHERE ACACardNo = :aca " +
                "  AND ISNULL(TargetSchema,'dbo') = ISNULL(:schema,'dbo') " +  // ← 這行很關鍵
                "  AND TargetTable IN (:tbls)";
        try (var con = sql2o.open()) {
            var s = (schema == null || schema.isBlank()) ? "dbo" : schema;
            return con.createQuery(sql)
                    .addParameter("aca", acaCardNo)
                    .addParameter("schema", s)
                    .addParameter("tbls", tables)
                    .executeAndFetch(MirrorRow.class);
        }
    }

    // 通用鏡像表：ACA_EraseMirror
    public void upsert(String table, String id, String acaCardNo, String payloadB64, String ivB64, String sha256Hex, String schema) {
        String sql = "MERGE INTO dbo.ACA_EraseMirror AS t " +
                "USING (VALUES(:sch,:tbl,:tid,:aca,:pl,:iv,:sha)) AS s(" +
                "  TargetSchema,TargetTable,TargetID,ACACardNo,EncodedPayload,AesIvBase64,PayloadSha256Hex) " +
                "  ON t.TargetSchema=s.TargetSchema AND t.TargetTable=s.TargetTable AND t.TargetID=s.TargetID " +
                "WHEN MATCHED THEN " +
                "  UPDATE SET EncodedPayload=s.EncodedPayload, AesIvBase64=s.AesIvBase64, PayloadSha256Hex=s.PayloadSha256Hex " +
                "WHEN NOT MATCHED THEN " +
                "  INSERT (TargetSchema,TargetTable,TargetID,ACACardNo,EncodedPayload,AesIvBase64,PayloadSha256Hex) " +
                "  VALUES (s.TargetSchema,s.TargetTable,s.TargetID,s.ACACardNo,s.EncodedPayload,s.AesIvBase64,s.PayloadSha256Hex);";
        ;
        try (var con = sql2o.open()) {
            con.createQuery(sql)
                    .addParameter("sch", schema)          // 可傳 null
                    .addParameter("tbl", table)
                    .addParameter("tid", id)
                    .addParameter("aca", acaCardNo)
                    .addParameter("pl", payloadB64)
                    .addParameter("iv", ivB64)
                    .addParameter("sha", sha256Hex)
                    .executeUpdate();
        }
    }
}
