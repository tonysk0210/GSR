package com.hn2.cms.service.aca4001.erase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hn2.cms.payload.aca4001.Aca4001ErasePayload;
import com.hn2.cms.repository.aca4001.erase.CrmRecRepo;
import com.hn2.cms.repository.aca4001.erase.EraseAuditRepo;
import com.hn2.cms.repository.aca4001.erase.EraseMirrorRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrmRecEraseService {

    private static final String TABLE = "CrmRec"; // 目前服務操作的目標資料表
    private static final String SCHEMA = "dbo";   // Schema（可視環境改為 null 表示預設）

    // 依賴的 Repository / 工具們
    private final CrmRecRepo crmRepo; // 讀寫 CrmRec（原表）
    private final EraseMirrorRepo mirrorRepo; // 讀寫 ACA_EraseMirror（鏡像表）
    private final EraseAuditRepo auditRepo; // 讀寫 ACA_EraseAudit（稽核表）
    private final AesGcmCrypto crypto;      // AES-GCM 加解密工具
    private final ObjectMapper om;          // JSON 解析/序列化

    // 改成「已規範化」的白名單（全部大寫；去掉非英數與底線）
    private static final Set<String> DATE_COLS_NORM = Set.of(
            "CRMCHADATE", "CRMDISDATE", "CRMRELEASEDATE", "CRMVERDICTDATE"
    );
    private static final Set<String> INT_COLS_NORM = Set.of(
            "CREATEDBYUSERID", "MODIFIEDBYUSERID", "CRMSENTENCE", "CRMTERM"
    );


    /**
     * 依 ACACardNo 還原該個案下所有（指定表）鏡像資料：
     * - 從 Mirror 抓加密 payload → 解密 → 校驗 SHA-256 → 還原欄位 → 取消 isERASE
     * - 寫入 Restore 稽核紀錄
     */
    @Transactional
    public void restoreByAcaCardNo(
            String acaCardNo,
            String restoreReason,
            String operatorUserId,
            String operatorIp
    ) {
        var mirrors = mirrorRepo.findAllByAcaCardNo(acaCardNo, List.of(TABLE), SCHEMA);
        log.info("mirror count for ACA={} -> {}", acaCardNo, mirrors.size());
        if (mirrors.isEmpty()) return; // 冪等

        for (var m : mirrors) {
            final String id = m.getTargetId();

            if (!crmRepo.isErased(id)) {
                log.info("skip restore: id={} isERASE=0", id);
                continue;
            }

            // 1) 解密 + 校驗
            String json = crypto.decryptToJson(m.getPayloadBase64(), m.getIvBase64());
            String sha = AesGcmCrypto.sha256Hex(json);
            if (!sha.equalsIgnoreCase(m.getSha256())) {
                throw new IllegalStateException("Mirror payload 校驗失敗(SHA-256 mismatch): " + id);
            }
            log.info("restore target={}, payload sha={}, json snippet={}",
                    id, sha, json.substring(0, Math.min(200, json.length())));

            // 2) 解析 JSON 並一致性檢查
            ObjectNode node;
            try {
                node = (ObjectNode) om.readTree(json);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            if (!TABLE.equals(node.path("table").asText()) || !id.equals(node.path("id").asText())) {
                throw new IllegalStateException("Mirror內容與目標不符: " + id);
            }

            // 3) fields 轉 Map 並型別正規化
            var fieldsNodeRaw = node.get("fields");
            if (fieldsNodeRaw == null || !fieldsNodeRaw.isObject()) {
                throw new IllegalStateException("Mirror payload 缺少 fields 物件或型別不正確");
            }
            ObjectNode fieldsNode = (ObjectNode) fieldsNodeRaw;

            var map = new java.util.LinkedHashMap<String, Object>();
            fieldsNode.fields().forEachRemaining(e -> {
                var v = e.getValue();
                Object val = v.isNull() ? null :
                        v.isNumber() ? v.numberValue() :
                                v.isBoolean() ? v.booleanValue() :
                                        v.asText();
                val = normalizeValue(e.getKey(), val);
                map.put(e.getKey(), val);
            });

            int nonNullCount = (int) map.values().stream().filter(v -> v != null).count();
            log.info("restore {} -> totalCols={}, nonNullCols={}, cols={}", id, map.size(), nonNullCount, map.keySet());
            if (map.isEmpty()) {
                throw new IllegalStateException("Mirror fields 為空，無可還原欄位: " + id);
            }

            // 4) 回寫欄位 + isERASE=0（內部已固定覆寫 ModifiedByUserID=operatorUserId）
            int rows = crmRepo.restoreFieldsAndUnmarkErased(id, map, operatorUserId);
            log.info("restore update rows for {} -> {}", id, rows);
            if (rows != 1) {
                throw new IllegalStateException("復原失敗或狀態已變更 (rows=" + rows + "): " + id);
            }

            // 5) 寫 Restore 稽核（不再帶 UserName/CreatedByBranchID）
            auditRepo.insertRestore(
                    SCHEMA, TABLE, id, acaCardNo,
                    restoreReason,
                    operatorUserId,   // CreatedByUserID
                    operatorIp        // UserIP
            );
        }
    }


    // 標記為事務方法，確保 eraseCrm 的所有 DB 操作在同一個 transaction 內
    @Transactional
    public void eraseCrm(Aca4001ErasePayload payload, String operatorUserId, String operatorIp) {
        var ids = java.util.Optional.ofNullable(payload.getSelectedCrmRecIds()).orElse(List.of());
        if (ids.isEmpty()) return;

        // 1) 驗證歸屬
        var wrong = crmRepo.findNotBelong(payload.getAcaCardNo(), ids);
        if (!wrong.isEmpty()) {
            throw new IllegalArgumentException("CrmRec 非本案：" + wrong);
        }

        // 2) 逐筆處理
        for (String id : ids) {
            if (crmRepo.isErased(id)) continue; // 冪等

            // 2.1 讀白名單欄位
            var fields = crmRepo.loadSensitiveFields(id);
            if (fields == null) continue;

            // 2.2 組 JSON
            ObjectNode node = om.createObjectNode();
            node.put("table", TABLE);
            node.put("id", id);
            node.set("fields", om.valueToTree(fields));
            String json;
            try {
                json = om.writeValueAsString(node);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException(e);
            }

            // 2.3 加密 + SHA-256
            var enc = crypto.encryptJson(json);
            String sha256 = sha256Hex(json);

            // 2.4 寫 Mirror
            mirrorRepo.upsert(
                    TABLE, id, payload.getAcaCardNo(),
                    enc.payloadBase64, enc.ivBase64, sha256, SCHEMA
            );

            // 2.5 寫 Audit（不再帶 UserName/CreatedByBranchID）
            auditRepo.insertErase(
                    SCHEMA, TABLE, id, payload.getAcaCardNo(),
                    payload.getDocNum(), payload.getEraseReason(),
                    operatorUserId,     // CreatedByUserID
                    operatorIp          // UserIP
            );

            // 2.6 清空欄位 + 標記 isERASE=1（你的實作中會把 CreatedByUserID/ModifiedByUserID 設為 -2）
            crmRepo.nullifyAndMarkErased(id);
        }
    }


    // ---- Helpers ------------------------------------------------------------

    private static String sha256Hex(String s) {
        try {
            var md = MessageDigest.getInstance("SHA-256");
            byte[] h = md.digest(s.getBytes(StandardCharsets.UTF_8));
            var sb = new StringBuilder(64);
            for (byte b : h) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    // 將欄位名規範化：移除非 [A-Za-z0-9_] 後，再把底線去掉，整個大寫
    private static String normKey(String col) {
        if (col == null) return "";
        String s = col.replaceAll("[^A-Za-z0-9_]", ""); // 先移除雜字元
        s = s.replace("_", "");                         // 再拿掉底線
        return s.toUpperCase(java.util.Locale.ROOT);
    }

    /**
     * 還原時將 JSON 的字串/數值轉回較接近 DB 欄位預期的型別（最常見：日期、整數）
     */
    private static Object normalizeValue(String col, Object val) {
        if (val == null) return null;
        String key = normKey(col);

        // 日期欄位：支援 ISO8601（含 T、含時區偏移），也支援 'yyyy-MM-dd' / 'yyyy-MM-dd HH:mm:ss'
        if (DATE_COLS_NORM.contains(key)) {
            if (val instanceof java.sql.Timestamp) return val; // 已是 Timestamp
            if (val instanceof java.util.Date) return new java.sql.Timestamp(((java.util.Date) val).getTime());
            String s = val.toString().trim();
            if (s.isEmpty()) return null;

            // 先嘗試 ISO 8601（含時區）的解析：2000-05-14T00:00:00.000+08:00
            try {
                java.time.OffsetDateTime odt = java.time.OffsetDateTime.parse(s);
                return java.sql.Timestamp.from(odt.toInstant());
            } catch (Exception ignore) {
            }

            // 再嘗試 ZonedDateTime
            try {
                java.time.ZonedDateTime zdt = java.time.ZonedDateTime.parse(s);
                return java.sql.Timestamp.from(zdt.toInstant());
            } catch (Exception ignore) {
            }

            // 最後走舊規則：去掉 T/Z/小數秒/時區
            s = s.replace('T', ' ');
            int plus = Math.max(s.indexOf('+'), s.indexOf('-')); // 找到 +08:00 或 -05:00
            if (plus > 10) s = s.substring(0, plus);
            int z = s.indexOf('Z');
            if (z > 0) s = s.substring(0, z);
            int dot = s.indexOf('.');
            if (dot > 0) s = s.substring(0, dot);
            if (s.length() == 10) s += " 00:00:00";
            String ts = s.substring(0, Math.min(19, s.length()));
            try {
                return java.sql.Timestamp.valueOf(ts);
            } catch (IllegalArgumentException ex) {
                log.warn("normalizeValue: 日期欄位 {} 值 [{}] 解析失敗，交由 JDBC 嘗試原字串", col, val);
                return val.toString();
            }
        }

        // 整數欄位
        if (INT_COLS_NORM.contains(key)) {
            if (val instanceof Number) return ((Number) val).intValue();
            try {
                return Integer.parseInt(val.toString());
            } catch (Exception ex) {
                log.warn("normalizeValue: 整數欄位 {} 值 [{}] 解析失敗，交由 JDBC 嘗試字串綁定", col, val);
                return val.toString();
            }
        }

        return val;
    }
}
