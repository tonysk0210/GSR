package com.hn2.cms.repository.aca4001.erase;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class EraseMirrorRepo {
    private final org.sql2o.Sql2o sql2o;

    /**
     * MirrorRow 是對應 ACA_EraseMirror 表的一行紀錄
     * 用來承接資料庫查詢結果（欄位對應成 Java 屬性）
     */
    @Data
    @NoArgsConstructor
    public static class MirrorRow {
        private String targetSchema; // 來源 Schema（通常是 dbo）
        private String targetTable; // 來源資料表名稱（如 CrmRec）
        private String targetId; // 被塗銷紀錄的主鍵 ID
        private String acaCardNo; // 個案卡號
        private String payloadBase64; // 加密後的 JSON 資料（Base64）
        private String ivBase64; // AES-GCM 的 IV（Base64）
        private String sha256; // 明文 JSON 的 SHA-256 值（用來驗證完整性）
    }

    /**
     * 依 ACACardNo + schema + table 清單，查詢 ACA_EraseMirror 的所有紀錄
     * - 這樣可以一次撈出某個個案底下，特定表（如 CrmRec）的所有鏡像資料
     * - schema 預設會補成 "dbo"
     */
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
                // schema 允許 null，若 Mirror 表紀錄是 null，就當成 "dbo"
                "  AND ISNULL(TargetSchema,'dbo') = ISNULL(:schema,'dbo') " +  // ← 這行很關鍵
                "  AND TargetTable IN (:tbls)";
        try (var con = sql2o.open()) {
            // 如果 schema 是空字串或 null，則補上 "dbo"
            var s = (schema == null || schema.isBlank()) ? "dbo" : schema;
            return con.createQuery(sql)
                    .addParameter("aca", acaCardNo)
                    .addParameter("schema", s)
                    .addParameter("tbls", tables)
                    .executeAndFetch(MirrorRow.class);
        }
    }

    /**
     * 通用 upsert 方法：將資料寫入 ACA_EraseMirror
     * - 如果 (TargetSchema, TargetTable, TargetID) 已存在 → 更新 EncodedPayload / AesIvBase64 / PayloadSha256Hex
     * - 如果不存在 → 插入新的一筆
     *
     * @param table      來源資料表名稱（例如 CrmRec）
     * @param id         被塗銷紀錄的主鍵 ID
     * @param acaCardNo  個案卡號
     * @param payloadB64 加密後 JSON 資料（Base64）
     * @param ivB64      AES-GCM IV（Base64）
     * @param sha256Hex  明文 JSON 的 SHA-256
     * @param schema     Schema 名稱（允許 null）
     */
    /*public void upsert(String table, String id, String acaCardNo, String payloadB64, String ivB64, String sha256Hex, String schema) {
        // SQL Server MERGE 語法
        // - s(...) 是臨時資料來源 (VALUES)
        // - ON 比對條件：TargetSchema + TargetTable + TargetID
        // - MATCHED → UPDATE
        // - NOT MATCHED → INSERT
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
                    .addParameter("sch", schema)  // 允許 null，資料庫會存成 NULL
                    .addParameter("tbl", table)
                    .addParameter("tid", id)
                    .addParameter("aca", acaCardNo)
                    .addParameter("pl", payloadB64)
                    .addParameter("iv", ivB64)
                    .addParameter("sha", sha256Hex)
                    .executeUpdate();
        }
    }*/
    public void upsert(String table, String id, String acaCardNo,
                       String payloadB64, String ivB64, String sha256Hex, String schema) {

        if (id == null || id.isBlank() || "null".equalsIgnoreCase(id)) {
            throw new IllegalArgumentException("EraseMirror.upsert: TargetID 不可為空/不可為 'null' 字串, table=" + table + ", aca=" + acaCardNo);
        }
        // 可加：log 參數
        log.info("[MirrorUpsert] sch={}, tbl={}, id={}, aca={}, sha256={}",
                schema, table, id, acaCardNo, sha256Hex);

        String sql = "MERGE INTO dbo.ACA_EraseMirror AS t "
                + "USING (VALUES(:sch,:tbl,:tid,:aca,:pl,:iv,:sha)) AS s("
                + "  TargetSchema,TargetTable,TargetID,ACACardNo,EncodedPayload,AesIvBase64,PayloadSha256Hex) "
                + "  ON t.TargetSchema=s.TargetSchema AND t.TargetTable=s.TargetTable AND t.TargetID=s.TargetID "
                + "WHEN MATCHED THEN "
                + "  UPDATE SET EncodedPayload=s.EncodedPayload, AesIvBase64=s.AesIvBase64, "
                + "             PayloadSha256Hex=s.PayloadSha256Hex, ACACardNo=s.ACACardNo " // ★ 更新 ACACardNo
                + "WHEN NOT MATCHED THEN "
                + "  INSERT (TargetSchema,TargetTable,TargetID,ACACardNo,EncodedPayload,AesIvBase64,PayloadSha256Hex) "
                + "  VALUES (s.TargetSchema,s.TargetTable,s.TargetID,s.ACACardNo,s.EncodedPayload,s.AesIvBase64,s.PayloadSha256Hex);";
        try (var con = sql2o.open()) {
            con.createQuery(sql)
                    .addParameter("sch", (schema == null || schema.isBlank()) ? "dbo" : schema)
                    .addParameter("tbl", table)
                    .addParameter("tid", id.trim())        // ★ 保險：trim
                    .addParameter("aca", acaCardNo)
                    .addParameter("pl", payloadB64)
                    .addParameter("iv", ivB64)
                    .addParameter("sha", sha256Hex)
                    .executeUpdate();
        }
    }

}
