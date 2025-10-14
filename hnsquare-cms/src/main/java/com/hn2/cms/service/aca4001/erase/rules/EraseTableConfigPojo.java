package com.hn2.cms.service.aca4001.erase.rules;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ✅【EraseTableConfigPojo】
 * 本類別是「塗銷/還原規則設定物件」（POJO）。
 * 目的：
 * - 用來描述某一資料表（table）的塗銷與還原規則。
 * - 由 @Configuration 類別（例如 ACABrdConfig）建立 Bean。
 * - 執行時，會由通用執行器（例如 EraseRestoreExecutor）讀取這個設定，
 * 依照這裡定義的欄位資訊自動產生 SQL，完成清空或還原。
 * 換句話說：這是一個「宣告式設定模板」，決定某張表該怎麼被處理。
 */
@Data
public class EraseTableConfigPojo {

    private String schema = "dbo";              // 資料庫 schema 名稱，預設為 dbo。
    private String table;                       // 目標資料表名稱（例如：ProRec、CrmRec、ACABrd）, 每個表都需要一個對應的設定 Bean。
    private String idColumn = "ID";             // 主鍵欄位名稱，預設為 ID。

    // ---------------------- 子表（Child Table）設定區 ----------------------
    private String parentTable;                 // 子表對應的父表名稱。若該表屬於某父表（例如 ProDtl → ProRec），在這裡填上父表名稱。若是主表（無父關聯），可留空。
    private String parentFkColumn;              // 子表中指向父表的外鍵欄位。例如 ProDtl 的 ProRecID、或 ACAFamilies 的 FamCardNo。

    // ---------------------- 可選的父鍵轉換設定 ----------------------
    private String parentIdLookupTable;         // 當父表的主鍵與子表外鍵型態不一致時，可以定義一個「轉換查詢」。此欄位指定轉換所依據的中介表（lookup table），例如：ACABrd。
    private String parentIdLookupSrcColumn;     // 查詢父鍵轉換時使用的「來源欄位名稱」。例如從父表的 ACACardNo（來源）轉成子表使用的 FamCardNo（目的）。
    private String parentIdLookupDstColumn;     // 查詢父鍵轉換時使用的「目標欄位名稱」。例如 ACABrd.FamCardNo（輸出）對應子表的外鍵。

    // ---------------------- 欄位型別設定 ----------------------
    private List<String> whitelist = List.of(); // 白名單欄位：列出需要被「清空/還原」的欄位名稱清單。
    private Set<String> dateCols = Set.of();    // 定義哪些欄位是「日期型態」欄位（例如：ProDate、MemoDate）。用於還原時自動格式化日期字串。
    private Set<String> intCols = Set.of();     // 定義哪些欄位是「整數型態」欄位（例如：CreatedByUserID）。用於還原時自動格式化整數字串。

    // ---------------------- 清空 / 還原 規則設定 ----------------------
    /**
     * 清空策略：沒列到的白名單欄位預設設為 NULL
     */
    private Map<String, Object> eraseExtraSet = new LinkedHashMap<>();      // 清空時額外覆寫/追加（例：isERASE=1, ModifiedOnDate=${NOW}）
    private Map<String, Object> restoreExtraSet = new LinkedHashMap<>();    // 還原時額外覆寫/追加（例：isERASE=0, ModifiedOnDate=${NOW}, ModifiedByUserID=:uid）

    /**
     * 判斷是否為「子表」。
     */
    public boolean isChild() {
        return parentTable != null && !parentTable.isBlank() && parentFkColumn != null && !parentFkColumn.isBlank();
    }
}
