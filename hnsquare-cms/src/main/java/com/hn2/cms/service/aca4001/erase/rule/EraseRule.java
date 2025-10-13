package com.hn2.cms.service.aca4001.erase.rule;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

//定義規則物件（Java POJO）
@Data
public class EraseRule {
    private String schema = "dbo";
    private String table;             // 例：ProRec / ProDtl
    private String idColumn = "ID";

    // 子表才需要 ↓
    private String parentTable;       // 例：ProDtl 的 parentTable = "ProRec"
    private String parentFkColumn;    // 例：ProDtl 的外鍵 = "ProRecID"

    // 新增 3 個可選欄位（有值才啟用）
    private String parentIdLookupTable;       // 例如: "ACABrd"
    private String parentIdLookupSrcColumn;   // 例如: "ACACardNo"（輸入）
    private String parentIdLookupDstColumn;   // 例如: "FamCardNo"（輸出）

    private List<String> whitelist = List.of();
    private Set<String> dateCols = Set.of();
    private Set<String> intCols = Set.of();

    // 清空策略：沒列到的白名單欄位預設設為 NULL
    private Map<String, Object> eraseSet = new LinkedHashMap<>();

    // 還原時額外覆寫/追加（例：isERASE=0, ModifiedOnDate=${NOW}, ModifiedByUserID=:uid）
    private Map<String, Object> restoreExtraSet = new LinkedHashMap<>();

    public boolean isChild() {
        return parentTable != null && !parentTable.isBlank() && parentFkColumn != null && !parentFkColumn.isBlank();
    }
}
