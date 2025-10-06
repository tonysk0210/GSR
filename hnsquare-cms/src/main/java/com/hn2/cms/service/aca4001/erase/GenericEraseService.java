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

    // 整體用途：
    // - 提供跨表、可擴充的「塗銷/還原」共用邏輯。
    // - 透過 SPI (EraseTarget / DependentEraseTarget) 抽象各表細節。
    // - 流程：讀原始 → 鏡像(加密/校驗) → 稽核 → 清空/標記；還原則反向並寫入 RESTORE 稽核。
    private final List<EraseTarget> eraseTargets; // 直接目標表（主表）的 SPI 實作清單
    private final List<DependentEraseTarget> dependentEraseTargets; // 依附表（子表/關聯表）的 SPI 實作清單
    private final EraseMirrorRepo mirrorRepo; // 鏡像儲存庫（存放加密 JSON 與校驗）
    private final EraseAuditRepo auditRepo; // 稽核儲存庫（ERASE / RESTORE 記錄）
    private final AesGcmCrypto crypto; // AES-GCM 加解密工具
    private final ObjectMapper om; // JSON 序列化/反序列化工具

    // ========== 共用：鏡像 + 稽核 + 清空 ==========
    @Transactional
    public void eraseRows(EraseCommand cmd) {

        // 1) 先處理「直接表」(EraseTarget)
        for (EraseTarget t : eraseTargets) {
            var ids = cmd.idsOf(t.table());   // 從命令以表名取出要處理的主鍵清單
            if (ids.isEmpty()) continue; // 無勾選跳過

            var rows = t.loadRowsByIds(ids); // 由 SPI 實作讀出即將塗銷的原始資料（鏡像用）
            if (rows.isEmpty()) continue; // 保險：查無資料則略過

            for (var r : rows) {
                String id = RowUtils.extractIdOrThrow(r, t.idColumn(), t.table()); // 可靠取出主鍵值
                String json = buildRowPayloadJson(t.schema(), t.table(), t.idColumn(), id, r); // 打包鏡像 JSON
                var enc = crypto.encryptJson(json); // AES-GCM 加密（含 IV）
                String sha256 = AesGcmCrypto.sha256Hex(json); // 計算完整性校驗碼

                // upsert 鏡像：允許覆蓋更新，避免重複 insert 造成衝突
                mirrorRepo.upsert(t.table(), id, cmd.getAcaCardNo(), enc.payloadBase64, enc.ivBase64, sha256, t.schema());
                // 寫入 ERASE 稽核（誰、何時、哪張表/哪個ID、原因、文件編號…）
                auditRepo.insertErase(t.schema(), t.table(), id, cmd.getAcaCardNo(),
                        cmd.getDocNum(), cmd.getEraseReason(), cmd.getOperatorUserId(), cmd.getOperatorIp());
            }
            t.nullifyAndMarkErased(ids); // ★ 由 SPI 實作實際執行：欄位清空 + isERASE=1
        }

        // 2) 再處理「依附表」(DependentEraseTarget) —— 透過 parentTableName 取得父鍵
        for (DependentEraseTarget dt : dependentEraseTargets) {
            var parentIds = cmd.idsOf(dt.parentTableName()); // 先從命令取父表的主鍵清單
            if (parentIds.isEmpty()) continue;

            var rows = dt.loadRowsByParentIds(parentIds); // 由 SPI 依父鍵查出所有子列
            for (var r : rows) {
                String id = RowUtils.extractIdOrThrow(r, dt.idColumn(), dt.table());
                String json = buildRowPayloadJson(dt.schema(), dt.table(), dt.idColumn(), id, r);
                var enc = crypto.encryptJson(json);
                String sha256 = AesGcmCrypto.sha256Hex(json);

                mirrorRepo.upsert(dt.table(), id, cmd.getAcaCardNo(), enc.payloadBase64, enc.ivBase64, sha256, dt.schema());
                auditRepo.insertErase(dt.schema(), dt.table(), id, cmd.getAcaCardNo(),
                        cmd.getDocNum(), cmd.getEraseReason(), cmd.getOperatorUserId(), cmd.getOperatorIp());
            }
            dt.nullifyAndMarkErasedByParent(parentIds); // ★ 依父鍵批次清空/標記（避免逐筆慢）
        }
    }

    // ========== 共用：還原（用 ACACardNo 撈 Mirror） ==========
    @Transactional
    public void restoreAllByAcaCardNo(RestoreCommand cmd) {
        // 準備允許還原的表名集合（避免撈到不該處理的鏡像）
        var allowed = new java.util.HashSet<String>();
        eraseTargets.forEach(t -> allowed.add(t.table())); // 加入直接表
        dependentEraseTargets.forEach(dt -> allowed.add(dt.table())); // 加入依附表

        // 依卡號撈鏡像（可限定 schema='dbo'）
        var mirrors = mirrorRepo.findAllByAcaCardNo(cmd.getAcaCardNo(), new java.util.ArrayList<>(allowed), "dbo");
        if (mirrors.isEmpty()) return; // 無鏡像資料可還原

        // 依表分組：table -> 鏡像列
        var byTable = mirrors.stream()
                .collect(java.util.stream.Collectors.groupingBy(EraseMirrorRepo.MirrorRow::getTargetTable));

        var handledTables = new java.util.HashSet<String>(); // 記錄已處理表，避免重複

        // 1) 先還原非 Dependent 的目標表（避免順序依賴）
        for (var t : eraseTargets) {
            if (t instanceof DependentEraseTarget) continue; // 若實作了 Dependent 介面則略過（交給下一段）
            var list = byTable.get(t.table());
            if (list == null || list.isEmpty()) continue;

            var rowsToRestore = new java.util.ArrayList<java.util.Map<String, Object>>();
            for (var m : list) {
                var map = decryptMirrorToRowMapOrThrow(m); // 解密鏡像、校驗、轉回欄位 Map
                rowsToRestore.add(map);

                // 寫入 RESTORE 稽核
                auditRepo.insertRestore(t.schema(), t.table(), m.getTargetId(),
                        cmd.getAcaCardNo(), cmd.getRestoreReason(),
                        cmd.getOperatorUserId(), cmd.getOperatorIp());
            }
            t.restoreFromRows(rowsToRestore, cmd.getOperatorUserId()); // ★ 由 SPI 把欄位回填（含 Created/ModifiedBy）
            handledTables.add(t.table());
        }

        // 2) 再還原 Dependent 表（避免與前面重複）
        for (var dt : dependentEraseTargets) {
            if (handledTables.contains(dt.table())) continue; // 若已處理則略過
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
            dt.restoreFromRows(rowsToRestore, cmd.getOperatorUserId()); // ★ 由 SPI 還原子表
            handledTables.add(dt.table());
        }

        // 3) 全部還原後，刪除該卡號的稽核紀錄（依資料治理規則）
        auditRepo.deleteByAcaCardNo(cmd.getAcaCardNo());
    }

    // ====== Helpers ======

    // 將單筆資料列封裝為鏡像 JSON（含 schema/table/idColumn/id/fields）
    private String buildRowPayloadJson(String schema, String table, String idCol, String id, Map<String, Object> fields) {
        var node = om.createObjectNode();
        node.put("schema", schema); // 來源 schema
        node.put("table", table); // 來源表名
        node.put("idColumn", idCol); // 主鍵欄位名
        node.put("id", id); // 主鍵值
        node.set("fields", om.valueToTree(fields)); // 欄位內容（Map -> JSON）
        try {
            return om.writeValueAsString(node); // 轉為字串準備加密入鏡像
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e); // 理論上少見：序列化失敗
        }
    }

    // 將鏡像資料解密 + 校驗 + 還原為欄位 Map（SPI 還原用）
    private Map<String, Object> decryptMirrorToRowMapOrThrow(EraseMirrorRepo.MirrorRow m) {
        String json = crypto.decryptToJson(m.getPayloadBase64(), m.getIvBase64()); // AES-GCM 解密
        String sha = AesGcmCrypto.sha256Hex(json); // 重算 SHA-256
        if (!sha.equalsIgnoreCase(m.getSha256())) { // 完整性校驗
            throw new IllegalStateException("Mirror payload 校驗失敗: " + m.getTargetId());
        }
        try {
            var node = (ObjectNode) om.readTree(json); // JSON 轉 Node
            var fieldsNode = (ObjectNode) node.get("fields");
            var map = new java.util.LinkedHashMap<String, Object>();
            map.put("schema", node.path("schema").asText());
            map.put("table", node.path("table").asText());
            map.put("idColumn", node.path("idColumn").asText());
            map.put("id", node.path("id").asText());
            fieldsNode.fields().forEachRemaining(e -> map.put(e.getKey(), e.getValue().isNull() ? null : e.getValue().isNumber() ? e.getValue().numberValue() : e.getValue().isBoolean() ? e.getValue().booleanValue() : e.getValue().asText()));
            return map;
        } catch (Exception e) {
            throw new IllegalStateException(e); // 解密或解析失敗，一律視為鏡像異常
        }
    }

}
