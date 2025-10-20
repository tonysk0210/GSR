package com.hn2.cms.dto.aca2003;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Aca2003QueryDto {
    private Integer id;
    private Timestamp createdOnDate;
    /** 分會顯示名稱（由 Lists 對應 CreatedByBranchID） */
    private String createdByBranchName;

    private String drgUserText;
    private String oprFamilyText;
    private String oprFamilyCareText;
    private String oprSupportText;
    private String oprContactText;
    private String oprReferText;
    private String addr;
    private String oprAddr;

    private String acaCardNo;
    private String acaName;   // from ACABrd
    private String acaIdNo;   // from ACABrd（一起帶回）
}
