package com.hn2.cms.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper=false)
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Table
@Entity(name = "CrmRec")
@EntityListeners(AuditingEntityListener.class)
public class CrmRecEntity {

    // 建檔代碼
    @Id
    @Column(name = "ID", nullable = false)
    private String id;

    // 建檔編號
    @Column(name = "ACACardNo")
    private String acaCardNo;

    // 執行機關1
    @Column(name = "ProSource1")
    private String proSource1;

    // 執行機關2
    @Column(name = "ProNoticeDep")
    private String proNoticeDep;

    // 罪名1
    @Column(name = "CrmCrime1")
    private String crmCrime1;

    // 罪名2
    @Column(name = "CrmCrime2")
    private String crmCrime2;

    // 罪名3
    @Column(name = "CrmCrime3")
    private String crmCrime3;

    // 刑期時間
    @Column(name = "CrmTerm")
    private String crmTerm;

    // 入獄日期
    @Column(name = "CrmChaDate")
    private LocalDate crmChaDate;

    // 出獄原因
    @Column(name = "CrmDischarge")
    private String crmDischarge;

    // 出獄日期
    @Column(name = "CrmDisDate")
    private LocalDate crmDisDate;

    // 在監所接受技訓種類
    @Column(name = "CrmTrain")
    private String crmTrain;

    // 證照
    @Column(name = "CrmCert")
    private String crmCert;

    // 備註
    @Column(name = "CrmMemo")
    private String crmMemo;

    // 減刑案
    @Column(name = "CrmRemission")
    private String crmRemission;

    // 預定獲釋日期
    @Column(name = "Crm_ReleaseDate")
    private LocalDate crmReleaseDate;

    // 預定獲釋原因
    @Column(name = "Crm_Release")
    private String crmRelease;

    // 未入獄原因
    @Column(name = "Crm_NoJail")
    private String crmNoJail;

    // 刑期
    @Column(name = "Crm_Sentence")
    private String crmSentence;

    // 執行日期
    @Column(name = "Crm_VerdictDate")
    private LocalDate crmVerdictDate;

    // 建檔分會
    @Column(name = "CreatedByBranchID")
    private String createdByBranchID;

    // 建檔人員
    @Column(name = "CreatedByUserID")
    private String createdByUserID;

    // 建檔時間
    @Column(name = "CreatedOnDate")
    private LocalDate createdOnDate;

    // 修檔人員
    @Column(name = "ModifiedByUserID")
    private String modifiedByUserID;

    // 修檔時間
    @Column(name = "ModifiedOnDate")
    private LocalDate modifiedOnDate;

    // 刪除註記
    @Column(name = "IsDeleted")
    private int isDeleted;


}
