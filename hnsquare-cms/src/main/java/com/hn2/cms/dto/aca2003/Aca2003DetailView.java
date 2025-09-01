package com.hn2.cms.dto.aca2003;

import java.sql.Timestamp;

/**
 * Aca2003DetailView（介面型投影 / Interface Projection）
 * <p>
 * 用途：
 * - 對應 Repository 中使用 {@code nativeQuery=true} 的 SELECT 結果集；
 * - 只取需要的欄位，避免 Entity 整體載入，提升查詢效率與鬆耦合。
 * <p>
 * 映射規則（關鍵）：
 * - 每個 getter 名稱（去掉 get、首字母小寫）必須**精準對應** SQL 中的欄位別名（AS）。
 * 例如：SQL 的 {@code AS createdOnDate} → {@code getCreatedOnDate()}。
 * - 若調整 SQL 欄位別名，請**同步**調整這裡的 getter 名稱。
 */
public interface Aca2003DetailView {
    Integer getId();

    Timestamp getCreatedOnDate();

    String getCreatedByBranchId();

    String getDrgUserText();

    String getOprFamilyText();

    String getOprFamilyCareText();

    String getOprSupportText();

    String getOprContactText();

    String getOprReferText();

    String getAddr();

    String getOprAddr();

    String getAcaCardNo();

    String getAcaName();

    String getAcaIdNo();
}
