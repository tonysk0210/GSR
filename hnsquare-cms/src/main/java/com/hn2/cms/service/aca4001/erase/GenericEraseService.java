package com.hn2.cms.service.aca4001.erase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hn2.cms.repository.aca4001.erase.EraseAuditRepo;
import com.hn2.cms.repository.aca4001.erase.EraseMirrorRepo;
import com.hn2.cms.service.aca4001.erase.model.EraseCommand;
import com.hn2.cms.service.aca4001.erase.model.RestoreCommand;
import com.hn2.cms.service.aca4001.erase.spi.DependentEraseTarget;
import com.hn2.cms.service.aca4001.erase.spi.EraseTarget;
import com.hn2.cms.service.aca4001.erase.support.RowUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GenericEraseService {
    private final List<EraseTarget> eraseTargets;                  // 由 Spring 注入（所有 @Component 的 Adapter）
    private final List<DependentEraseTarget> dependentEraseTargets;
    private final EraseMirrorRepo mirrorRepo;
    private final EraseAuditRepo auditRepo;
    private final AesGcmCrypto crypto;
    private final ObjectMapper om;

    // ========== 共用：鏡像 + 稽核 + 清空 ==========
    /*@Transactional
    public void eraseRows(String acaCardNo,
                          Map<String, List<String>> tableToIds,   // 表 → 主鍵 IDs
                          String operatorUserId, String operatorIp) {
        // 1) 單表（主鍵直接選取）
        for (EraseTarget t : eraseTargets) {
            var ids = tableToIds.getOrDefault(t.table(), List.of());
            if (ids.isEmpty()) continue;

            // 1.1 讀 rows
            var rows = t.loadRowsByIds(ids);
            if (rows.isEmpty()) continue;

            // 1.2 逐筆鏡像
            for (var r : rows) {
                String id = extractIdOrThrow(r, t.idColumn(), t.table());
                String json = buildRowPayloadJson(t.schema(), t.table(), t.idColumn(), id, r);
                var enc = crypto.encryptJson(json);
                String sha256 = AesGcmCrypto.sha256Hex(json);

                mirrorRepo.upsert(t.table(), id, acaCardNo, enc.payloadBase64, enc.ivBase64, sha256, t.schema());
                auditRepo.insertErase(t.schema(), t.table(), id, acaCardNo, null, "ERASE_BY_API", operatorUserId, operatorIp);
            }

            // 1.3 清空 + isERASE=1
            t.nullifyAndMarkErased(ids);
        }

        // 2) 依附表（用父鍵清）——例如 ProjectJailGuidanceSummary by ProRecID
        // 期待 tableToIds 以「父表名」塞入父鍵清單（例如 "ProRec" -> [P001, P002]）
        for (DependentEraseTarget dt : dependentEraseTargets) {
            // 用父表名當 key 比較直覺，也可改用一個固定 key 集合
            var parentIds = tableToIds.getOrDefault("ProRec", List.of()); // 你也可用 dt.parentTableName() 來做
            if (parentIds.isEmpty()) continue;

            var rows = dt.loadRowsByParentIds(parentIds);
            for (var r : rows) {
                String id = extractIdOrThrow(r, dt.idColumn(), dt.table());
                String json = buildRowPayloadJson(dt.schema(), dt.table(), dt.idColumn(), id, r);
                var enc = crypto.encryptJson(json);
                String sha256 = AesGcmCrypto.sha256Hex(json);

                mirrorRepo.upsert(dt.table(), id, acaCardNo, enc.payloadBase64, enc.ivBase64, sha256, dt.schema());
                auditRepo.insertErase(dt.schema(), dt.table(), id, acaCardNo, null, "ERASE_BY_API", operatorUserId, operatorIp);
            }
            dt.nullifyAndMarkErasedByParent(parentIds);
        }
    }*/
    @Transactional
    public void eraseRows(EraseCommand cmd) {
        // 1) 單表
        for (EraseTarget t : eraseTargets) {
            var ids = cmd.idsOf(t.table());   // ← 改這裡，從 command 取
            if (ids.isEmpty()) continue;

            var rows = t.loadRowsByIds(ids);
            if (rows.isEmpty()) continue;

            for (var r : rows) {
                String id = RowUtils.extractIdOrThrow(r, t.idColumn(), t.table());
                String json = buildRowPayloadJson(t.schema(), t.table(), t.idColumn(), id, r);
                var enc = crypto.encryptJson(json);
                String sha256 = AesGcmCrypto.sha256Hex(json);

                mirrorRepo.upsert(t.table(), id, cmd.getAcaCardNo(), enc.payloadBase64, enc.ivBase64, sha256, t.schema());
                auditRepo.insertErase(t.schema(), t.table(), id, cmd.getAcaCardNo(),
                        cmd.getDocNum(), cmd.getEraseReason(), cmd.getOperatorUserId(), cmd.getOperatorIp());
            }
            t.nullifyAndMarkErased(ids);
        }

        // 2) 依附表（讀各自 parentTableName）
        for (DependentEraseTarget dt : dependentEraseTargets) {
            var parentIds = cmd.idsOf(dt.parentTableName());
            if (parentIds.isEmpty()) continue;

            var rows = dt.loadRowsByParentIds(parentIds);
            for (var r : rows) {
                String id = RowUtils.extractIdOrThrow(r, dt.idColumn(), dt.table());
                String json = buildRowPayloadJson(dt.schema(), dt.table(), dt.idColumn(), id, r);
                var enc = crypto.encryptJson(json);
                String sha256 = AesGcmCrypto.sha256Hex(json);

                mirrorRepo.upsert(dt.table(), id, cmd.getAcaCardNo(), enc.payloadBase64, enc.ivBase64, sha256, dt.schema());
                auditRepo.insertErase(dt.schema(), dt.table(), id, cmd.getAcaCardNo(),
                        cmd.getDocNum(), cmd.getEraseReason(), cmd.getOperatorUserId(), cmd.getOperatorIp());
            }
            dt.nullifyAndMarkErasedByParent(parentIds);
        }
    }

    // ========== 共用：還原（用 ACACardNo 撈 Mirror） ==========
    /*@Transactional
    public void restoreAllByAcaCardNo(String acaCardNo, String operatorUserId, String operatorIp, String reason) {
        // 撈出所有鏡像（此處可選擇只還原你管理的表；為簡化先撈出所有，過濾用「Adapter 的表清單」）
        var allowedTables = new java.util.HashSet<String>();
        eraseTargets.forEach(t -> allowedTables.add(t.table()));
        dependentEraseTargets.forEach(dt -> allowedTables.add(dt.table()));

        var mirrors = mirrorRepo.findAllByAcaCardNo(acaCardNo, List.copyOf(allowedTables), "dbo"); // 你的 repo 已處理 null/dbo
        if (mirrors.isEmpty()) return;

        // 先把鏡像按表分組
        var byTable = mirrors.stream().collect(java.util.stream.Collectors.groupingBy(EraseMirrorRepo.MirrorRow::getTargetTable));

        // 單表還原
        for (EraseTarget t : eraseTargets) {
            var list = byTable.getOrDefault(t.table(), List.of());
            if (list.isEmpty()) continue;

            var rowsToRestore = new java.util.ArrayList<Map<String, Object>>();
            for (var m : list) {
                var map = decryptMirrorToRowMapOrThrow(m);
                rowsToRestore.add(map);
                auditRepo.insertRestore(t.schema(), t.table(), m.getTargetId(), acaCardNo, reason, operatorUserId, operatorIp);
            }
            t.restoreFromRows(rowsToRestore, operatorUserId); // 內部設 isERASE=0 + ModifiedByUserID/operator
        }

        // 依附表還原
        for (DependentEraseTarget dt : dependentEraseTargets) {
            var list = byTable.getOrDefault(dt.table(), List.of());
            if (list.isEmpty()) continue;

            var rowsToRestore = new java.util.ArrayList<Map<String, Object>>();
            for (var m : list) {
                var map = decryptMirrorToRowMapOrThrow(m);
                rowsToRestore.add(map);
                auditRepo.insertRestore(dt.schema(), dt.table(), m.getTargetId(), acaCardNo, reason, operatorUserId, operatorIp);
            }
            dt.restoreFromRows(rowsToRestore, operatorUserId);
        }
    }*/
    @Transactional
    public void restoreAllByAcaCardNo(RestoreCommand cmd) {
        var allowedTables = new java.util.HashSet<String>();
        eraseTargets.forEach(t -> allowedTables.add(t.table()));
        dependentEraseTargets.forEach(dt -> allowedTables.add(dt.table()));

        var mirrors = mirrorRepo.findAllByAcaCardNo(cmd.getAcaCardNo(),
                List.copyOf(allowedTables), "dbo");
        if (mirrors.isEmpty()) return;

        var byTable = mirrors.stream()
                .collect(java.util.stream.Collectors.groupingBy(EraseMirrorRepo.MirrorRow::getTargetTable));

        for (EraseTarget t : eraseTargets) {
            var list = byTable.getOrDefault(t.table(), List.of());
            if (list.isEmpty()) continue;

            var rowsToRestore = new java.util.ArrayList<Map<String, Object>>();
            for (var m : list) {
                var map = decryptMirrorToRowMapOrThrow(m);
                rowsToRestore.add(map);
                auditRepo.insertRestore(t.schema(), t.table(), m.getTargetId(),
                        cmd.getAcaCardNo(), cmd.getRestoreReason(),
                        cmd.getOperatorUserId(), cmd.getOperatorIp());
            }
            t.restoreFromRows(rowsToRestore, cmd.getOperatorUserId());
        }

        for (DependentEraseTarget dt : dependentEraseTargets) {
            var list = byTable.getOrDefault(dt.table(), List.of());
            if (list.isEmpty()) continue;

            var rowsToRestore = new java.util.ArrayList<Map<String, Object>>();
            for (var m : list) {
                var map = decryptMirrorToRowMapOrThrow(m);
                rowsToRestore.add(map);
                auditRepo.insertRestore(dt.schema(), dt.table(), m.getTargetId(),
                        cmd.getAcaCardNo(), cmd.getRestoreReason(),
                        cmd.getOperatorUserId(), cmd.getOperatorIp());
            }
            dt.restoreFromRows(rowsToRestore, cmd.getOperatorUserId());
        }
    }

    // ====== Helpers ======
    private String buildRowPayloadJson(String schema, String table, String idCol, String id, Map<String, Object> fields) {
        var node = om.createObjectNode();
        node.put("schema", schema);
        node.put("table", table);
        node.put("idColumn", idCol);
        node.put("id", id);
        node.set("fields", om.valueToTree(fields)); // 包含白名單欄位 + CreatedBy/ModifiedByUserID 等
        try {
            return om.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private Map<String, Object> decryptMirrorToRowMapOrThrow(EraseMirrorRepo.MirrorRow m) {
        String json = crypto.decryptToJson(m.getPayloadBase64(), m.getIvBase64());
        String sha = AesGcmCrypto.sha256Hex(json);
        if (!sha.equalsIgnoreCase(m.getSha256())) {
            throw new IllegalStateException("Mirror payload 校驗失敗: " + m.getTargetId());
        }
        try {
            var node = (ObjectNode) om.readTree(json);
            // 直接還原 fields map
            var fieldsNode = (ObjectNode) node.get("fields");
            var map = new java.util.LinkedHashMap<String, Object>();
            map.put("schema", node.path("schema").asText());
            map.put("table", node.path("table").asText());
            map.put("idColumn", node.path("idColumn").asText());
            map.put("id", node.path("id").asText());
            fieldsNode.fields().forEachRemaining(e -> map.put(e.getKey(), e.getValue().isNull() ? null : e.getValue().isNumber() ? e.getValue().numberValue() : e.getValue().isBoolean() ? e.getValue().booleanValue() : e.getValue().asText()));
            return map;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    // 在 GenericEraseService 內新增：
    private String extractIdOrThrow(Map<String, Object> row, String idColumn, String table) {
        // 1) 優先讀固定別名 __PK__
        Object v = row.get("__PK__");
        if (v == null && idColumn != null) {
            // 2) 退而求其次：讀 adapter 宣告的 idColumn
            v = row.get(idColumn);
            if (v == null) {
                // 3) 大小寫容錯
                v = row.get(idColumn.toUpperCase());
                if (v == null) v = row.get(idColumn.toLowerCase());
            }
        }
        String id = (v == null) ? null : v.toString().trim();
        if (id == null || id.isEmpty() || "null".equalsIgnoreCase(id)) {
            throw new IllegalStateException("無法從結果列取到有效主鍵: table=" + table
                    + ", idColumn=" + idColumn
                    + ", rowKeys=" + row.keySet());
        }
        return id;
    }

    // 重用剛才 CrmRecTarget 的這兩個方法（可複製到 GenericEraseService 或抽到共用 Utils）
    private static String toStringCI(Map<String, Object> m, String key) {
        Object v = getCI(m, key);
        return v == null ? null : String.valueOf(v);
    }
    private static Object getCI(Map<String, Object> m, String key) {
        if (m.containsKey(key)) return m.get(key);
        String up = key.toUpperCase(java.util.Locale.ROOT);
        String low = key.toLowerCase(java.util.Locale.ROOT);
        if (m.containsKey(up)) return m.get(up);
        if (m.containsKey(low)) return m.get(low);
        for (String k : m.keySet()) if (k.equalsIgnoreCase(key)) return m.get(k);
        return null;
    }
}
