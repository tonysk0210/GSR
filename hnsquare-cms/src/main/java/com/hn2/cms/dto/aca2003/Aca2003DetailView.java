package com.hn2.cms.dto.aca2003;

import java.sql.Timestamp;

//SQL alias → Projection getter
//
//SQL 裡 AS xxx 的 alias
//會對應到 Projection 的 getter 方法，轉換規則：
//getCreatedOnDate() → 去掉 get → 首字母小寫 → createdOnDate

// Aca2003DetailView 跟 Entity 沒有直接關係，它不是從 JPA Entity 的欄位做 mapping，而是從 資料庫查詢結果集 (ResultSet) 映射過來的。
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
