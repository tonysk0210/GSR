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
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrmRecEraseService {

    private static final String TABLE = "CrmRec";
    private static final String SCHEMA = "dbo"; // 或 null

    private final CrmRecRepo crmRepo;
    private final EraseMirrorRepo mirrorRepo;
    private final EraseAuditRepo auditRepo;
    private final AesGcmCrypto crypto;          // AES-GCM 工具
    private final ObjectMapper om;

    // ---- 型別正規化白名單（依你實際欄位再調整/擴充）----
    private static final Set<String> DATE_COLS = Set.of(
            "CrmChaDate", "CrmDisDate", "Crm_ReleaseDate", "Crm_VerdictDate"
    );
    private static final Set<String> INT_COLS = Set.of(
            "CreatedByUserID", "ModifiedByUserID", "Crm_Sentence", "CrmTerm"
    );

    @Transactional
    public void restoreByAcaCardNo(String acaCardNo, String restoreReason,
                                   String operatorUserId, String operatorUserName,
                                   String operatorIp, String operatorBranchId) {

        var mirrors = mirrorRepo.findAllByAcaCardNo(acaCardNo, List.of(TABLE), SCHEMA);
        log.info("mirror count for ACA={} -> {}", acaCardNo, mirrors.size());
        if (mirrors.isEmpty()) return;

        for (var m : mirrors) {
            final String id = m.getTargetId();

            // 僅在已塗銷時才復原（冪等）
            if (!crmRepo.isErased(id)) {
                log.info("skip restore: id={} isERASE=0", id);
                continue;
            }

            // 解密 + 校驗
            String json = crypto.decryptToJson(m.getPayloadBase64(), m.getIvBase64()); // 先密文、後 IV
            String sha = AesGcmCrypto.sha256Hex(json);
            if (!sha.equalsIgnoreCase(m.getSha256())) {
                throw new IllegalStateException("Mirror payload 校驗失敗(SHA-256 mismatch): " + id);
            }
            log.info("restore target={}, payload sha={}, json snippet={}",
                    id, sha, json.substring(0, Math.min(200, json.length())));

            // 解析 fields → Map<String,Object>
            ObjectNode node;
            try {
                node = (ObjectNode) om.readTree(json);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            if (!TABLE.equals(node.path("table").asText()) || !id.equals(node.path("id").asText())) {
                throw new IllegalStateException("Mirror內容與目標不符: " + id);
            }

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
                // 還原時做型別正規化（日期/整數）
                val = normalizeValue(e.getKey(), val);
                map.put(e.getKey(), val);
            });

            int nonNullCount = (int) map.values().stream().filter(v -> v != null).count();
            log.info("restore {} -> totalCols={}, nonNullCols={}, cols={}", id, map.size(), nonNullCount, map.keySet());
            if (map.isEmpty()) {
                throw new IllegalStateException("Mirror fields 為空，無可還原欄位: " + id);
            }

            // 寫回欄位 + isERASE=0（方法內會排除系統欄位）
            int rows = crmRepo.restoreFieldsAndUnmarkErased(id, map, operatorUserId);
            log.info("restore update rows for {} -> {}", id, rows);
            if (rows != 1) {
                throw new IllegalStateException("復原失敗或狀態已變更 (rows=" + rows + "): " + id);
            }

            // 寫 Audit（RESTORE）— 無 docNum
            auditRepo.insertRestore(SCHEMA, TABLE, id, acaCardNo,
                    restoreReason, operatorUserName, operatorIp, operatorBranchId, operatorUserId);
        }
    }

    @Transactional
    public void eraseCrm(Aca4001ErasePayload payload,
                         String operatorUserId,
                         String operatorUserName,
                         String operatorIp,
                         String operatorBranchId) {

        var ids = Optional.ofNullable(payload.getSelectedCrmRecIds()).orElse(List.of());
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

            // 2.2 組明文 JSON（只含白名單欄位）
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

            // 2.3 記憶體中 AES-GCM 加密
            var enc = crypto.encryptJson(json);
            String sha256 = sha256Hex(json); // 或用 AesGcmCrypto.sha256Hex(json)

            // 2.4 寫 Mirror（通用）
            mirrorRepo.upsert(TABLE, id, payload.getAcaCardNo(),
                    enc.payloadBase64, enc.ivBase64, sha256, SCHEMA);

            // 2.5 寫 Audit（ERASE）
            auditRepo.insertErase(SCHEMA, TABLE, id, payload.getAcaCardNo(),
                    payload.getDocNum(), payload.getEraseReason(),
                    operatorUserName, operatorIp,
                    operatorBranchId, operatorUserId);

            // 2.6 清空原欄位 + isERASE=1
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

    /**
     * 還原時將 JSON 的字串/數值轉回較接近 DB 欄位預期的型別（最常見：日期、整數）
     */
    private static Object normalizeValue(String col, Object val) {
        if (val == null) return null;

        if (DATE_COLS.contains(col)) {
            if (val instanceof String) {
                String s = ((String) val).trim();
                if (s.isEmpty()) return null;
                s = s.replace('T', ' ');
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
        }

        if (INT_COLS.contains(col)) {
            if (val instanceof Number) {
                return ((Number) val).intValue();
            }
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
