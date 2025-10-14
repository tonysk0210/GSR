package com.hn2.cms.service.aca4001.erase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hn2.cms.repository.aca4001.erase.EraseAuditRepo;
import com.hn2.cms.repository.aca4001.erase.EraseMirrorRepo;
import com.hn2.cms.service.aca4001.erase.command.EraseCommand;
import com.hn2.cms.service.aca4001.erase.command.RestoreCommand;
import com.hn2.cms.service.aca4001.erase.rules.EraseTableConfigPojo;
import com.hn2.cms.service.aca4001.erase.rules.EraseRestoreExecutor;
import com.hn2.cms.service.aca4001.erase.support.RowUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GenericEraseService {
    private final EraseMirrorRepo mirrorRepo;                       // 鏡像表存取（ACA_EraseMirror）
    private final EraseAuditRepo auditRepo;                         // 塗銷異動表存取（ACA_EraseAudit）
    private final AesGcmCrypto crypto;                              // AES-GCM 加解密與 sha256 計算
    private final ObjectMapper om;                                  // JSON 序列化/反序列化
    private final List<EraseTableConfigPojo> tableConfig;           // 各表的規則宣告
    private final EraseRestoreExecutor executor;                    // 通用執行器

    // 新增在類別裡（任一欄位區塊都可）
    private static final String C_RULE = "\u001B[96m";  // 亮青：Rule
    private static final String C_RST = "\u001B[95m";  // 粉紫：Restore 標籤
    private static final String C_ERS = "\u001B[94m";  // 亮藍：Erase 標籤
    private static final String C_RESET = "\u001B[0m";

    /**
     * 以規則引擎執行：鏡像（寫入 ACA_EraseMirror）→ 清空 → 寫 ERASE 稽核。
     */
    @Transactional
    public void eraseRows(EraseCommand cmd) {
        runRuleErase(cmd); // 依規則：鏡像→清空

        // 成功後寫一筆 ERASE 稽核（整體流程成功才寫）
        auditRepo.insertEraseAction(
                cmd.getAcaCardNo(),
                cmd.getDocNum(),
                cmd.getEraseReason(),
                cmd.getOperatorUserId(),
                cmd.getOperatorIp()
        );
    }

    private void runRuleErase(EraseCommand cmd) {
        for (var rule : tableConfig) {
            // 依規則決定用哪個 key 清單：子表用 parentTable 的 keys，主表用自己的 keys
            List<String> keys = rule.isChild() ? cmd.idsOf(rule.getParentTable()) : cmd.idsOf(rule.getTable());
            if (keys == null || keys.isEmpty()) continue;

            // 撈資料（白名單欄位＋__PK__），子表走父鍵；主表走主鍵
            var rows = rule.isChild() ? executor.loadRowsByParentIds(rule, keys) : executor.loadRowsByIds(rule, keys);
            if (rows.isEmpty()) continue;

            // —— 鏡像：為每一列把「欄位 Map」打包成 JSON → AES-GCM 加密 → SHA → upsert 鏡像表
            for (var row : rows) {
                String id = RowUtils.extractIdOrThrow(row, rule.getIdColumn(), rule.getTable());
                String json = buildRowPayloadJson(rule.getSchema(), rule.getTable(), rule.getIdColumn(), id, row);

                logJson(false, false, cmd.getAcaCardNo(), rule.getTable(), id, json); // [RULE][ERASE]

                var enc = crypto.encryptJson(json);
                String sha = AesGcmCrypto.sha256Hex(json);
                mirrorRepo.upsert(rule.getTable(), id, cmd.getAcaCardNo(), enc.payloadBase64, enc.ivBase64, sha, rule.getSchema());
            }

            // —— 清空（Erase）：子表用父鍵，主表用主鍵
            if (rule.isChild()) executor.eraseByParent(rule, keys);
            else executor.eraseByIds(rule, keys);
        }
    }

    /**
     * 以規則引擎執行：從鏡像解密→校驗→回寫→寫 RESTORE 稽核→刪鏡像。
     */
    @Transactional
    public void restoreAllByAcaCardNo(RestoreCommand cmd) {
        runRuleRestore(cmd); // 依規則：從鏡像解密→校驗→回寫

        // 成功後寫一筆 RESTORE 稽核，並刪除該卡號的鏡像紀錄
        auditRepo.insertRestoreAction(
                cmd.getAcaCardNo(),
                cmd.getRestoreReason(),
                cmd.getOperatorUserId(),
                cmd.getOperatorIp()
        );
        auditRepo.deleteByAcaCardNo(cmd.getAcaCardNo());
    }

    private void runRuleRestore(RestoreCommand cmd) {
        // 只處理 rule 有定義過的表（避免鏡像表裡有舊資料或非本規則表）
        var allowedTables = tableConfig.stream()
                .map(EraseTableConfigPojo::getTable)
                .distinct()
                .collect(Collectors.toList());

        // 撈出該卡號、且目標表在 allowedTables 的所有鏡像資料
        var mirrors = mirrorRepo.findAllByAcaCardNo(cmd.getAcaCardNo(), allowedTables, "dbo");
        if (mirrors.isEmpty()) return;

        // 依目標表分組 → 依規則順序逐表回寫
        var byTable = mirrors.stream()
                .collect(Collectors.groupingBy(EraseMirrorRepo.MirrorRow::getTargetTable));

        for (var rule : tableConfig) {
            var list = byTable.get(rule.getTable());
            if (list == null || list.isEmpty()) continue;

            var rows = new ArrayList<Map<String, Object>>();
            for (var m : list) {
                // 解密 +（可選）SHA 驗證
                String json = decryptPayloadJson(m, true); // 解密＋SHA 校驗
                logJson(false, true, m.getAcaCardNo(), m.getTargetTable(), m.getTargetId(), json); // [RULE][RESTORE]
                rows.add(parsePayloadToMap(json)); // 還原用的 Map（含 __PK__）
            }

            // 只覆蓋白名單欄位，並套用 restoreExtraSet（如 isERASE=0, ModifiedByUserID=:uid）
            executor.restoreRows(rule, rows, cmd.getOperatorUserId());
        }
    }

    // ====== Helpers ======

    /**
     * 將單列資料打包成鏡像 JSON（含 schema/table/idColumn/id/fields）。
     */
    private String buildRowPayloadJson(String schema, String table, String idCol, String id, Map<String, Object> fields) {
        var node = om.createObjectNode();
        node.put("schema", schema);
        node.put("table", table);
        node.put("idColumn", idCol);
        node.put("id", id);
        node.set("fields", om.valueToTree(fields));
        try {
            return om.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * 解密鏡像 payload，必要時進行 SHA-256 驗證。
     */
    private String decryptPayloadJson(EraseMirrorRepo.MirrorRow m, boolean verifySha) {
        final String json = crypto.decryptToJson(m.getPayloadBase64(), m.getIvBase64());
        if (verifySha) {
            final String sha = AesGcmCrypto.sha256Hex(json);
            if (!sha.equalsIgnoreCase(m.getSha256())) {
                throw new IllegalStateException("Mirror payload 校驗失敗: " + m.getTargetId());
            }
        }
        return json;
    }

    /**
     * 將鏡像 JSON 解析回可回寫用的欄位 Map（含 __PK__）。
     */
    private Map<String, Object> parsePayloadToMap(String json) {
        final var root = readJson(json);

        String id = getTextCI(root, "id", "_ID", "ID");
        var fieldsNode = getObjCI(root, "fields", "FIELDS");
        if (fieldsNode == null) {
            throw new IllegalStateException("鏡像 payload 缺少 fields/FIELDS 物件，id=" + id + ", raw=" + json);
        }

        String schema = getTextCI(root, "schema", "SCHEMA");
        String table = getTextCI(root, "table", "TABLE");
        String idCol = getTextCI(root, "idColumn", "IDCOL", "IDCOLUMN");

        var map = new java.util.LinkedHashMap<String, Object>();
        if (schema != null) map.put("schema", schema);
        if (table != null) map.put("table", table);
        if (idCol != null) map.put("idColumn", idCol);
        if (id != null) map.put("id", id);

        var it = fieldsNode.fields();
        while (it.hasNext()) {
            var e = it.next();
            var v = e.getValue();
            Object val = v.isNull() ? null
                    : v.isNumber() ? v.numberValue()
                    : v.isBoolean() ? v.booleanValue()
                    : v.asText();
            map.put(e.getKey(), val);
        }

        if (id != null) map.put("__PK__", id);
        return map;
    }

    /**
     * 彩色 JSON log（Rule/Erase vs Rule/Restore）。
     */
    private void logJson(boolean isLegacyShouldAlwaysFalse, boolean isRestore, String aca, String table, String id, String json) {
        // 只保留 RULE；參數 isLegacyShouldAlwaysFalse 僅為了沿用既有呼叫點簽名（可視需要移除並改呼叫點）
        if (!log.isInfoEnabled()) return;
        String phase = isRestore ? "RESTORE" : "ERASE";
        String colorPhase = isRestore ? C_RST : C_ERS;
        String preview = (json != null && json.length() > 2000) ? json.substring(0, 2000) + "...(truncated)" : json;
        log.info("{}[{}]{}{}[{}]{} aca={}, tbl={}, id={}, json={}",
                C_RULE, "RULE", C_RESET,
                colorPhase, phase, C_RESET,
                aca, table, id, preview);
    }

    // JSON 容錯工具：readJson / getTextCI / getObjCI
    // ---- JSON 容錯讀取與大小寫容忍 ----
    private com.fasterxml.jackson.databind.JsonNode readJson(String json) {
        try {
            return om.readTree(json);
        } catch (Exception e) {
            throw new IllegalStateException("鏡像 payload 不是合法 JSON", e);
        }
    }

    // 讀文字欄位：支援多候選鍵 + 大小寫
    private String getTextCI(com.fasterxml.jackson.databind.JsonNode n, String... keys) {
        for (String k : keys) {
            var child = n.get(k);
            if (child == null) {
                var up = n.get(k.toUpperCase());
                var lo = n.get(k.toLowerCase());
                child = (up != null) ? up : lo;
            }
            if (child != null && !child.isNull()) return child.asText();
        }
        return null;
    }

    // 讀物件欄位：支援多候選鍵 + 大小寫
    private com.fasterxml.jackson.databind.node.ObjectNode getObjCI(com.fasterxml.jackson.databind.JsonNode n, String... keys) {
        for (String k : keys) {
            var child = n.get(k);
            if (child == null) {
                var up = n.get(k.toUpperCase());
                var lo = n.get(k.toLowerCase());
                child = (up != null) ? up : lo;
            }
            if (child != null && child.isObject()) return (com.fasterxml.jackson.databind.node.ObjectNode) child;
        }
        return null;
    }
}
