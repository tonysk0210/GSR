package com.hn2.cms.service.aca4001.erase.spi;

import java.util.List;
import java.util.Map;
import java.util.Set;

// 依附表：用父鍵（例如 ProRecID）去清空該表所有符合的多筆列
public interface DependentEraseTarget extends EraseTarget {
    String parentTableName(); // 其父表名（如 "ProRec"）

    List<Map<String, Object>> loadRowsByParentIds(List<String> parentIds);

    int nullifyAndMarkErasedByParent(List<String> parentIds);// 依父鍵清空/標記
}