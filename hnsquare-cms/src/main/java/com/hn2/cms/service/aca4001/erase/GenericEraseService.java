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
        // 1) 針對各 EraseTarget 清空
        for (EraseTarget t : eraseTargets) {
            var ids = cmd.idsOf(t.table());
            if (ids.isEmpty()) continue;

            var rows = t.loadRowsByIds(ids);
            if (rows.isEmpty()) continue;

            for (var r : rows) {
                String id = RowUtils.extractIdOrThrow(r, t.idColumn(), t.table());
                String json = buildRowPayloadJson(t.schema(), t.table(), t.idColumn(), id, r); // 把原始欄位打包成 JSON
                var enc = crypto.encryptJson(json);
                String sha256 = AesGcmCrypto.sha256Hex(json); // 接著計算雜湊

                mirrorRepo.upsert(t.table(), id, cmd.getAcaCardNo(), enc.payloadBase64, enc.ivBase64, sha256, t.schema());
            }
            t.nullifyAndMarkErased(ids);
        }

        // 2) 針對 DependentEraseTarget
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
            }
            dt.nullifyAndMarkErasedByParent(parentIds);
        }

        // 3) ★ 整個流程成功後，才寫「一筆」Audit
        auditRepo.insertEraseAction(
                cmd.getAcaCardNo(),
                cmd.getDocNum(),
                cmd.getEraseReason(),
                cmd.getOperatorUserId(),
                cmd.getOperatorIp()
        );
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

        for (var t : eraseTargets) {
            if (t instanceof DependentEraseTarget) continue;
            var list = byTable.get(t.table());
            if (list == null || list.isEmpty()) continue;

            var rowsToRestore = new java.util.ArrayList<java.util.Map<String, Object>>();
            for (var m : list) rowsToRestore.add(decryptMirrorToRowMapOrThrow(m));
            t.restoreFromRows(rowsToRestore, cmd.getOperatorUserId());
            handledTables.add(t.table());
        }

        for (var dt : dependentEraseTargets) {
            if (handledTables.contains(dt.table())) continue;
            var list = byTable.get(dt.table());
            if (list == null || list.isEmpty()) continue;

            var rowsToRestore = new java.util.ArrayList<java.util.Map<String, Object>>();
            for (var m : list) rowsToRestore.add(decryptMirrorToRowMapOrThrow(m));
            dt.restoreFromRows(rowsToRestore, cmd.getOperatorUserId());
            handledTables.add(dt.table());
        }

        // ★ 流程成功 → 寫一筆還原 Audit
        auditRepo.insertRestoreAction(
                cmd.getAcaCardNo(),
                cmd.getRestoreReason(),
                cmd.getOperatorUserId(),
                cmd.getOperatorIp()
        );

        // 保留原本 Mirror 清除
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
        String json = crypto.decryptToJson(m.getPayloadBase64(), m.getIvBase64()); //先解密
        String sha = AesGcmCrypto.sha256Hex(json); //再重算雜湊
        if (!sha.equalsIgnoreCase(m.getSha256())) { //與資料庫帶出的 m.getSha256()（也就是 PayloadSha256Hex）比對
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
