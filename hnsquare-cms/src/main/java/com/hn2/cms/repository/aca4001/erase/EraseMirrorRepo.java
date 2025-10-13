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
        private String targetTable;  // 來源資料表名稱（如 CrmRec）
        private String targetId;  // 被塗銷紀錄的主鍵 ID
        private String acaCardNo; // 個案卡號
        private String payloadBase64; // 加密後的 JSON 資料（Base64）
        private String ivBase64; // AES-GCM 的 IV（Base64）
        private String sha256;   // 明文 JSON 的 SHA-256 值（用來驗證完整性）
    }

    /**
     * 依個案卡號（ACACardNo）查出 ACA_EraseMirror 中屬於「指定 schema、指定表清單」的所有鏡像列。
     * 查詢條件：
     * - ACACardNo = :aca
     * - ISNULL(TargetSchema,'dbo') = ISNULL(:schema,'dbo')  // Mirror 中若為 NULL 視同 'dbo'，呼叫方 schema 為空也視同 'dbo'
     * - TargetTable IN (:tbls)
     * 回傳：
     * - 將每列映射為 MirrorRow（包含 targetSchema/table/id、acaCardNo、payloadBase64、ivBase64、sha256）
     * - 若無資料回傳空清單
     *
     * @param acaCardNo 指定個案卡號
     * @param tables    僅限於這些目標表名（白名單）
     * @param schema    目標 schema；可為 null/空白，將被視為 "dbo"
     * @return 鏡像資料列的清單（List<MirrorRow>）
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
                // schema 允許 null，Mirror 表紀錄為 NULL 的也視為 "dbo"
                "  AND ISNULL(TargetSchema,'dbo') = ISNULL(:schema,'dbo') " +   // ← 核心：雙方都把 NULL 視為 'dbo'
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
     * 對 ACA_EraseMirror 進行「UPSERT（有則更新、無則新增）」：
     * 以 (TargetSchema, TargetTable, TargetID) 作為唯一定義鍵，
     * 將加密後的鏡像內容（EncodedPayload + AesIvBase64）與明文校驗雜湊（PayloadSha256Hex）寫入。
     * 規則：
     * - 若已存在相同 (schema, table, id) → UPDATE EncodedPayload / AesIvBase64 / PayloadSha256Hex / ACACardNo
     * - 若不存在 → INSERT 一筆並填入 CreatedOnDate = SYSDATETIME()
     * - 允許傳入 schema 為 null/空白，會自動視為 "dbo"
     * - id 不可為 null/空白/字串 "null"（大小寫不敏感），並在綁定前會 trim()
     * 安全性 / 完整性：
     * - SHA-256 為明文 JSON 的雜湊，用於「還原前」校驗解密結果未遭竄改
     * - AES-GCM 密文與 IV 一併保存
     *
     * @param table      來源表名（如 "CrmRec" / "ProRec"）
     * @param id         主鍵值（字串化）
     * @param acaCardNo  個案卡號
     * @param payloadB64 Base64( AES-GCM(明文JSON) )
     * @param ivB64      Base64( AES-GCM IV )
     * @param sha256Hex  明文 JSON 的 SHA-256（十六進位字串）
     * @param schema     來源 schema；null/空白 → 視為 "dbo"
     * @throws IllegalArgumentException 當 id 無效（null/空白/"null"）
     */
    public void upsert(String table, String id, String acaCardNo, String payloadB64, String ivB64, String sha256Hex, String schema) {

        // 基本防呆：TargetID 不可為空、也不可是字面 "null"
        if (id == null || id.isBlank() || "null".equalsIgnoreCase(id)) {
            throw new IllegalArgumentException("EraseMirror.upsert: TargetID 不可為空/不可為 'null' 字串, table=" + table + ", aca=" + acaCardNo);
        }
        // 記錄關鍵參數（便於除錯追蹤）
        log.info("[MirrorUpsert] sch={}, tbl={}, id={}, aca={}, sha256={}",
                schema, table, id, acaCardNo, sha256Hex);

        // 使用 SQL Server MERGE 做 UPSERT：
        //   - USING 一筆臨時資料列 s(...)
        //   - ON 條件：以 (TargetSchema,TargetTable,TargetID) 對應到目標表 t
        //   - MATCHED → UPDATE（更新密文/IV/SHA 與 ACACardNo）
        //   - NOT MATCHED → INSERT 新列並寫入 CreatedOnDate=SYSDATETIME()

        String sql = "MERGE INTO dbo.ACA_EraseMirror AS t "
                + "USING (VALUES(:sch,:tbl,:tid,:aca,:pl,:iv,:sha)) AS s("
                + "  TargetSchema,TargetTable,TargetID,ACACardNo,EncodedPayload,AesIvBase64,PayloadSha256Hex) "
                + "  ON t.TargetSchema=s.TargetSchema AND t.TargetTable=s.TargetTable AND t.TargetID=s.TargetID "
                + "WHEN MATCHED THEN "
                + "  UPDATE SET EncodedPayload=s.EncodedPayload, AesIvBase64=s.AesIvBase64, "
                + "             PayloadSha256Hex=s.PayloadSha256Hex, ACACardNo=s.ACACardNo "
                + "WHEN NOT MATCHED THEN "
                + "  INSERT (TargetSchema,TargetTable,TargetID,ACACardNo,EncodedPayload,AesIvBase64,PayloadSha256Hex,CreatedOnDate) "
                + "  VALUES (s.TargetSchema,s.TargetTable,s.TargetID,s.ACACardNo,s.EncodedPayload,s.AesIvBase64,s.PayloadSha256Hex,SYSDATETIME());";
        try (var con = sql2o.open()) {
            con.createQuery(sql)
                    // schema 若為 null/空白 → 視為 "dbo"
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
