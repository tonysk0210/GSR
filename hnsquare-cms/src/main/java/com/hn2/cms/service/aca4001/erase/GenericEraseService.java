package com.hn2.cms.service.aca4001.erase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hn2.cms.repository.aca4001.erase.EraseAuditRepo;
import com.hn2.cms.repository.aca4001.erase.EraseMirrorRepo;
import com.hn2.cms.service.aca4001.erase.command.EraseCommand;
import com.hn2.cms.service.aca4001.erase.command.RestoreCommand;
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
    @Transactional
    public void restoreAllByAcaCardNo(RestoreCommand cmd) {
        var allowed = new java.util.HashSet<String>();
        eraseTargets.forEach(t -> allowed.add(t.table()));
        dependentEraseTargets.forEach(dt -> allowed.add(dt.table()));

        var mirrors = mirrorRepo.findAllByAcaCardNo(cmd.getAcaCardNo(), new java.util.ArrayList<>(allowed), "dbo");
        if (mirrors.isEmpty()) return;

        var byTable = mirrors.stream()
                .collect(java.util.stream.Collectors.groupingBy(EraseMirrorRepo.MirrorRow::getTargetTable));

        var handledTables = new java.util.HashSet<String>();

        // 1) 只處理「非 Dependent」的 EraseTarget
        for (var t : eraseTargets) {
            if (t instanceof DependentEraseTarget) continue;           // ★ 避免重複
            var list = byTable.get(t.table());
            if (list == null || list.isEmpty()) continue;

            var rowsToRestore = new java.util.ArrayList<java.util.Map<String, Object>>();
            for (var m : list) {
                var map = decryptMirrorToRowMapOrThrow(m);
                rowsToRestore.add(map);
                auditRepo.insertRestore(t.schema(), t.table(), m.getTargetId(),
                        cmd.getAcaCardNo(), cmd.getRestoreReason(),
                        cmd.getOperatorUserId(), cmd.getOperatorIp());
            }
            t.restoreFromRows(rowsToRestore, cmd.getOperatorUserId());
            handledTables.add(t.table());
        }

        // 2) 再處理 DependentEraseTarget（避開已處理的表）
        for (var dt : dependentEraseTargets) {
            if (handledTables.contains(dt.table())) continue;          // ★ 避免重複
            var list = byTable.get(dt.table());
            if (list == null || list.isEmpty()) continue;

            var rowsToRestore = new java.util.ArrayList<java.util.Map<String, Object>>();
            for (var m : list) {
                var map = decryptMirrorToRowMapOrThrow(m);
                rowsToRestore.add(map);
                auditRepo.insertRestore(dt.schema(), dt.table(), m.getTargetId(),
                        cmd.getAcaCardNo(), cmd.getRestoreReason(),
                        cmd.getOperatorUserId(), cmd.getOperatorIp());
            }
            dt.restoreFromRows(rowsToRestore, cmd.getOperatorUserId());
            handledTables.add(dt.table());
        }

        // 3) ★ 全部還原完後，把 ACA_EraseAudit 該個案的紀錄刪掉
        auditRepo.deleteByAcaCardNo(cmd.getAcaCardNo());
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

}
