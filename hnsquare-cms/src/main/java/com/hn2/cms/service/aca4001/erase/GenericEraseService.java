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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    /**
     * 依規則執行鏡像與實體塗銷，確保流程集中管理。
     */
    private void runRuleErase(EraseCommand cmd) {
        logLoadedTables();
        if (cmd == null || cmd.isEmpty()) {
            return;
        }

        for (var rule : tableConfig) {
            List<String> targetKeys = collectKeysForRule(cmd, rule);
            if (targetKeys.isEmpty()) {
                continue;
            }

            List<Map<String, Object>> rows = loadRowsForRule(rule, targetKeys);
            if (rows.isEmpty()) {
                continue;
            }

            persistMirrorRows(cmd, rule, rows);
            eraseSourceRows(rule, targetKeys);
        }
    }

    /**
     * 依照塗銷規則自鏡像資料復原原始紀錄，並記錄還原稽核。
     */
    @Transactional
    public void restoreAllByAcaCardNo(RestoreCommand cmd) {
        runRuleRestore(cmd);

        auditRepo.insertRestoreAction(
                cmd.getAcaCardNo(),
                cmd.getRestoreReason(),
                cmd.getOperatorUserId(),
                cmd.getOperatorIp());
        auditRepo.deleteByAcaCardNo(cmd.getAcaCardNo());
    }

    /**
     * 依規則與鏡像資料逐表復原，同時保留原有稽核流程。
     */
    private void runRuleRestore(RestoreCommand cmd) {
        var mirrorsByTable = groupMirrorsByTable(cmd);
        if (mirrorsByTable.isEmpty()) {
            return;
        }

        for (var rule : tableConfig) {
            var mirrorRows = mirrorsByTable.get(rule.getTable());
            if (mirrorRows == null || mirrorRows.isEmpty()) {
                continue;
            }

            List<Map<String, Object>> rows = mirrorRows.stream()
                    .map(this::toRestorableRow)
                    .collect(Collectors.toList());

            executor.restoreRows(rule, rows, cmd.getOperatorUserId());
        }
    }

    /**
     * 將目前載入的規則表名寫入 log，方便追蹤此次塗銷涵蓋範圍。
     */
    private void logLoadedTables() {
        if (!log.isInfoEnabled()) {
            return;
        }
        log.info("Loaded rules: {}", tableConfig.stream()
                .map(EraseTableConfigPojo::getTable)
                .collect(Collectors.toList()));
    }

    /**
     * 根據規則取得對應的主鍵（或 parent 主鍵），同時濾除空白值。
     */
    private List<String> collectKeysForRule(EraseCommand cmd, EraseTableConfigPojo rule) {
        String table = rule.isChild() ? rule.getParentTable() : rule.getTable();
        if (table == null) {
            return Collections.emptyList();
        }
        return sanitizeIds(cmd.idsOf(table));
    }

    /**
     * 依規則屬性決定採用 ID 或 parent ID 執行查詢。
     */
    private List<Map<String, Object>> loadRowsForRule(EraseTableConfigPojo rule, List<String> keys) {
        return rule.isChild()
                ? executor.loadRowsByParentIds(rule, keys)
                : executor.loadRowsByIds(rule, keys);
    }

    /**
     * 將查到的資料逐筆寫入鏡像表，保留加密後 payload 與校驗碼。
     */
    private void persistMirrorRows(EraseCommand cmd, EraseTableConfigPojo rule, List<Map<String, Object>> rows) {
        for (var row : rows) {
            persistMirrorRow(cmd, rule, row);
        }
    }

    /**
     * 建立鏡像紀錄並記錄 JSON 內容，提供後續還原使用。
     */
    private void persistMirrorRow(EraseCommand cmd, EraseTableConfigPojo rule, Map<String, Object> row) {
        String id = RowUtils.extractIdOrThrow(row, rule.getIdColumn(), rule.getTable());
        String json = buildRowPayloadJson(rule.getSchema(), rule.getTable(), rule.getIdColumn(), id, row);

        logJson(false, false, cmd.getAcaCardNo(), rule.getTable(), id, json);

        var enc = crypto.encryptJson(json);
        String sha = AesGcmCrypto.sha256Hex(json);
        mirrorRepo.upsert(rule.getTable(), id, cmd.getAcaCardNo(), enc.payloadBase64, enc.ivBase64, sha, rule.getSchema());
    }

    /**
     * 驅動實際刪除：child 規則以 parent key 為準、一般規則直接用主鍵。
     */
    private void eraseSourceRows(EraseTableConfigPojo rule, List<String> keys) {
        if (rule.isChild()) {
            executor.eraseByParent(rule, keys);
        } else {
            executor.eraseByIds(rule, keys);
        }
    }

    /**
     * 以 acaCardNo 取出鏡像資料並依 table 分組，僅保留規則涵蓋的表。
     */
    private Map<String, List<EraseMirrorRepo.MirrorRow>> groupMirrorsByTable(RestoreCommand cmd) {
        var allowedTables = tableConfig.stream()
                .map(EraseTableConfigPojo::getTable)
                .distinct()
                .collect(Collectors.toList());
        if (allowedTables.isEmpty()) {
            return Collections.emptyMap();
        }

        var mirrors = mirrorRepo.findAllByAcaCardNo(cmd.getAcaCardNo(), allowedTables, "dbo");
        if (mirrors.isEmpty()) {
            return Collections.emptyMap();
        }
        return mirrors.stream().collect(Collectors.groupingBy(EraseMirrorRepo.MirrorRow::getTargetTable));
    }

    /**
     * 將鏡像紀錄解密為 Map，同步寫 log 方便稽核。
     */
    private Map<String, Object> toRestorableRow(EraseMirrorRepo.MirrorRow mirror) {
        String json = decryptPayloadJson(mirror, true);
        logJson(false, true, mirror.getAcaCardNo(), mirror.getTargetTable(), mirror.getTargetId(), json);
        return parsePayloadToMap(json);
    }

    /**
     * 去除空白與重複項，避免誤觸空鍵或重複刪除。
     */
    private List<String> sanitizeIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return ids.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());
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
