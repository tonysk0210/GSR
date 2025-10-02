package com.hn2.cms.service.aca4001.erase.spi;

import java.util.List;
import java.util.Map;
import java.util.Set;

// 依附表：用父鍵（例如 ProRecID）去清空該表所有符合的多筆列
public interface DependentEraseTarget extends EraseTarget {
    String parentTableName(); // ← 新增：宣告父表名（例如 "ProRec"）
    List<Map<String,Object>> loadRowsByParentIds(List<String> parentIds);
    int nullifyAndMarkErasedByParent(List<String> parentIds);
}