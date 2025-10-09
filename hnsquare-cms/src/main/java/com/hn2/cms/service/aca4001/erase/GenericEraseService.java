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
    private final List<EraseTarget> eraseTargets;                   // 各可塗銷的主表/獨立表 Adapter
    private final List<DependentEraseTarget> dependentEraseTargets; // 各相依的子表/關聯表 Adapter
    private final EraseMirrorRepo mirrorRepo;                       // 鏡像表存取（ACA_EraseMirror）
    private final EraseAuditRepo auditRepo;                         // 塗銷異動表存取（ACA_EraseAudit）
    private final AesGcmCrypto crypto;                              // AES-GCM 加解密與 sha256 計算
    private final ObjectMapper om;                                  // JSON 序列化

    /**
     * 執行整批「鏡像 → 清空 → 寫入塗銷異動表」的塗銷流程。
     * 流程概要：
     * 1) 針對每個 EraseTarget（獨立表）：
     * - 依指令中的 tableToIds 取出該表要處理的 ID
     * - 載入原始列資料 → 打包成 JSON → AES-GCM 加密 → 計算 SHA-256（明文）
     * - upsert 到 ACA_EraseMirror（含密文、IV、明文 SHA）
     * - 對原表做欄位淨空/標記塗銷
     * 2) 針對每個 DependentEraseTarget（相依子表）：
     * - 依父表 ID 批次載入相依列 → 同步鏡像 → 子表欄位淨空/標記塗銷
     * 3) 全部成功後，寫入「一筆」ERASE 塗銷異動紀錄（Audit）
     * 交易特性：
     * - @Transactional：任一步失敗會丟出例外並整個回滾（不會留下半套鏡像/清空）。
     *
     * @param cmd EraseCommand：包含 acaCardNo、table→ids 對應、操作者資訊、單號與原因
     */
    @Transactional
    public void eraseRows(EraseCommand cmd) {
        // 1) 針對各 EraseTarget（主表/獨立表）清空
        for (EraseTarget t : eraseTargets) {
            var ids = cmd.idsOf(t.table());  // 由指令取出這張表要處理的 ID 清單
            if (ids.isEmpty()) continue;

            var rows = t.loadRowsByIds(ids); // 先把原始列資料撈出（白名單欄位 + 主鍵）
            if (rows.isEmpty()) continue;

            for (var r : rows) {
                String id = RowUtils.extractIdOrThrow(r, t.idColumn(), t.table());              // 安全取主鍵
                String json = buildRowPayloadJson(t.schema(), t.table(), t.idColumn(), id, r);  // 將整列打包成 JSON（含表名/欄位/值）
                var enc = crypto.encryptJson(json);                                             // AES-GCM：得到密文 (Base64) 與 IV
                String sha256 = AesGcmCrypto.sha256Hex(json);                                   // 對明文 JSON 算 SHA-256（十六進位）

                // upsert 到鏡像表（TargetSchema/TargetTable/TargetID 作為鍵）
                mirrorRepo.upsert(t.table(), id, cmd.getAcaCardNo(), enc.payloadBase64, enc.ivBase64, sha256, t.schema());
            }
            // 鏡像完成後，才對原表進行欄位淨空/標記（避免沒鏡像就清空）
            t.nullifyAndMarkErased(ids);
        }

        // 2) 針對 DependentEraseTarget（子表/關聯表）
        for (DependentEraseTarget dt : dependentEraseTargets) {
            var parentIds = cmd.idsOf(dt.parentTableName()); // 依父表名取出父 ID（例如 ACABrd 或 ProRec）
            if (parentIds.isEmpty()) continue;

            var rows = dt.loadRowsByParentIds(parentIds);    // 以父 ID 撈出所有相依列
            for (var r : rows) {
                String id = RowUtils.extractIdOrThrow(r, dt.idColumn(), dt.table());
                String json = buildRowPayloadJson(dt.schema(), dt.table(), dt.idColumn(), id, r);
                var enc = crypto.encryptJson(json);
                String sha256 = AesGcmCrypto.sha256Hex(json);

                mirrorRepo.upsert(dt.table(), id, cmd.getAcaCardNo(), enc.payloadBase64, enc.ivBase64, sha256, dt.schema());
            }

            // 鏡像完畢，再對子表批次做欄位淨空/標記
            dt.nullifyAndMarkErasedByParent(parentIds);
        }

        // 3) 整個流程成功後，才寫「一筆」Erase 塗銷異動紀錄（若前面任一步失敗將回滾，不會寫入）
        auditRepo.insertEraseAction(
                cmd.getAcaCardNo(),
                cmd.getDocNum(),
                cmd.getEraseReason(),
                cmd.getOperatorUserId(),
                cmd.getOperatorIp()
        );
    }

    /**
     * 依 ACACardNo 執行「整案還原」流程：
     * 1) 從 ACA_EraseMirror 取出該案（且屬於允許的目標表）的所有鏡像資料。
     * 2) 依表名分組；先還原獨立表（EraseTarget），再還原相依表（DependentEraseTarget），
     * 每筆 mirror 皆會先解密 & 驗證（AES-GCM + 明文 SHA-256）後才寫回。
     * 3) 全部成功後，寫入一筆 RESTORE 稽核；最後移除鏡像資料（delete by ACACardNo）。
     * 交易特性：
     * - 標註 @Transactional：任何一步異常會回滾（不會產生半套還原/稽核）。
     * 安全性：
     * - 還原順序：先主（非 Dependent）後子（Dependent），避免 FK/依賴順序問題。
     * - 每筆 mirror 於寫回前會執行 decrypt + SHA-256 校驗，確保鏡像未遭竄改。
     *
     * @param cmd 還原指令（含 ACACardNo、操作者資訊、還原原因）
     */
    @Transactional
    public void restoreAllByAcaCardNo(RestoreCommand cmd) {
        // 1) 建立允許還原的表名集合（包含所有 EraseTarget 與 DependentEraseTarget 的 table()）
        var allowed = new java.util.HashSet<String>();
        eraseTargets.forEach(t -> allowed.add(t.table()));
        dependentEraseTargets.forEach(dt -> allowed.add(dt.table()));

        // 2) 依 ACACardNo + 允許的表名 + schema('dbo') 從鏡像表撈資料
        var mirrors = mirrorRepo.findAllByAcaCardNo(cmd.getAcaCardNo(), new java.util.ArrayList<>(allowed), "dbo");
        if (mirrors.isEmpty()) return;

        // 3) 依 TargetTable 分組 mirror rows（Map<tableName, List<MirrorRow>>）
        var byTable = mirrors.stream().collect(java.util.stream.Collectors.groupingBy(EraseMirrorRepo.MirrorRow::getTargetTable));

        var handledTables = new java.util.HashSet<String>(); // 記錄已處理過的表

        // 4) 先處理獨立表（非 DependentEraseTarget）
        for (var t : eraseTargets) {
            if (t instanceof DependentEraseTarget) continue; // 保險：略過相依型
            var list = byTable.get(t.table());
            if (list == null || list.isEmpty()) continue;

            // 將每筆 mirror 解密 + 校驗 → 還原為欄位 Map
            var rowsToRestore = new java.util.ArrayList<java.util.Map<String, Object>>();
            for (var m : list) rowsToRestore.add(decryptMirrorToRowMapOrThrow(m));

            // 寫回原表（由各 Target 實作）
            t.restoreFromRows(rowsToRestore, cmd.getOperatorUserId());
            handledTables.add(t.table());
        }

        // 5) 再處理相依表（DependentEraseTarget）：主表先復原後，子表才寫回，避免 FK 問題
        for (var dt : dependentEraseTargets) {
            if (handledTables.contains(dt.table())) continue; // 若已處理過就跳過（保護性）
            var list = byTable.get(dt.table());
            if (list == null || list.isEmpty()) continue;

            var rowsToRestore = new java.util.ArrayList<java.util.Map<String, Object>>();
            for (var m : list) rowsToRestore.add(decryptMirrorToRowMapOrThrow(m));
            dt.restoreFromRows(rowsToRestore, cmd.getOperatorUserId());
            handledTables.add(dt.table());
        }

        // 6) 全部成功才寫一筆還原稽核（RESTORE）
        auditRepo.insertRestoreAction(
                cmd.getAcaCardNo(),
                cmd.getRestoreReason(),
                cmd.getOperatorUserId(),
                cmd.getOperatorIp()
        );

        // 7) 清掉該案的鏡像（避免重覆還原；你選擇的是 hard delete）
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
