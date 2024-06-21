package com.hn2.cms.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class Aca2002CrmRecQueryDto {
    // 建檔代碼
    private String id;

    // 建檔編號
    private String acaCardNo;

    // 執行機關1
    private String proSource1;

    // 執行機關2
    private String proNoticeDep;

    // 罪名1
    private String crmCrime1;

    // 罪名2
    private String crmCrime2;

    // 罪名3
    private String crmCrime3;

    // 刑期時間
    private String crmTerm;

    // 入獄日期
    private LocalDate crmChaDate;

    // 出獄原因
    private String crmDischarge;

    // 出獄日期
    private LocalDate crmDisDate;

    // 在監所接受技訓種類
    private String crmTrain;

    // 證照
    private String crmCert;

    // 備註
    private String crmMemo;

    // 減刑案
    private String crmRemission;

    // 預定獲釋日期
    private LocalDate crmReleaseDate;

    // 預定獲釋原因
    private String crmRelease;

    // 未入獄原因
    private String crmNoJail;

    // 刑期
    private String crmSentence;

    // 執行日期
    private LocalDate crmVerdictDate;

    // 建檔分會
    private String createdByBranchID;

    // 建檔人員
    private int createdByUserID;

    // 建檔時間
    private LocalDate createdOnDate;

    // 修檔人員
    private int modifiedByUserID;

    // 修檔時間
    private LocalDate modifiedOnDate;

    // 刪除註記
    private int isDeleted;

}
