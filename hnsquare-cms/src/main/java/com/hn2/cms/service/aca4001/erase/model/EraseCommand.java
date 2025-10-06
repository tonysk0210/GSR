package com.hn2.cms.service.aca4001.erase.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.ToString;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Getter
@ToString
public final class EraseCommand {
    /*
     * 整體用途：
     * - 以「命令物件（Command Pattern）」封裝一次塗銷操作所需的全部資訊。
     * - 由 Service 層建立並傳入 GenericEraseService，讓後者依據此命令執行：
     *   1) 哪個 AcaCardNo
     *   2) 哪些表（table）對應哪些主鍵（ids）
     *   3) 操作者資訊（userId、ip）
     *   4) 稽核/業務所需的 docNum、eraseReason（可為 null）
     * - 類別設為 final + 只讀欄位，並在建構子中做不可變包裝，確保執行過程中不被竄改。
     */
    private final String acaCardNo; // 此次塗銷所屬個案卡號
    private final Map<String, List<String>> tableToIds; //「表名 -> 主鍵清單」的映射，例如：{"CrmRec":[...], "ProRec":[...]}
    private final String operatorUserId; // 操作者ID（稽核）
    private final String operatorIp; // 操作者IP（稽核）
    private final Integer docNum;       // 文件編號（稽核/追蹤，可為 null）
    private final String eraseReason;   // 塗銷原因（稽核/追蹤，可為 null）

    // 啟用 Builder 模式，便於在 Service 層以鏈式語法建構不變物件
    @Builder
    public EraseCommand(String acaCardNo,
                        @Singular("tableIds") Map<String, List<String>> tableToIds,
                        String operatorUserId,
                        String operatorIp,
                        Integer docNum,
                        String eraseReason
    ) {
        this.acaCardNo = acaCardNo;
        this.tableToIds = (tableToIds == null) ? Collections.emptyMap() : Collections.unmodifiableMap(tableToIds);
        this.operatorUserId = operatorUserId;
        this.operatorIp = operatorIp;
        this.docNum = docNum;
        this.eraseReason = eraseReason;
    }

    /**
     * 依表名安全地取回該表要處理的主鍵清單。
     * - 若該表沒有對應的清單，回傳空的不可變 List，避免回傳 null 造成 NPE。
     * - 若有，回傳該清單的不可變視圖，確保外部無法竄改。
     */
    public List<String> idsOf(String table) {
        List<String> v = tableToIds.get(table);
        return v == null ? Collections.emptyList() : Collections.unmodifiableList(v);
    }
}