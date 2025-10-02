package com.hn2.cms.service.aca4001.erase.spi;

import java.util.List;
import java.util.Map;
import java.util.Set;

// 單表：選到哪些主鍵 ID，就處理該表的那些列
public interface EraseTarget {
    default String schema() { return "dbo"; }
    String table();
    default String idColumn() { return "__PK__"; }
    List<String> whitelistColumns();
    default Set<String> dateColsNorm() { return java.util.Collections.emptySet(); }
    default Set<String> intColsNorm()  { return java.util.Collections.emptySet(); }

    List<Map<String,Object>> loadRowsByIds(List<String> ids);
    int nullifyAndMarkErased(List<String> ids);
    int restoreFromRows(List<Map<String,Object>> rows, String operatorUserId);
    default boolean isErased(String id) { return false; }
}