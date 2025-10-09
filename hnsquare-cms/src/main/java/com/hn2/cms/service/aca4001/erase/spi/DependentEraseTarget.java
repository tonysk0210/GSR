package com.hn2.cms.service.aca4001.erase.spi;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 「相依塗銷目標」的 SPI：用在那些『由父表資料驅動』的子表/關聯表。
 * 例：ProDtl/ProRecMember 依附於 ProRec；Families/Memo/Career 依附於 ACABrd。
 * GenericEraseService 的使用方式：
 * - erase：以 parentIds（來自 EraseCommand.tableToIds 的 parentTableName() 對應鍵）
 * 先 mirror 子表資料（loadRowsByParentIds），再對這些資料做 nullifyAndMarkErasedByParent。
 * - restore：先還原非相依的 EraseTarget，之後再還原 DependentEraseTarget（避免 FK/邏輯順序問題）。
 * 實作契約重點：
 * - parentTableName() 必須回傳『父表在 EraseCommand 中使用的表名字串』，以便框架找到 parentIds。
 * - 本介面仍繼承 EraseTarget：table() 回傳「子表名」、idColumn() 回傳「子表主鍵欄位」、
 * whitelistColumns()/loadRowsByIds()/restoreFromRows() 仍照一般表的規則實作。
 */
public interface DependentEraseTarget extends EraseTarget {
    /**
     * 回傳父表名稱（必實作）。
     * 例：子表為 "ProDtl" 時，通常回 "ProRec"；若子表依附個案主檔則回 "ACABrd"。
     * 注意：此名稱必須與 EraseCommand.tableToIds 的 key 一致，否則拿不到 parentIds。
     */
    String parentTableName();

    /**
     * 依父表主鍵清單載入『子表』資料列，用於鏡像（mirror）流程。
     * 需求：每筆 Map 應包含子表主鍵（idColumn()）＋ 白名單欄位（whitelistColumns()）。
     * 典型 SQL 會以 子表.FK = 父表.ID AND 子表.IsDeleted=0 AND 子表.ID IN (...) 或 FK IN (...)。
     */
    List<Map<String, Object>> loadRowsByParentIds(List<String> parentIds);

    /**
     * 針對『父表主鍵清單』，把所有相依列做「欄位淨空/標記已塗銷」等處理（不刪除）。
     * 回傳受影響筆數（總和）。
     * 典型 SQL：UPDATE 子表 SET <白名單欄位=null>, IsErased=1, Modified... WHERE FK IN (...).
     */
    int nullifyAndMarkErasedByParent(List<String> parentIds);
}